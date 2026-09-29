// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.particle;

import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.client.particle.VisualParticle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.jspecify.annotations.Nullable;

/**
 * backport: runs the 26.x particle group pipeline inside 1.21.1's ParticleEngine.
 *
 * <ul>
 *   <li>Every 26.x group key ({@link ParticleRenderType}) and every {@link SingleQuadParticle.Layer}
 *       is a 1.21.1 render type, so the engine keeps, ticks, evicts and clears their particles in its
 *       own queues.
 *   <li>{@code ParticleEngineBridgeMixin} records the frame (camera, partial tick, frustum) at the
 *       start of {@code ParticleEngine.render}. When the engine then opens one of these types ({@code
 *       begin}), the type renders all its particles here — group: {@code extractRenderState} +
 *       {@code submit}; layer: {@code SingleQuadParticle.extract} into a {@link
 *       QuadParticleRenderState} + {@code submit} — through the core immediate {@link
 *       SubmitNodeCollector} into a private buffer source, flushed right away, and returns null so the
 *       engine skips its per-particle loop.
 *   <li>The same mixin drives what 26.x ran in {@code ParticleGroup.add/tickParticle} and {@code
 *       ParticleEngine.clearParticles} for Flywheel-visualized particles ({@link VisualParticle}).
 * </ul>
 */
public final class ParticleBridge {

    private static final Map<ParticleRenderType, Function<ParticleEngine, ParticleGroup<?>>> FACTORIES =
            new ConcurrentHashMap<>();
    private static final Map<ParticleRenderType, ParticleGroup<?>> GROUPS = new IdentityHashMap<>();
    private static @Nullable ParticleEngine groupsEngine;

    private static @Nullable ParticleEngine frameEngine;
    private static @Nullable LightTexture frameLight;
    private static @Nullable Camera frameCamera;
    private static @Nullable Frustum frameFrustum;
    private static float framePartialTick;

    private static MultiBufferSource.@Nullable BufferSource buffers;

    private ParticleBridge() {}

    private static volatile boolean groupsPosted;

    /**
     * Client entry: posts {@link RegisterParticleGroupsEvent} on the mod bus during client setup (the
     * tree's listener fills the group factories). Optional: without it the event is posted on the
     * mod's bus the first time a group is needed.
     */
    public static void register(IEventBus modBus) {
        modBus.addListener(
                (FMLClientSetupEvent event) -> event.enqueueWork(() -> postGroups(modBus)));
    }

    private static synchronized void postGroups(@Nullable IEventBus modBus) {
        if (groupsPosted) return;
        groupsPosted = true;
        if (modBus == null)
            modBus =
                    ModList.get()
                            .getModContainerById("hbm")
                            .map(ModContainer::getEventBus)
                            .orElse(null);
        if (modBus != null) modBus.post(new RegisterParticleGroupsEvent());
    }

    static void registerGroup(
            ParticleRenderType key, Function<ParticleEngine, ParticleGroup<?>> factory) {
        FACTORIES.put(key, factory);
    }

    // ---- engine access --------------------------------------------------------------------

    static @Nullable Queue<Particle> queue(
            ParticleEngine engine, net.minecraft.client.particle.ParticleRenderType key) {
        return ((ParticleEngineAccess) engine).hbm$particles().get(key);
    }

    /** The group instance of {@code key} for {@code engine} (created on first use). */
    public static @Nullable ParticleGroup<?> group(ParticleEngine engine, ParticleRenderType key) {
        if (groupsEngine != engine) {
            GROUPS.clear();
            groupsEngine = engine;
        }
        ParticleGroup<?> group = GROUPS.get(key);
        if (group == null) {
            if (!groupsPosted) postGroups(null);
            Function<ParticleEngine, ParticleGroup<?>> factory = FACTORIES.get(key);
            if (factory == null) return null;
            group = factory.apply(engine);
            group.key = key;
            GROUPS.put(key, group);
        }
        return group;
    }

    // ---- frame ----------------------------------------------------------------------------

    /** Called by the mixin at the head of {@code ParticleEngine.render}. */
    public static void beginFrame(
            ParticleEngine engine,
            LightTexture lightTexture,
            Camera camera,
            float partialTick,
            @Nullable Frustum frustum) {
        frameEngine = engine;
        frameLight = lightTexture;
        frameCamera = camera;
        framePartialTick = partialTick;
        frameFrustum = frustum;
    }

    static void renderGroup(ParticleRenderType key) {
        ParticleEngine engine = frameEngine;
        Camera camera = frameCamera;
        if (engine == null || camera == null) return;
        ParticleGroup<?> group = group(engine, key);
        if (group == null || group.isEmpty()) return;
        submit(group.extractRenderState(frameFrustum, camera, framePartialTick), camera);
    }

    static void renderLayer(SingleQuadParticle.LayerType type) {
        ParticleEngine engine = frameEngine;
        Camera camera = frameCamera;
        if (engine == null || camera == null) return;
        Queue<Particle> queue = queue(engine, type);
        if (queue == null || queue.isEmpty()) return;
        float pt = framePartialTick;
        Frustum frustum = frameFrustum;
        QuadParticleRenderState state = new QuadParticleRenderState();
        for (Particle particle : queue) {
            if (frustum != null && !frustum.isVisible(particle.getRenderBoundingBox(pt))) continue;
            if (particle instanceof SingleQuadParticle quad) quad.extract(state, camera, pt);
        }
        if (!state.isEmpty()) submit(state, camera);
    }

    private static void submit(ParticleGroupRenderState state, Camera camera) {
        MultiBufferSource.BufferSource buf = buffers;
        if (buf == null) buf = buffers = MultiBufferSource.immediate(new ByteBufferBuilder(1 << 18));
        try {
            state.submit(SubmitNodeCollector.immediate(buf), CameraRenderState.of(camera));
        } finally {
            buf.endBatch();
            state.clear();
            // RenderType.clearRenderState turned these off; the engine's loop expects them on
            if (frameLight != null) frameLight.turnOnLightLayer();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }
    }

    // ---- lifecycle hooks (26.x ParticleGroup.add / tickParticle, ParticleEngine.clearParticles)

    public static void onAdmitted(Particle particle) {
        if (particle instanceof VisualParticle visual) visual.refreshVisual();
    }

    public static void onTicked(Particle particle) {
        if (particle instanceof VisualParticle visual) {
            if (particle.isAlive()) visual.refreshVisual();
            else visual.removeVisual();
        }
    }

    public static void onCleared(Map<?, ? extends Collection<Particle>> queues) {
        for (Collection<Particle> queue : queues.values())
            for (Particle particle : queue)
                if (particle instanceof VisualParticle visual) visual.removeVisual();
        // 26.x drops its group instances with the particles; they are recreated on demand
        GROUPS.clear();
    }
}

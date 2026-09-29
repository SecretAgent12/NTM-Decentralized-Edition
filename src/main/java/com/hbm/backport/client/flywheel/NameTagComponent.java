// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.component.EntityComponent;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * backport: CrankShaft's NameTagComponent (26.x) draws the name tag of a visualized entity (whose vanilla renderer is
 * skipped) as instanced font glyphs, which needs CrankShaft-only glyph instance types and 26.x font internals. This
 * replacement keeps its API and gate (name, within 64 blocks, shouldShow) and draws the tag with 1.21.1's own
 * EntityRenderer#renderNameTag logic in RenderLevelStageEvent AFTER_ENTITIES, for components whose visual ran
 * beginFrame this frame (i.e. was not culled). NeoForge's RenderNameTagEvent is not fired for these entities.
 */
public final class NameTagComponent implements EntityComponent {
    private static final Set<NameTagComponent> LIVE = ConcurrentHashMap.newKeySet();
    private static volatile boolean hooked;
    private static int frame;

    private final Entity entity;
    private Supplier<@Nullable Component> nameTag;
    private BooleanSupplier shouldShow;
    private volatile @Nullable Component shown;
    private volatile int shownFrame = -1;

    public NameTagComponent(VisualizationContext context, Entity entity) {
        this.entity = entity;
        nameTag = entity::getDisplayName;
        shouldShow = () -> entity.shouldShowName()
                || entity.hasCustomName() && entity == Minecraft.getInstance().getEntityRenderDispatcher().crosshairPickEntity;
        hook();
        LIVE.add(this);
    }

    private static void hook() {
        if (hooked) return;
        synchronized (NameTagComponent.class) {
            if (hooked) return;
            NeoForge.EVENT_BUS.addListener(NameTagComponent::onRenderLevelStage);
            hooked = true;
        }
    }

    public NameTagComponent nameTag(Supplier<@Nullable Component> nameTag) {
        this.nameTag = nameTag;
        return this;
    }

    public NameTagComponent shouldShow(BooleanSupplier shouldShow) {
        this.shouldShow = shouldShow;
        return this;
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        Component name = nameTag.get();
        Vec3 camera = ctx.camera().getPosition();
        if (name == null || entity.distanceToSqr(camera) >= 4096.0 || !shouldShow.getAsBoolean()) {
            shown = null;
            return;
        }
        shown = name;
        shownFrame = frame;
    }

    @Override
    public void delete() {
        LIVE.remove(this);
        shown = null;
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        int current = frame++;
        if (LIVE.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Font font = minecraft.font;
        for (NameTagComponent tag : LIVE) {
            Component name = tag.shown;
            if (name == null || tag.shownFrame != current) continue;
            Entity entity = tag.entity;
            Vec3 attachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTick));
            if (attachment == null) continue;
            int light = minecraft.getEntityRenderDispatcher().getPackedLightCoords(entity, partialTick);
            double x = net.minecraft.util.Mth.lerp(partialTick, entity.xOld, entity.getX()) - cam.x;
            double y = net.minecraft.util.Mth.lerp(partialTick, entity.yOld, entity.getY()) - cam.y;
            double z = net.minecraft.util.Mth.lerp(partialTick, entity.zOld, entity.getZ()) - cam.z;
            // EntityRenderer#renderNameTag (1.21.1)
            poseStack.pushPose();
            poseStack.translate(x + attachment.x, y + attachment.y + 0.5, z + attachment.z);
            poseStack.mulPose(camera.rotation());
            poseStack.scale(0.025F, -0.025F, 0.025F);
            Matrix4f pose = poseStack.last().pose();
            boolean notSneaking = !entity.isDiscrete();
            int background = (int) (minecraft.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
            float left = (float) (-font.width(name) / 2);
            int yOffset = "deadmau5".equals(name.getString()) ? -10 : 0;
            font.drawInBatch(name, left, yOffset, 0x20FFFFFF, false, pose, buffers,
                    notSneaking ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, background, light);
            if (notSneaking) {
                font.drawInBatch(name, left, yOffset, -1, false, pose, buffers, Font.DisplayMode.NORMAL, 0, light);
            }
            poseStack.popPose();
        }
        buffers.endBatch();
    }
}

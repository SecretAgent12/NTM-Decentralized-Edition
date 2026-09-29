// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.hbm.backport.client.core.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jspecify.annotations.Nullable;

/**
 * backport: the 1.21.1 side of 26.x item definitions. Items whose definition is more than a plain
 * model get {@link Model} as inventory model (custom renderer, no display transform of its own);
 * 1.21.1 then calls this BEWLR, which resolves the 26.x {@link ItemStackRenderState} (the layers
 * carry the display transforms) and submits it.
 *
 * <p>1.21.1 BEWLRs do not receive the holding entity, but every 1.21.1 item draw first resolves the
 * model's {@link ItemOverrides} with (stack, level, entity, seed); {@link Overrides} records that and
 * the following renderByItem of the same stack uses it as the 26.x {@link ItemOwner}.
 */
public final class ItemDefinitionRenderer extends BlockEntityWithoutLevelRenderer {
    private static @Nullable ItemDefinitionRenderer instance;

    public static final IClientItemExtensions EXTENSIONS =
            new IClientItemExtensions() {
                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return ItemDefinitionRenderer.get();
                }
            };

    // last ItemOverrides.resolve call (render thread)
    private static @Nullable ItemStack capturedStack;
    private static @Nullable ClientLevel capturedLevel;
    private static @Nullable LivingEntity capturedEntity;
    private static int capturedSeed;

    private final List<ItemStackRenderState> pool = new ArrayList<>();
    private int depth;

    private ItemDefinitionRenderer() {
        super(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    public static ItemDefinitionRenderer get() {
        if (instance == null) instance = new ItemDefinitionRenderer();
        return instance;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {}

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        if (ClientItems.model(stack.getItem()) == null) return; // not baked (yet): avoid legacy recursion
        Level level;
        LivingEntity entity;
        int seed;
        if (capturedStack == stack) {
            level = capturedLevel != null ? capturedLevel : Minecraft.getInstance().level;
            entity = capturedEntity;
            seed = capturedSeed;
        } else {
            level = Minecraft.getInstance().level;
            entity = null;
            seed = 0;
        }
        if (depth == pool.size()) pool.add(new ItemStackRenderState());
        ItemStackRenderState state = pool.get(depth++);
        try {
            ItemModelResolver.get()
                    .updateForTopItem(state, stack, displayContext, level, ItemOwner.of(entity), seed);
            poseStack.pushPose();
            // 1.21.1 already applied translate(-0.5); the 26.x layers recenter themselves
            poseStack.translate(0.5F, 0.5F, 0.5F);
            state.submit(poseStack, SubmitNodeCollector.immediate(buffers), light, overlay, 0);
            poseStack.popPose();
        } finally {
            state.clear();
            depth--;
        }
    }

    static final class Overrides extends ItemOverrides {
        private final ItemStackRenderState scratch = new ItemStackRenderState();

        @Override
        public @Nullable BakedModel resolve(
                BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            BakedModel result = model;
            if (model instanceof Model m
                    && Minecraft.getInstance().isSameThread()
                    && ClientItems.model(stack.getItem()) != null) {
                // GUI lighting (flat vs 3D) is chosen from the model before rendering: resolve for GUI
                ItemModelResolver.get()
                        .updateForTopItem(scratch, stack, ItemDisplayContext.GUI, level, ItemOwner.of(entity), seed);
                result = scratch.usesBlockLight() ? m.blockLit : m.flat;
                scratch.clear();
            }
            // recorded last: the scratch resolve above may resolve other stacks' models
            capturedStack = stack;
            capturedLevel = level;
            capturedEntity = entity;
            capturedSeed = seed;
            return result;
        }
    }

    /** Inventory model of a definition-rendered item (two variants: GUI block light / flat light). */
    public static final class Model implements BakedModel {
        private static final Overrides OVERRIDES = new Overrides();
        private final BakedModel base;
        private final boolean usesBlockLight;
        final Model blockLit;
        final Model flat;

        public Model(BakedModel base) {
            this.base = base;
            this.usesBlockLight = base.usesBlockLight();
            this.blockLit = usesBlockLight ? this : new Model(base, true, this);
            this.flat = usesBlockLight ? new Model(base, false, this) : this;
        }

        private Model(BakedModel base, boolean usesBlockLight, Model other) {
            this.base = base;
            this.usesBlockLight = usesBlockLight;
            this.blockLit = usesBlockLight ? this : other;
            this.flat = usesBlockLight ? other : this;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public boolean isGui3d() {
            return true;
        }

        @Override
        public boolean usesBlockLight() {
            return usesBlockLight;
        }

        @Override
        public boolean isCustomRenderer() {
            return true;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return base.getParticleIcon();
        }

        /** The base model's transforms, for 1.21.1 readers (item entity bob height etc.). */
        @Override
        public ItemTransforms getTransforms() {
            return base.getTransforms();
        }

        /** The 26.x layers apply the display transforms themselves. */
        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
            return this;
        }

        @Override
        public ItemOverrides getOverrides() {
            return OVERRIDES;
        }
    }
}

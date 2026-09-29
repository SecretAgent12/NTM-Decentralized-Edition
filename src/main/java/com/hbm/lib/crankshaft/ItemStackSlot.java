// Ported from CrankShaft (https://github.com/Warfactory-Official/CrankShaft), MIT License.
// Copyright (c) 2026 movblock; portions Copyright (c) 2021-2024 Jozufozu (Flywheel, MIT).
// Modified by SecretAgent12 (NTM 1.21.1 backport): package relocated, ported to Flywheel 1.0 / Minecraft 1.21.1
// SPDX-License-Identifier: MIT (full license text: NOTICE.md in this directory)

package com.hbm.lib.crankshaft;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import com.hbm.backport.client.flywheel.VisualizerRegistry;
import net.minecraft.client.Minecraft;
import com.hbm.backport.client.itemmodel.ItemStackRenderState;
import com.hbm.backport.client.itemmodel.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/**
 * One stack a host draws through its item's {@link ItemStackVisualizer}: creates, updates, poses and deletes the
 * {@link ItemStackVisual}. One host thread at a time.
 */
public final class ItemStackSlot {
    private final ItemStackRenderState state = new ItemStackRenderState();
    private final Matrix4f itemTransform = new Matrix4f();
    private final Matrix4f pose = new Matrix4f();
    private ItemStack stack = ItemStack.EMPTY;
    @Nullable
    private ItemDisplayContext displayContext;
    @Nullable
    private ItemStackVisual visual;
    private boolean hidden;
    // The model animates its local transform or layers: re-resolved every frame, as vanilla does every stack.
    private boolean animated;

    public static boolean isVisualized(ItemStack stack) {
        return !stack.isEmpty() && VisualizerRegistry.getVisualizer(stack.getItem()) != null;
    }

    /**
     * @param pose where vanilla submits the stack's {@code ItemStackRenderState}: before the item model's display
     *             transform.
     * @return {@code false}, deleting any visual, when the item has no visualizer.
     */
    public boolean draw(VisualizationContext ctx, ItemStack stack, ItemDisplayContext displayContext,
                        @Nullable ItemOwner owner, int seed, Matrix4fc pose, int light, int overlay,
                        float partialTick) {
        ItemStackVisualizer visualizer = stack.isEmpty() ? null : VisualizerRegistry.getVisualizer(stack.getItem());
        if (visualizer == null) {
            delete();
            return false;
        }
        if (visual == null || stack.getItem() != this.stack.getItem() || displayContext != this.displayContext) {
            delete();
            create(visualizer, ctx, stack, displayContext, owner, seed);
        } else if (!ItemStack.matches(stack, this.stack)) {
            this.stack = stack.copy();
            itemTransform(stack, displayContext, owner, seed);
            if (!visual.update(this.stack)) {
                delete();
                create(visualizer, ctx, stack, displayContext, owner, seed);
            }
        } else if (animated) {
            itemTransform(this.stack, displayContext, owner, seed);
        }
        visual.beginFrame(this.pose.set(pose).mul(itemTransform), light, overlay, partialTick);
        hidden = false;
        return true;
    }

    public void hide() {
        if (visual != null && !hidden) {
            visual.hide();
            hidden = true;
        }
    }

    public void delete() {
        if (visual != null) {
            visual.delete();
            visual = null;
        }
        stack = ItemStack.EMPTY;
        displayContext = null;
    }

    private void create(ItemStackVisualizer visualizer, VisualizationContext ctx, ItemStack stack,
                        ItemDisplayContext displayContext, @Nullable ItemOwner owner, int seed) {
        this.stack = stack.copy();
        this.displayContext = displayContext;
        itemTransform(stack, displayContext, owner, seed);
        visual = visualizer.createVisual(ctx, this.stack, displayContext, owner);
    }

    // ItemStackRenderState.LayerRenderState.applyTransform of the first layer: display transform (left-hand aware) x
    // local transform.
    private void itemTransform(ItemStack stack, ItemDisplayContext displayContext, @Nullable ItemOwner owner,
                               int seed) {
        Minecraft minecraft = Minecraft.getInstance();
        com.hbm.backport.client.itemmodel.ItemModelResolver.get()
                 .updateForTopItem(state, stack, displayContext, minecraft.level, owner, seed);
        PoseStack.Pose layer = new PoseStack().last();
        if (state.activeLayerCount > 0) {
            state.layers[0].applyTransform(layer);
        }
        itemTransform.set(layer.pose());
        animated = state.isAnimated();
    }
}

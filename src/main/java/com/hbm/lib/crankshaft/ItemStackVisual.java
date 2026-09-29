// Ported from CrankShaft (https://github.com/Warfactory-Official/CrankShaft), MIT License.
// Copyright (c) 2026 movblock; portions Copyright (c) 2021-2024 Jozufozu (Flywheel, MIT).
// Modified by SecretAgent12 (NTM 1.21.1 backport): package relocated, ported to Flywheel 1.0 / Minecraft 1.21.1
// SPDX-License-Identifier: MIT (full license text: NOTICE.md in this directory)

package com.hbm.lib.crankshaft;

import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;

/**
 * An item stack drawn inside a host visual (a held, dropped, framed or displayed item). The host creates, poses and
 * deletes it on the host's own threads, frame plan workers included; the visualization manager never sees it.
 * <p>backport: with Flywheel 1.0 the only hosts are NTM's own visuals (ItemStackSlot); vanilla-held, dropped and
 * framed stacks render through the item's regular renderer.
 */
public interface ItemStackVisual {
    /**
     * The host's stack changed; same item and display context.
     *
     * @return {@code false} to be deleted and created anew for this stack.
     */
    boolean update(ItemStack stack);

    /**
     * Draw this frame.
     *
     * @param pose the frame a special item renderer receives for this stack: render-origin relative, the item
     *             model's display transform applied.
     */
    void beginFrame(Matrix4fc pose, int light, int overlay, float partialTick);

    /**
     * Not drawn this frame; {@link #beginFrame} reveals again.
     */
    void hide();

    void delete();
}

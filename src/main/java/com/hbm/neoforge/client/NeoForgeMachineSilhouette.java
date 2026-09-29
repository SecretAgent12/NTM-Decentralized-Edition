// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import com.hbm.client.render.MachinePlacementOutline;
import com.hbm.client.render.MachineSilhouette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.core.BlockOutlineRenderState;
import com.hbm.backport.client.core.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.common.NeoForge;

final class NeoForgeMachineSilhouette {
    private NeoForgeMachineSilhouette() {}

    static void register() {
        // backport: NeoForge 26 ExtractBlockOutlineRenderStateEvent + custom outline renderer ->
        // 1.21.1 RenderHighlightEvent.Block. A custom renderer returning true replaced the vanilla
        // outline, which here is cancelling the event.
        // backport: unverified: "true = skip vanilla outline" inferred from the 26.x call sites.
        NeoForge.EVENT_BUS.addListener(
                (RenderHighlightEvent.Block event) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level == null) return;
                    BlockPos pos = event.getTarget().getBlockPos();
                    BlockState blockState = mc.level.getBlockState(pos);
                    // 26.x extracts an outline only for a non-air block inside the world border
                    if (blockState.isAir() || !mc.level.getWorldBorder().isWithinBounds(pos)) return;
                    var placement =
                            MachinePlacementOutline.extract(mc.level, event.getTarget(), mc.player);
                    MachineSilhouette.State state =
                            placement != null
                                    ? null
                                    : MachineSilhouette.extract(
                                            mc.level,
                                            pos,
                                            blockState,
                                            event.getCamera().getPosition());
                    if (placement == null && state == null) return;

                    BlockOutlineRenderState outline =
                            BlockOutlineRenderState.of(
                                    pos,
                                    ItemBlockRenderTypes.getChunkRenderType(blockState)
                                            == RenderType.translucent(),
                                    blockState.getShape(
                                            mc.level,
                                            pos,
                                            CollisionContext.of(event.getCamera().getEntity())));
                    LevelRenderState levelState = LevelRenderState.of(event.getCamera(), outline);
                    SubmitNodeCollector collector =
                            SubmitNodeCollector.immediate(event.getMultiBufferSource());
                    if (placement != null) {
                        MachinePlacementOutline.submit(
                                placement, outline, collector, event.getPoseStack(), levelState);
                    } else {
                        // 26.x windowRenderState.appropriateLineWidth = 1.21.1 RenderStateShard
                        // LineStateShard's default width
                        float width = Math.max(2.5F, mc.getWindow().getWidth() / 1920.0F * 2.5F);
                        MachineSilhouette.submit(
                                state, outline, collector, event.getPoseStack(), levelState, width);
                    }
                    event.setCanceled(true);
                });
    }
}

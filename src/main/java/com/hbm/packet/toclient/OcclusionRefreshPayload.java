// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.NotNull;

public record OcclusionRefreshPayload(long chunkPos) implements CustomPacketPayload {

    public static final Type<OcclusionRefreshPayload> TYPE =
            new Type<>(Library.id("occlusion_refresh"));

    public static final StreamCodec<ByteBuf, OcclusionRefreshPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeLong(payload.chunkPos),
                    buf -> new OcclusionRefreshPayload(buf.readLong()));

    public static void handle(OcclusionRefreshPayload payload, IPayloadHandlerContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        LevelRenderer lr = mc.levelRenderer;
        ClientLevel level = mc.level;
        if (level == null) return;
        int cx = ChunkPos.getX(payload.chunkPos);
        int cz = ChunkPos.getZ(payload.chunkPos);
        LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
        if (chunk == null) return;

        // backport: 26.x reset() the stale mesh of every section that became all-air and
        // called SectionOcclusionGraph.schedulePropagationFrom(section) for every section of the
        // resent chunk. 1.21.1 has neither as public API (RenderSection.reset is private,
        // ViewArea.getRenderSectionAt protected, no schedulePropagationFrom); its equivalent is
        // marking the sections dirty: the rebuild sets the new CompiledSection (EMPTY for an
        // all-air section, so the stale mesh is dropped) and RenderSection.setCompiled ->
        // LevelRenderer.addRecentlyCompiledSection -> SectionOcclusionGraph.onSectionCompiled
        // queues the occlusion propagation from that section. Difference: the stale mesh stays
        // until the rebuild lands (normally the next frames) instead of vanishing at once.
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            lr.setSectionDirty(cx, chunk.getSectionYFromSectionIndex(i), cz);
        }
    }

    @Override
    public @NotNull Type<OcclusionRefreshPayload> type() {
        return TYPE;
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.backport.SubLevelSpace;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.blocks.network.FluidDuctBlockBase;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

public final class FluidPipeTintRequestPayload extends ThreadedPayload {

    public static final Type<FluidPipeTintRequestPayload> TYPE =
            new Type<>(Library.id("fluid_pipe_tint_request"));
    public static final StreamCodec<ByteBuf, FluidPipeTintRequestPayload> STREAM_CODEC =
            streamCodec(FluidPipeTintRequestPayload::decode);

    private final long[] posKeys;

    public FluidPipeTintRequestPayload(long[] posKeys) {
        this.posKeys = posKeys;
    }

    private static FluidPipeTintRequestPayload decode(ByteBuf buf) {
        int count = buf.readInt();
        if (count < 0 || count > buf.readableBytes() / Long.BYTES) {
            throw new DecoderException("fluid_pipe_tint_request count " + count);
        }
        long[] posKeys = new long[count];
        for (int i = 0; i < count; i++) posKeys[i] = buf.readLong();
        return new FluidPipeTintRequestPayload(posKeys);
    }

    public static void handle(FluidPipeTintRequestPayload payload, IPayloadHandlerContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        ServerLevel level = player.serverLevel();
        Reference2ObjectOpenHashMap<Fluid, LongArrayList> byFluid =
                new Reference2ObjectOpenHashMap<>();
        // backport-fix: BF-029 (DE 2026-10-02, commit 4a19c959) DE skipped keys beyond the view distance (+1 chunk)
        // and in unloaded chunks, so a forged packet could no longer load chunks on the main thread. NEXT 1.0.1
        // (2026-10-05) fixed it with the player's tracked chunks and blockStateIfLoaded; NEXT's code kept.
        ChunkTrackingView view = player.getChunkTrackingView();
        for (long key : payload.posKeys) {
            BlockPos pos = BlockPos.of(key);
            if (!withinView(level, pos, view)) continue;
            BlockState state = ChunkUtil.blockStateIfLoaded(level, pos);
            if (state == null || !(state.getBlock() instanceof FluidDuctBlockBase)) continue;
            PipeData data = FluidPipeGraph.dataAt(level, key);
            if (data == null) continue;
            byFluid.computeIfAbsent(data.fluid(), f -> new LongArrayList()).add(key);
        }
        for (Reference2ObjectMap.Entry<Fluid, LongArrayList> entry :
                byFluid.reference2ObjectEntrySet()) {
            FluidPipeTintData.syncTo(player, entry.getValue().toLongArray(), entry.getKey());
        }
    }

    /**
     * backport-fix: BF-078 a pipe on a physics contraption sits in the contraption's plot, millions
     * of blocks from the player: its position is checked where the contraption is in the world.
     * Without it every pipe on a contraption was refused and drew as "no fluid" until the
     * contraption was taken apart.
     */
    private static boolean withinView(ServerLevel level, BlockPos pos, ChunkTrackingView view) {
        if (view.contains(new ChunkPos(pos))) return true;
        if (!SubLevelSpace.inSubLevel(level, pos.getX(), pos.getZ())) return false;
        Vec3 world = SubLevelSpace.toWorld(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return view.contains(new ChunkPos(Mth.floor(world.x) >> 4, Mth.floor(world.z) >> 4));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(posKeys.length);
        for (long posKey : posKeys) buf.writeLong(posKey);
    }

    @Override
    public @NotNull Type<FluidPipeTintRequestPayload> type() {
        return TYPE;
    }
}

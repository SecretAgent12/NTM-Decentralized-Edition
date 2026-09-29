// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.injected.IChunkExtension;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(IOWorker.class)
public abstract class MixinIOWorker {

    @Shadow
    public abstract RegionStorageInfo storageInfo();

    /**
     * backport: 26.x injects at RETURN of the loadAsync lambda ("lambda$loadAsync$0", which returns
     * the Optional). In 1.21.1 that lambda is lambda$loadAsync$4 and returns an
     * Either&lt;Optional, Exception&gt;, so the sidecar is merged in a stage appended to the
     * returned future instead: every caller receives the future after the merge, and the merge
     * still runs on the IO worker thread when the read completes there.
     */
    @ModifyReturnValue(method = "loadAsync", at = @At("RETURN"))
    private CompletableFuture<Optional<CompoundTag>> hbm$readRadiationSidecar(
            CompletableFuture<Optional<CompoundTag>> future, @Local(argsOnly = true) ChunkPos pos) {
        RegionStorageInfo info = this.storageInfo();
        if (!"chunk".equals(info.type())) return future;
        return future.thenApply(
                result -> {
                    if (result == null || result.isEmpty()) return result;
                    byte[] rad = RadiationSystemNT.readSidecar(info.dimension(), pos);
                    if (rad != null)
                        result.get().putByteArray(IChunkExtension.RADIATION_NBT_KEY, rad);
                    return result;
                });
    }
}

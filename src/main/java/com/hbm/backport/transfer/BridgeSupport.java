// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.transfer;

import com.hbm.lib.neotransfer.access.ItemAccess;
import com.hbm.lib.neotransfer.item.ItemResource;
import com.hbm.lib.neotransfer.transaction.Transaction;
import com.hbm.lib.neotransfer.transaction.TransactionContext;
import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/** Shared helpers for the capability bridge. */
final class BridgeSupport {

    static final Logger LOGGER = LogUtils.getLogger();

    private BridgeSupport() {}

    private static final int MAX_NESTED_EXECUTE_LOGS = 16;
    private static final AtomicInteger NESTED_EXECUTE_LOGS = new AtomicInteger();

    /**
     * Opens the transaction a 1.21.1-style (simulate/execute) call runs in.
     *
     * <ul>
     *   <li>No transaction open (or root-commit callbacks running): a new root transaction.
     *   <li>A transaction is open on this thread (a foreign handler, called by the mod inside a transaction,
     *       calls back into a mod handler): a SIMULATE runs in a nested transaction that is aborted locally.
     *       An EXECUTE is refused ({@code null}): it would only become final together with the enclosing
     *       transaction, but the foreign caller has no rollback - if the enclosing transaction aborts (every
     *       "how much fits" probe of the mod does) the foreign half of the move stays done and items/fluid/energy
     *       are duplicated or deleted. The bridge's own imported handlers never execute foreign handlers inside
     *       an open transaction (they execute from root-commit callbacks, lifecycle ROOT_CLOSING), so this path
     *       is only reached by contract-violating foreign code (execute from within a simulate) or foreign code
     *       triggered as a side effect of mod code running inside a transaction.
     *   <li>A transaction is closing (inside close callbacks): no transaction can be opened; returns
     *       {@code null} and the caller reports "nothing moved".
     * </ul>
     */
    @SuppressWarnings("deprecation")
    static @Nullable Transaction open(boolean execute) {
        return switch (Transaction.getLifecycle()) {
            case OPEN -> {
                if (execute) {
                    if (NESTED_EXECUTE_LOGS.getAndIncrement() < MAX_NESTED_EXECUTE_LOGS) {
                        LOGGER.error(
                                "Capability bridge: refused a 1.21.1 EXECUTE call made while a transfer transaction"
                                        + " is open (it could not be rolled back on the caller's side)",
                                new Throwable("call site"));
                    }
                    yield null;
                }
                yield Transaction.open(Transaction.getCurrentOpenedTransaction());
            }
            case CLOSING -> null;
            case NONE, ROOT_CLOSING -> Transaction.openRoot();
        };
    }

    // ------------------------------------------------------------------ threading

    /**
     * Whether the calling thread is the logical server's main thread. The bridge's static caches, the
     * {@link Site} bookkeeping and the imported wrappers' pending state are not thread-safe and are only used on
     * that thread; other threads (client thread, worker threads) get uncached {@link Site#DETACHED} wrappers,
     * which can be read but refuse to record deferred work.
     */
    static boolean onServerThread() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSameThread();
    }

    /** {@link #onServerThread()} for a lookup in {@code level}: client levels are never "server thread". */
    static boolean onServerThread(Level level) {
        return level instanceof ServerLevel serverLevel && serverLevel.getServer().isSameThread();
    }

    // ------------------------------------------------------------------ per-position coordination

    /** An imported (deferred-execution) wrapper. */
    interface Deferred {
        /**
         * {@code true} while the wrapper has work that is not yet applied to its foreign handler: changes in a
         * still-open transaction, committed work waiting for its root-commit callback, or work being applied.
         */
        boolean hasWork();
    }

    /**
     * All imported wrappers of one capability kind (item, fluid or energy) at one block position.
     *
     * <p>NeoForge 1.21.1 builds a NEW foreign handler object (e.g. {@code InvWrapper}/{@code SidedInvWrapper})
     * for every capability lookup, and different sides of one block usually share one inventory. Two wrappers
     * simulating independently against the same unmodified foreign state would both be granted the same
     * space/items and duplicate or delete at commit. Therefore:
     *
     * <ul>
     *   <li>lookups on the same side return the same wrapper while it {@linkplain Deferred#hasWork has work}
     *       ({@link TransferBridge}); an idle wrapper is replaced by a fresh one around the fresh foreign handler,
     *       so stale handlers are not kept;
     *   <li>only one wrapper per site may have work at a time ({@link #claim}); any other wrapper for the same
     *       position (other side, or held from an earlier lookup, e.g. in a {@code BlockCapabilityCache}) refuses
     *       work (returns 0) until the owner's work is applied or aborted.
     * </ul>
     *
     * <p>Server thread only (see {@link #onServerThread()}); no synchronisation.
     */
    static final class Site {
        /** Wrappers created off the server thread: readable, but {@link #claim} always refuses. */
        static final Site DETACHED = new Site(null, null, 0);

        final @Nullable Level level;
        private final @Nullable ResourceKey<Level> dimension;
        final long pos;
        /** Per-side wrapper of the last lookup: index {@code Direction.ordinal()}, 6 = no side. */
        final Object[] wrappers = new Object[7];
        private @Nullable Deferred owner;

        Site(@Nullable Level level, @Nullable ResourceKey<Level> dimension, long pos) {
            this.level = level;
            this.dimension = dimension;
            this.pos = pos;
        }

        /** Whether {@code wrapper} may record work now; if so it becomes the owner. */
        boolean claim(Deferred wrapper) {
            if (this == DETACHED) return false;
            if (owner != null && owner != wrapper && owner.hasWork()) return false;
            owner = wrapper;
            return true;
        }

        BlockPos blockPos() {
            return BlockPos.of(pos);
        }

        @Override
        public String toString() {
            return this == DETACHED ? "detached" : dimension.location() + " " + blockPos().toShortString();
        }
    }

    /** {@code true} if a wrapper bound to {@code site} (null = unkeyed, identity-cached) may record work now. */
    static boolean claim(@Nullable Site site, Deferred wrapper) {
        return site == null || site.claim(wrapper);
    }

    /** Log context for a wrapper: its position if known, else the foreign handler. */
    static Object where(@Nullable Site site, Object delegate) {
        return site == null ? delegate : site + " (" + delegate + ")";
    }

    /**
     * Last resort for items the foreign handler would not take back at commit: drop them into the world at the
     * wrapper's position rather than delete them. Returns {@code false} (the caller logs the loss) when no
     * position is known (unkeyed wrapper) or the level is not a server level.
     */
    static boolean dropAtSite(@Nullable Site site, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (site == null || !(site.level instanceof ServerLevel level)) return false;
        BlockPos pos = site.blockPos();
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
        return true;
    }

    static int clampInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) value;
    }

    /**
     * Amount newly accepted by a deferred operation. {@code pending} is the signed net amount already promised in
     * this direction's favour ({@code > 0} = same direction as the new request, {@code < 0} = opposite direction),
     * {@code request} the new amount, and {@code simulate} simulates the COMBINED net operation against the real
     * handler (only called when the net result is in the request's direction).
     */
    static int deferredAccept(long pending, int request, java.util.function.IntUnaryOperator simulate) {
        long target = pending + request;
        if (target <= 0) return request; // only cancels out opposite pending work: always possible
        int simulated = Math.max(0, simulate.applyAsInt(clampInt(target)));
        long accepted = Math.max(-pending, 0) + Math.max(0, simulated - Math.max(pending, 0));
        return (int) Math.max(0, Math.min(accepted, request));
    }

    /**
     * Replaces the item held by {@code access}: {@code count} items of {@code before} become {@code after}
     * ({@code after} describes the result for ONE source item; it may be empty = consumed). Returns how many
     * source items were converted.
     */
    static int exchange(ItemAccess access, ItemResource before, int count, ItemStack after, TransactionContext tx) {
        if (after.isEmpty()) return access.extract(before, count, tx);
        ItemResource afterResource = ItemResource.of(after);
        int per = after.getCount();
        if (per == 1) return access.exchange(afterResource, count, tx);
        long want = (long) count * per;
        if (want > Integer.MAX_VALUE) return 0;
        try (Transaction sub = Transaction.open(tx)) {
            if (access.extract(before, count, sub) != count) return 0;
            if (access.insert(afterResource, (int) want, sub) != want) return 0;
            sub.commit();
            return count;
        }
    }

    /** Makes {@code target}'s component patch equal to {@code source}'s, in place. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void syncComponents(ItemStack target, ItemStack source) {
        DataComponentPatch wanted = source.getComponentsPatch();
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : target.getComponentsPatch().entrySet()) {
            DataComponentType type = entry.getKey();
            if (wanted.get(type) != null) continue;
            Object prototype = target.getPrototype().get(type);
            // Setting the prototype value drops the patch entry; removing a non-prototype component drops it too.
            if (prototype != null) target.set(type, prototype);
            else target.remove(type);
        }
        target.applyComponents(wanted);
    }
}

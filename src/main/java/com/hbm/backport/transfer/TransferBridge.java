// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.transfer;

import com.google.common.collect.MapMaker;
import com.hbm.lib.neotransfer.ResourceHandler;
import com.hbm.lib.neotransfer.access.ItemAccess;
import com.hbm.lib.neotransfer.energy.EnergyHandler;
import com.hbm.lib.neotransfer.fluid.FluidResource;
import com.hbm.lib.neotransfer.item.ItemResource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import org.jspecify.annotations.Nullable;

/**
 * Connects the mod's transfer-API capabilities ({@link Capabilities}) with NeoForge 1.21.1's
 * {@code IItemHandler}/{@code IFluidHandler}/{@code IEnergyStorage} capabilities.
 *
 * <p>Call {@link #register} once, at the END of the mod's {@code RegisterCapabilitiesEvent} handler, after all of
 * the mod's own providers are registered. For each capability pair (item/block, fluid/block, energy/block,
 * fluid/item, energy/item) and each registered block/item it then decides once:
 *
 * <ul>
 *   <li><b>native</b> (the mod registered a provider of its transfer capability for it): EXPORT - register the
 *       1.21.1 capability for it, answering by querying the mod's capability and wrapping the result, so other mods
 *       see IItemHandler/IFluidHandler/IEnergyStorage;
 *   <li><b>everything else</b>: IMPORT - register the mod's transfer capability for it, answering by querying the
 *       1.21.1 capability and wrapping the result, so the mod's pipes/machines reach chests, foreign tanks, cables.
 * </ul>
 *
 * <p><b>No loops by construction:</b> per capability pair, a block/item gets either the export provider or the
 * import provider, never both, so an exported handler is never re-imported for the same block/item. Wrappers are
 * also unwrapped instead of stacked when a foreign proxy block hands one of ours back (or a mod provider returns
 * an imported one). As a last line of defence against foreign proxy blocks pointing back at each other, the bridge
 * providers carry a per-thread depth guard ({@value #MAX_DEPTH}) that answers {@code null} when exceeded. The guard
 * covers lookups only; re-entrant calls while imported work is applied at commit are bounded by the transaction
 * manager's root-commit queue (see {@link ImportedItemHandler}).
 *
 * <p><b>Ordering:</b> NeoForge 1.21.1 queries a block's/item's providers in registration order and returns the
 * first non-null result (BlockCapability#getCapability / ItemCapability#getCapability). Only this mod registers
 * providers for its own transfer capabilities, so the import provider is the only one on foreign blocks; export
 * providers are appended after whatever else might exist for the mod's blocks. Blocks whose state is air are
 * skipped. The native/foreign split is taken from {@code isBlockRegistered}/{@code isItemRegistered} at call time,
 * hence "call last".
 *
 * <p><b>Import wrapper identity.</b> NeoForge 1.21.1 builds a new foreign handler object on every lookup, so
 * block imports are keyed by position, not by handler identity: see {@link BridgeSupport.Site} (same wrapper per
 * position/side while it has work; any other wrapper of that position refuses work meanwhile). The public
 * {@code from*} adapters, which get no position, cache by handler identity only - callers that look up a foreign
 * block capability themselves and wrap it lose the per-position protection (use {@link Capabilities} lookups).
 *
 * <p><b>Threading.</b> The deferred import state and the {@link BridgeSupport.Site} bookkeeping are server-thread
 * only: off that thread imports are uncached {@link BridgeSupport.Site#DETACHED} wrappers that can be read but
 * refuse to move anything, and item-access sharing is skipped. The export caches only hold stateless adapters in
 * concurrent maps, so they are safe from any thread.
 */
public final class TransferBridge {

    private static final int MAX_DEPTH = 8;
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);
    private static boolean registered;

    // Stateless export adapters (any thread).
    private static final ConcurrentMap<Object, IItemHandler> ITEM_EXPORTS = weakCache();
    private static final ConcurrentMap<Object, IFluidHandler> FLUID_EXPORTS = weakCache();
    private static final ConcurrentMap<Object, IEnergyStorage> ENERGY_EXPORTS = weakCache();

    // Stateful import wrappers, keyed by foreign handler identity (public adapters; server thread only).
    private static final ConcurrentMap<Object, ResourceHandler<ItemResource>> ITEM_IMPORTS = weakCache();
    private static final ConcurrentMap<Object, ResourceHandler<FluidResource>> FLUID_IMPORTS = weakCache();
    private static final ConcurrentMap<Object, EnergyHandler> ENERGY_IMPORTS = weakCache();

    // Block import sites, keyed by position (server thread only). Weak values: a site lives while any of its
    // wrappers is referenced (by mod code, or by an open transaction / the root-commit queue via its journal).
    private static final ConcurrentMap<SiteKey, BridgeSupport.Site> ITEM_SITES = weakValueCache();
    private static final ConcurrentMap<SiteKey, BridgeSupport.Site> FLUID_SITES = weakValueCache();
    private static final ConcurrentMap<SiteKey, BridgeSupport.Site> ENERGY_SITES = weakValueCache();

    // Item-export accesses per live stack, shared while active (server thread only).
    private static final ConcurrentMap<ItemStack, ContainerItemAccess> CONTAINER_ACCESSES = weakCache();

    private record SiteKey(ResourceKey<Level> dimension, long pos) {}

    private TransferBridge() {}

    private static <K, V> ConcurrentMap<K, V> weakCache() {
        return new MapMaker().weakKeys().weakValues().makeMap(); // weak keys compare by identity
    }

    private static <K, V> ConcurrentMap<K, V> weakValueCache() {
        return new MapMaker().weakValues().makeMap(); // strong keys compare by equals
    }

    // ------------------------------------------------------------------ adapters (public API)

    /** Transfer-API item handler as a 1.21.1 IItemHandler. */
    public static IItemHandler toItemHandler(ResourceHandler<ItemResource> handler) {
        if (handler instanceof ImportedItemHandler imported) return imported.delegate;
        return ITEM_EXPORTS.computeIfAbsent(handler, h -> new ExportedItemHandler(handler));
    }

    /** 1.21.1 IItemHandler as a transfer-API item handler (deferred to root commit, see ImportedItemHandler). */
    public static ResourceHandler<ItemResource> fromItemHandler(IItemHandler handler) {
        ResourceHandler<ItemResource> own = unwrapItem(handler);
        if (own != null) return own;
        if (!BridgeSupport.onServerThread()) return new ImportedItemHandler(handler, BridgeSupport.Site.DETACHED);
        return ITEM_IMPORTS.computeIfAbsent(handler, h -> new ImportedItemHandler(handler, null));
    }

    /** Transfer-API fluid handler as a 1.21.1 IFluidHandler. */
    public static IFluidHandler toFluidHandler(ResourceHandler<FluidResource> handler) {
        if (handler instanceof ImportedFluidHandler imported) return imported.delegate;
        return FLUID_EXPORTS.computeIfAbsent(handler, h -> new ExportedFluidHandler(handler));
    }

    /** 1.21.1 IFluidHandler as a transfer-API fluid handler (deferred to root commit, see ImportedFluidHandler). */
    public static ResourceHandler<FluidResource> fromFluidHandler(IFluidHandler handler) {
        ResourceHandler<FluidResource> own = unwrapFluid(handler);
        if (own != null) return own;
        if (!BridgeSupport.onServerThread()) return new ImportedFluidHandler(handler, BridgeSupport.Site.DETACHED);
        return FLUID_IMPORTS.computeIfAbsent(handler, h -> new ImportedFluidHandler(handler, null));
    }

    /** Transfer-API energy handler as a 1.21.1 IEnergyStorage. */
    public static IEnergyStorage toEnergyStorage(EnergyHandler handler) {
        if (handler instanceof ImportedEnergyHandler imported) return imported.delegate;
        return ENERGY_EXPORTS.computeIfAbsent(handler, h -> new ExportedEnergyStorage(handler));
    }

    /** 1.21.1 IEnergyStorage as a transfer-API energy handler (deferred to root commit, see ImportedEnergyHandler). */
    public static EnergyHandler fromEnergyStorage(IEnergyStorage storage) {
        EnergyHandler own = unwrapEnergy(storage);
        if (own != null) return own;
        if (!BridgeSupport.onServerThread()) return new ImportedEnergyHandler(storage, BridgeSupport.Site.DETACHED);
        return ENERGY_IMPORTS.computeIfAbsent(storage, h -> new ImportedEnergyHandler(storage, null));
    }

    private static @Nullable ResourceHandler<ItemResource> unwrapItem(IItemHandler handler) {
        return handler instanceof ExportedItemHandler exported ? exported.handler : null;
    }

    private static @Nullable ResourceHandler<FluidResource> unwrapFluid(IFluidHandler handler) {
        return handler instanceof ExportedFluidHandler exported && !(handler instanceof IFluidHandlerItem)
                ? exported.handler
                : null;
    }

    private static @Nullable EnergyHandler unwrapEnergy(IEnergyStorage storage) {
        return storage instanceof ExportedEnergyStorage exported && exported.access == null ? exported.handler : null;
    }

    /**
     * Block import at a position (see {@link BridgeSupport.Site}): returns the wrapper of the last lookup of this
     * position/side while it has work, else a fresh wrapper around {@code handler} bound to the position's site.
     */
    @SuppressWarnings("unchecked")
    private static <L, M> M importAt(
            ConcurrentMap<SiteKey, BridgeSupport.Site> sites,
            Level level,
            BlockPos pos,
            @Nullable Direction side,
            L handler,
            Function<L, @Nullable M> unwrap,
            BiFunction<L, BridgeSupport.@Nullable Site, M> create) {
        M own = unwrap.apply(handler);
        if (own != null) return own;
        if (!BridgeSupport.onServerThread(level)) return create.apply(handler, BridgeSupport.Site.DETACHED);
        SiteKey key = new SiteKey(level.dimension(), pos.asLong());
        BridgeSupport.Site site = sites.get(key);
        if (site == null || site.level != level) { // absent, or left over from an unloaded/replaced Level
            site = new BridgeSupport.Site(level, key.dimension(), key.pos());
            sites.put(key, site);
        }
        int index = side == null ? 6 : side.ordinal();
        Object cached = site.wrappers[index];
        if (cached instanceof BridgeSupport.Deferred deferred && deferred.hasWork()) return (M) cached;
        M fresh = create.apply(handler, site);
        site.wrappers[index] = fresh;
        return fresh;
    }

    private static ResourceHandler<ItemResource> importItems(
            IItemHandler handler, Level level, BlockPos pos, @Nullable Direction side) {
        return importAt(ITEM_SITES, level, pos, side, handler, TransferBridge::unwrapItem, ImportedItemHandler::new);
    }

    private static ResourceHandler<FluidResource> importFluids(
            IFluidHandler handler, Level level, BlockPos pos, @Nullable Direction side) {
        return importAt(FLUID_SITES, level, pos, side, handler, TransferBridge::unwrapFluid, ImportedFluidHandler::new);
    }

    private static EnergyHandler importEnergy(
            IEnergyStorage handler, Level level, BlockPos pos, @Nullable Direction side) {
        return importAt(ENERGY_SITES, level, pos, side, handler, TransferBridge::unwrapEnergy, ImportedEnergyHandler::new);
    }

    /**
     * The mod's fluid-item view of a foreign item that has a 1.21.1 {@code IFluidHandlerItem}, bound to
     * {@code access}; {@code null} if the item has none. The probe only obtains the capability of a single-item
     * copy (no transfer). The wrapper is stateless over {@code access}, so no per-slot cache is needed.
     */
    public static @Nullable ResourceHandler<FluidResource> fromFluidItem(ItemStack stack, ItemAccess access) {
        IFluidHandlerItem probe = ImportedFluidItemHandler.handlerOf(stack.copyWithCount(1));
        if (probe == null || probe.getTanks() <= 0) return null;
        return new ImportedFluidItemHandler(access, probe.getTanks());
    }

    /** The mod's energy view of a foreign item with a 1.21.1 {@code IEnergyStorage}; {@code null} if none. */
    public static @Nullable EnergyHandler fromEnergyItem(ItemStack stack, ItemAccess access) {
        return ImportedEnergyItemHandler.storageOf(stack.copyWithCount(1)) == null
                ? null
                : new ImportedEnergyItemHandler(access);
    }

    /** 1.21.1 {@code IFluidHandlerItem} for one of the mod's fluid items; {@code null} if it answers none. */
    public static @Nullable IFluidHandlerItem toFluidItem(ItemStack stack) {
        ContainerItemAccess access = containerAccess(stack);
        ResourceHandler<FluidResource> handler = Capabilities.Fluid.ITEM.getCapability(stack, access);
        return handler == null ? null : new ExportedFluidHandlerItem(handler, access);
    }

    /**
     * 1.21.1 {@code IEnergyStorage} for one of the mod's energy items; {@code null} if it answers none. Operations
     * that would replace the item or change the count are refused (see {@link ExportedEnergyStorage}); component
     * changes are written back into {@code stack} with the concurrent-change guard of {@link ContainerItemAccess}.
     */
    public static @Nullable IEnergyStorage toEnergyItem(ItemStack stack) {
        ContainerItemAccess access = containerAccess(stack);
        EnergyHandler handler = Capabilities.Energy.ITEM.getCapability(stack, access);
        return handler == null ? null : new ExportedEnergyStorage(handler, access);
    }

    /**
     * The access for an item export over {@code stack}: lookups of the same live stack share one access while it
     * has journaled changes in an open transaction (and still holds the same item and count - otherwise the mod's
     * handler for {@code stack}'s item would be bound to a different item), so two exported handlers cannot both
     * diverge from and then overwrite the same stack. Anything that still slips through is caught by
     * {@link ContainerItemAccess}'s write-back guard. Off the server thread: never shared.
     */
    private static ContainerItemAccess containerAccess(ItemStack stack) {
        if (!BridgeSupport.onServerThread()) return new ContainerItemAccess(stack);
        ContainerItemAccess access = CONTAINER_ACCESSES.get(stack);
        if (access != null && access.isActive() && access.keepsItemAndCount()) return access;
        access = new ContainerItemAccess(stack);
        CONTAINER_ACCESSES.put(stack, access);
        return access;
    }

    // ------------------------------------------------------------------ registration

    /** See class doc. Call once, after the mod's own capability providers are registered. */
    public static synchronized void register(RegisterCapabilitiesEvent event) {
        if (registered) return;
        registered = true;

        bridgeBlocks(
                event,
                Capabilities.Item.BLOCK,
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                TransferBridge::toItemHandler,
                TransferBridge::importItems);
        bridgeBlocks(
                event,
                Capabilities.Fluid.BLOCK,
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                TransferBridge::toFluidHandler,
                TransferBridge::importFluids);
        bridgeBlocks(
                event,
                Capabilities.Energy.BLOCK,
                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                TransferBridge::toEnergyStorage,
                TransferBridge::importEnergy);

        bridgeItems(
                event,
                Capabilities.Fluid.ITEM,
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM,
                TransferBridge::toFluidItem,
                TransferBridge::fromFluidItem);
        bridgeItems(
                event,
                Capabilities.Energy.ITEM,
                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM,
                TransferBridge::toEnergyItem,
                TransferBridge::fromEnergyItem);
    }

    private static <M, L> void bridgeBlocks(
            RegisterCapabilitiesEvent event,
            BlockCapability<M, @Nullable Direction> modCap,
            BlockCapability<L, @Nullable Direction> legacyCap,
            Function<M, L> export,
            BlockImporter<L, M> importer) {
        List<Block> natives = new ArrayList<>();
        List<Block> foreign = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block.defaultBlockState().isAir()) continue;
            (event.isBlockRegistered(modCap, block) ? natives : foreign).add(block);
        }
        if (!natives.isEmpty()) {
            event.registerBlock(
                    legacyCap,
                    (level, pos, state, be, side) ->
                            guarded(() -> {
                                M handler = modCap.getCapability(level, pos, state, be, side);
                                return handler == null ? null : export.apply(handler);
                            }),
                    natives.toArray(new Block[0]));
        }
        if (!foreign.isEmpty()) {
            event.registerBlock(
                    modCap,
                    (level, pos, state, be, side) ->
                            guarded(() -> {
                                L handler = legacyCap.getCapability(level, pos, state, be, side);
                                return handler == null ? null : importer.create(handler, level, pos, side);
                            }),
                    foreign.toArray(new Block[0]));
        }
    }

    @FunctionalInterface
    private interface BlockImporter<L, M> {
        M create(L handler, Level level, BlockPos pos, @Nullable Direction side);
    }

    @FunctionalInterface
    private interface ItemImporter<M> {
        @Nullable M create(ItemStack stack, ItemAccess access);
    }

    private static <M, L> void bridgeItems(
            RegisterCapabilitiesEvent event,
            ItemCapability<M, ItemAccess> modCap,
            ItemCapability<L, @Nullable Void> legacyCap,
            Function<ItemStack, @Nullable L> export,
            ItemImporter<M> importer) {
        List<Item> natives = new ArrayList<>();
        List<Item> foreign = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            (event.isItemRegistered(modCap, item) ? natives : foreign).add(item);
        }
        if (!natives.isEmpty()) {
            event.registerItem(
                    legacyCap, (stack, ctx) -> guarded(() -> export.apply(stack)), natives.toArray(new Item[0]));
        }
        if (!foreign.isEmpty()) {
            event.registerItem(
                    modCap,
                    (stack, access) -> guarded(() -> importer.create(stack, access)),
                    foreign.toArray(new Item[0]));
        }
    }

    private static <T> @Nullable T guarded(Supplier<@Nullable T> lookup) {
        int[] depth = DEPTH.get();
        if (depth[0] >= MAX_DEPTH) return null;
        depth[0]++;
        try {
            return lookup.get();
        } finally {
            depth[0]--;
        }
    }
}

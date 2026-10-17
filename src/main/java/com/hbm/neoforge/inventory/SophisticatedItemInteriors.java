// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.inventory;

import com.hbm.handler.UnstableFuses;
import com.hbm.inventory.InventoryAlias;
import com.hbm.inventory.ItemInteriorAdapter;
import com.hbm.inventory.ItemInteriorBackend;
import com.hbm.inventory.ItemInteriors;
import com.hbm.inventory.SlotSource;
import com.hbm.neoforge.hazard.SophisticatedLinkedStorage;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import com.hbm.backport.ItemInstance;
import net.minecraft.world.item.ItemStack;
import com.hbm.backport.ItemStackTemplate;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import com.hbm.lib.neotransfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.infinity.InfinityInventoryPart;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import net.p3pp3rf1y.sophisticatedstorage.item.StackStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import org.jspecify.annotations.Nullable;

/**
 * backport: ported to Sophisticated Core / Backpacks / Storage 1.21.1. There a stored record is a
 * CompoundTag (SophisticatedContentsStorage), not a 26.x ContainerContents whose InventoryData and
 * UpgradeData hold the live stacks every wrapper of the record shares. With no such shared lists,
 * the backend keeps one wrapper bound to the record for as long as the record stands, and that
 * wrapper's InventoryHandler / UpgradeHandler (slot sources through MixinInventoryHandler /
 * MixinUpgradeHandler, which delegate to this backend while it owns them) are the interior's slot
 * sources. A record change (created, replaced, removed) rebuilds the wrapper, as 26.x re-read the
 * record.
 */
public final class SophisticatedItemInteriors {
    private SophisticatedItemInteriors() {}

    public static void registerBackpacks() {
        ItemInteriors.register(Backpacks.ADAPTER);
    }

    public static void registerStorage() {
        ItemInteriors.register(Storage.ADAPTER);
    }

    public static void backpackRecordChanged(UUID id) {
        recordChanged(Backpacks.ADAPTER, id);
    }

    public static void storageRecordChanged(UUID id) {
        recordChanged(Storage.ADAPTER, id);
    }

    private static void recordChanged(ItemInteriorAdapter adapter, UUID id) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
            ItemInteriors.backendChanged(ServerLifecycleHooks.getCurrentServer(), adapter, id);
    }

    private abstract static class Adapter implements ItemInteriorAdapter {
        private final Class<?> type;
        private final boolean endpointExclusion;

        Adapter(Class<?> type, boolean endpointExclusion) {
            this.type = type;
            this.endpointExclusion = endpointExclusion;
        }

        @Override
        public boolean handles(Class<?> type) {
            return this.type.isAssignableFrom(type);
        }

        @Override
        public @Nullable UUID reference(ItemInstance stack) {
            // backport-fix: BF-082 the linked storage endpoint component only exists since
            // Sophisticated Core 1.5.x; read it behind SophisticatedLinkedStorage's presence check
            if (endpointExclusion && SophisticatedLinkedStorage.hasEndpointComponent(stack))
                return null;
            return stack.get(ModCoreDataComponents.STORAGE_UUID.get());
        }
    }

    private abstract static class Backend extends ItemInteriorBackend {
        private static final SlotSource[] EMPTY = new SlotSource[0];
        protected final UUID id;
        protected final ItemStackTemplate prototype;
        private @Nullable CompoundTag contents;
        private SlotSource[] sources = EMPTY;
        private @Nullable IStorageWrapper wrapper;

        Backend(MinecraftServer server, UUID id, ItemStackTemplate prototype) {
            super(server);
            this.id = id;
            this.prototype = prototype;
            changed();
        }

        protected abstract @Nullable CompoundTag readContents();

        protected abstract IStorageWrapper createWrapper();

        protected abstract void persist(IStorageWrapper current);

        protected final CompoundTag contents() {
            return contents;
        }

        @Override
        public SlotSource[] inventorySources() {
            return sources;
        }

        @Override
        public void changed() {
            // backport: 26.x swapped sources only when the record object changed and dropped its
            // throwaway wrapper on every call. Here the wrapper's handlers are the sources and hold
            // their own copy of the stacks, and a 1.21.1 record can be rewritten in place
            // (BackpackStorage.setBackpackContents merges into the existing tag), so every change
            // rebuilds the wrapper from the record.
            CompoundTag current = readContents();
            contents = current;
            wrapper = current == null ? null : createWrapper();
            sources =
                    wrapper == null
                            ? EMPTY
                            : new SlotSource[] {
                                (SlotSource) (Object) wrapper.getInventoryHandler(),
                                (SlotSource) (Object) wrapper.getUpgradeHandler()
                            };
        }

        @Override
        public void slotChanged(SlotSource source, int slot) {
            if (sources.length == 0 || source != sources[1]) return;
            // backport: 26.x compared the partitioner's part layout before and after; 1.21.1's
            // InventoryPartitioner keeps it private, and an upgrade change is what re-partitions,
            // so the inventory is reindexed on every upgrade slot change
            UnstableFuses.reindexSource(server, sources[0]);
        }

        @Override
        public boolean physicalSlot(SlotSource source, int slot) {
            if (sources.length == 0 || slot < 0 || slot >= source.storedSlotCount()) return false;
            if (source == sources[1]) return true;
            if (source != sources[0]) return false;
            InventoryHandler inventory = wrapper.getInventoryHandler();
            return ((InventoryAlias) inventory).inventoryIdentity()
                            == SophisticatedContentsStorage.inventoryIdentity(contents)
                    && slot < inventory.getSlots()
                    && !(inventory.getInventoryPartitioner().getPartBySlot(slot)
                            instanceof InfinityInventoryPart);
        }

        @Override
        public boolean replaceStoredItem(
                SlotSource source, int slot, ItemStack expected, ItemStack replacement) {
            if (inventoryCarrier() == null
                    || Transaction.getLifecycle() != Transaction.Lifecycle.NONE
                    || slot < 0
                    || slot >= source.storedSlotCount()
                    || source.storedItem(slot) != expected) return false;
            if (!physicalSlot(source, slot)) return false;
            IStorageWrapper current = wrapper;
            if (source == sources[0])
                // backport: setStackInSlotInternal -> setSlotStack (see MixinInventoryHandler)
                current.getInventoryHandler().setSlotStack(slot, replacement);
            else {
                var upgrades = current.getUpgradeHandler();
                if (((InventoryAlias) upgrades).inventoryIdentity()
                        != SophisticatedContentsStorage.upgradesIdentity(contents)) return false;
                upgrades.setStackInSlot(slot, replacement);
            }
            return true;
        }

        @Override
        public void publishStoredChanges(SlotSource source, int slot) {
            persist(wrapper);
        }
    }

    // backport: the slot count a record's handler saved ("Size", ItemStackHandler format), or -1
    private static int savedSlots(CompoundTag contents, String key) {
        CompoundTag inventory = contents.getCompound(key);
        return inventory.contains("Size", Tag.TAG_INT) ? inventory.getInt("Size") : -1;
    }

    private static final class Backpacks {
        private static final ItemInteriorAdapter ADAPTER =
                new Adapter(BackpackItem.class, true) {
                    @Override
                    public ItemInteriorBackend backend(
                            MinecraftServer server, UUID id, ItemStackTemplate prototype) {
                        return new BackpackBackend(server, id, prototype);
                    }
                };

        private static final class BackpackBackend extends Backend {
            BackpackBackend(MinecraftServer server, UUID id, ItemStackTemplate prototype) {
                super(server, id, prototype);
            }

            @Override
            protected @Nullable CompoundTag readContents() {
                return ((SophisticatedContentsStorage) BackpackStorage.get()).storedContents(id);
            }

            @Override
            protected IStorageWrapper createWrapper() {
                ItemStack item = prototype.create();
                item.remove(
                        net.p3pp3rf1y.sophisticatedbackpacks.init.ModDataComponents.COLUMNS_TAKEN
                                .get());
                var current = BackpackWrapper.fromStack(item);
                // backport: 26.x sized the wrapper from the record's InventoryData / UpgradeData;
                // a 1.21.1 record keeps each handler's saved slot count instead
                int inventorySlots = savedSlots(contents(), InventoryHandler.INVENTORY_TAG);
                int upgradeSlots =
                        savedSlots(
                                contents(),
                                net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler
                                        .UPGRADE_INVENTORY_TAG);
                if (inventorySlots >= 0 && upgradeSlots >= 0)
                    current.setSlotNumbers(inventorySlots, upgradeSlots);
                return current;
            }

            @Override
            protected void persist(IStorageWrapper current) {
                // backport: 1.21.1 Sophisticated has no StorageWrapperRepository
                // .invalidateStorageWrapperContents (its wrappers are cached per stack, not per
                // record); the handler already wrote the slot into the record, so marking the
                // storage dirty is what is left of persisting it
                BackpackStorage.get().setDirty();
            }
        }
    }

    private static final class Storage {
        private static final ItemInteriorAdapter ADAPTER =
                new Adapter(StorageBlockItem.class, false) {
                    @Override
                    public ItemInteriorBackend backend(
                            MinecraftServer server, UUID id, ItemStackTemplate prototype) {
                        return new StorageBackend(server, id, prototype);
                    }
                };

        private static final class StorageBackend extends Backend {
            StorageBackend(MinecraftServer server, UUID id, ItemStackTemplate prototype) {
                super(server, id, prototype);
            }

            @Override
            protected @Nullable CompoundTag readContents() {
                return ((SophisticatedContentsStorage) ItemContentsStorage.get())
                        .storedContents(id);
            }

            @Override
            protected IStorageWrapper createWrapper() {
                // backport: 26.x subclassed StackStorageWrapper to pin the saved ContainerContents,
                // its uuid, slot counts and settings. A 1.21.1 StackStorageWrapper reads all of those
                // from the record its stack's storage uuid names (ItemContentsStorage), which is this
                // record, and its constructor already reads the contents, before a subclass field
                // could be set.
                return new StackStorageWrapper(prototype.create());
            }

            @Override
            protected void persist(IStorageWrapper current) {
                // backport: see BackpackBackend.persist
                ItemContentsStorage.get().setDirty();
            }
        }
    }
}

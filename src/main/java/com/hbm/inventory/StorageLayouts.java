// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class StorageLayouts {
    private static final String BE = "net.minecraft.world.level.block.entity.";
    public static final String BASE = BE + "BaseContainerBlockEntity";
    public static final String CHEST = BE + "ChestBlockEntity";
    public static final String BARREL = BE + "BarrelBlockEntity";
    public static final String HOPPER = BE + "HopperBlockEntity";
    public static final String DISPENSER = BE + "DispenserBlockEntity";
    public static final String SHULKER = BE + "ShulkerBoxBlockEntity";
    public static final String BREWING = BE + "BrewingStandBlockEntity";
    public static final String CRAFTER = BE + "CrafterBlockEntity";
    public static final String FURNACE = BE + "AbstractFurnaceBlockEntity";
    public static final String CAMPFIRE = BE + "CampfireBlockEntity";
    public static final String BOOKSHELF = BE + "ChiseledBookShelfBlockEntity";
    public static final String LECTERN = BE + "LecternBlockEntity";
    public static final String LECTERN_MENU = LECTERN + "$1";
    public static final String BRUSHABLE = BE + "BrushableBlockEntity";
    public static final String JUKEBOX = BE + "JukeboxBlockEntity";
    public static final String POT = BE + "DecoratedPotBlockEntity";
    public static final String VAULT = BE + "vault.VaultBlockEntity";
    public static final String CRAFTING =
            "net.minecraft.world.inventory.TransientCraftingContainer";
    public static final String TOM_MATRIX = "com.tom.storagemod.util.CraftingMatrix";
    public static final String COMPACT =
            "com.witchica.compactstorage.block.entity.base.BaseCompactStorageBlockEntity";
    public static final String DRUM =
            "com.witchica.compactstorage.block.entity.base.BaseItemDrumBlockEntity";
    public static final String STACK_HANDLER =
            "com.hbm.lib.neotransfer.StacksResourceHandler";
    public static final String ITEM_HANDLER =
            "com.hbm.lib.neotransfer.item.ItemStacksResourceHandler";
    public static final String UPGRADES = "net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler";
    // backport: Sophisticated Core 1.21.1's InventoryHandler is a NeoForge ItemStackHandler, not a
    // (woven) ItemStacksResourceHandler, so it gets its own list-replacement hook
    public static final String SOPH_INVENTORY =
            "net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler";
    public static final String SOPH_STORAGE =
            "net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity";
    public static final String SOPH_BACKPACK =
            "net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity";
    // backport: no ShelfBlockEntity in 1.21.1; Tech Reborn (Fabric), Storage Drawers and AE2 are not
    // DE integrations (no slot adapters for them here, so admitting their block entities would throw
    // in BlockStorage); Sophisticated Core 1.21.1 has no ContainerContents$InventoryData/UpgradeData
    // (see MixinInventoryHandler)

    public enum Kind {
        METHODS,
        LIST,
        STACK,
        DECLARED,
        ARRAY,
        CHILDREN,
        KEYED,
        INTERIOR
    }

    public enum Owner {
        SELF,
        EXTERNAL,
        MENU
    }

    public enum Publish {
        NONE,
        CONTAINER,
        BOOKSHELF,
        LECTERN,
        JUKEBOX
    }

    public enum Event {
        LIST,
        ROOT,
        OWNER
    }

    public record Source(
            Kind kind,
            String field,
            String fieldOwner,
            Owner owner,
            Publish publish,
            boolean containerCommit) {}

    public record Write(String owner, String field, boolean nullableOld, boolean array) {
        public Write(String owner, String field) {
            this(owner, field, false, false);
        }

        public Write(String owner, String field, boolean nullableOld) {
            this(owner, field, nullableOld, false);
        }

        public static Write array(String owner, String field) {
            return new Write(owner, field, false, true);
        }
    }

    public record Hook(String method, Event event, String ownerGetter) {
        public Hook(String method, Event event) {
            this(method, event, null);
        }
    }

    public record Root(String getter, String descriptor, boolean refresh) {}

    public record Layout(Source source, List<Write> writes, List<Hook> hooks, Root root) {}

    public enum Admission {
        NONE,
        EXACT,
        FAMILY
    }

    public enum Loader {
        COMMON,
        NEOFORGE
    }

    public enum Host {
        NONE,
        EMPTY,
        CABLE,
        INTERFACE,
        PATTERN,
        WORKBENCH,
        CPU,
        CHILDREN,
        STANDARD,
        STANDARD_UPGRADES,
        TERMINAL,
        CRAFTING,
        ENCODING,
        UPGRADES
    }

    public enum Format {
        INV_MAP,
        ITEM_LIST,
        KEY_LIST,
        KEY_MAP
    }

    public record Field(String name, Format format, int capacity, int firstSlot) {}

    public record Packed(List<String> typeIds, List<Field> fields, boolean coreOnly) {}

    public record Metadata(
            Admission admission,
            Loader loader,
            Host host,
            @Nullable Packed packed,
            @Nullable Layout weaving) {}

    public static final Map<String, Metadata> DECLARATIONS = declarations();
    public static final Map<String, Layout> TARGETS = weaving();
    public static final Map<String, Metadata> BLOCK_TYPES = blockTypes();

    private StorageLayouts() {}

    private static Map<String, Metadata> declarations() {
        var targets = new LinkedHashMap<String, Metadata>();
        declare(
                targets,
                BASE,
                weave(
                        source(
                                new Source(
                                        Kind.METHODS,
                                        null,
                                        null,
                                        Owner.SELF,
                                        Publish.NONE,
                                        false))));
        for (String target :
                List.of(CHEST, BARREL, HOPPER, DISPENSER, SHULKER, BREWING, CRAFTER, FURNACE))
            declare(
                    targets,
                    target,
                    target.equals(FURNACE)
                            ? weave(hooks("setItems"))
                            : nativeOwner(hooks("setItems")));
        for (String name :
                List.of(
                        "TrappedChestBlockEntity",
                        "DropperBlockEntity",
                        "FurnaceBlockEntity",
                        "BlastFurnaceBlockEntity",
                        "SmokerBlockEntity")) declare(targets, BE + name, nativeOwner(null));
        for (String target : List.of(CAMPFIRE))
            declare(targets, target, nativeOwner(source(list("items", Owner.SELF, Publish.NONE))));
        declare(
                targets,
                BOOKSHELF,
                nativeOwner(source(list("items", Owner.SELF, Publish.BOOKSHELF))));
        declare(targets, CRAFTING, weave(source(list("items", Owner.MENU, Publish.CONTAINER))));
        declare(
                targets,
                TOM_MATRIX,
                weave(
                        source(
                                new Source(
                                        Kind.LIST,
                                        "items",
                                        null,
                                        Owner.EXTERNAL,
                                        Publish.CONTAINER,
                                        true))));
        for (String target : List.of(BRUSHABLE, POT, JUKEBOX, LECTERN)) {
            String field = target.equals(LECTERN) ? "book" : "item";
            Publish publish =
                    target.equals(LECTERN)
                            ? Publish.LECTERN
                            : target.equals(JUKEBOX) ? Publish.JUKEBOX : Publish.NONE;
            declare(
                    targets,
                    target,
                    nativeOwner(
                            new Layout(
                                    new Source(Kind.STACK, field, null, Owner.SELF, publish, false),
                                    List.of(new Write(target, field)),
                                    List.of(),
                                    null)));
        }
        declare(targets, LECTERN_MENU, weave(writes(LECTERN, "book")));
        declare(targets, COMPACT, family(hooks("loadAdditional", "resizeInventory", "setItems")));
        declare(targets, STACK_HANDLER, weave(hooks("setStacks", "deserialize")));
        // backport: 1.21.1 UpgradeHandler / InventoryHandler replace their stacks list in setSize
        // (deserializeNBT goes through it); 26.x hooked loadUpgradesFromData / deserialize
        declare(targets, UPGRADES, weave(hooks("setSize")));
        declare(targets, SOPH_INVENTORY, weave(hooks("setSize")));
        declare(
                targets,
                SOPH_STORAGE,
                family(
                        new Layout(
                                null,
                                List.of(),
                                List.of(new Hook("loadStorageWrapper", Event.ROOT)),
                                null)));
        declare(
                targets,
                SOPH_BACKPACK,
                family(
                        new Layout(
                                null,
                                List.of(),
                                List.of(new Hook("setBackpack", Event.ROOT)),
                                null)));
        declare(
                targets,
                ITEM_HANDLER,
                weave(
                        source(
                                new Source(
                                        Kind.LIST,
                                        "stacks",
                                        STACK_HANDLER,
                                        Owner.EXTERNAL,
                                        Publish.NONE,
                                        false))));
        declare(
                targets,
                VAULT,
                nativeOwner(
                        root(
                                "getServerData",
                                "()Lnet/minecraft/world/level/block/entity/vault/VaultServerData;",
                                false)));
        declare(
                targets,
                DRUM,
                family(
                        root(
                                "getDrumInventory",
                                "()Lcom/witchica/compactstorage/inventory/DrumInventory;",
                                true)));
        for (String name :
                List.of(
                        "net.minecraft.world.level.block.piston.PistonMovingBlockEntity",
                        "com.tom.storagemod.block.entity.FilingCabinetBlockEntity",
                        "com.tom.storagemod.block.entity.CraftingTerminalBlockEntity",
                        "com.hbm.blockentity.BlockEntityMachineBase",
                        "com.hbm.blockentity.machine.BlockEntityFoundryCastingBase"))
            declare(targets, name, family(null));

        return Map.copyOf(targets);
    }

    private static Map<String, Layout> weaving() {
        var targets = new LinkedHashMap<String, Layout>();
        DECLARATIONS.forEach(
                (name, metadata) -> {
                    if (metadata.weaving() != null) targets.put(name, metadata.weaving());
                });
        return Map.copyOf(targets);
    }

    private static Map<String, Metadata> blockTypes() {
        var types = new LinkedHashMap<String, Metadata>();
        for (Metadata metadata : DECLARATIONS.values()) {
            if (metadata.packed() == null) continue;
            for (String id : metadata.packed().typeIds())
                if (types.putIfAbsent(id, metadata) != null)
                    throw new IllegalStateException("Duplicate storage block type: " + id);
        }
        return Map.copyOf(types);
    }

    private static void declare(Map<String, Metadata> targets, String name, Metadata metadata) {
        if (targets.putIfAbsent(name, metadata) != null)
            throw new IllegalStateException("Duplicate storage declaration: " + name);
    }

    private static Metadata weave(Layout layout) {
        return new Metadata(Admission.NONE, Loader.COMMON, Host.NONE, null, layout);
    }

    private static Metadata nativeOwner(@Nullable Layout layout) {
        return new Metadata(Admission.EXACT, Loader.COMMON, Host.NONE, null, layout);
    }

    private static Metadata family(@Nullable Layout layout) {
        return new Metadata(Admission.FAMILY, Loader.COMMON, Host.NONE, null, layout);
    }

    private static Source list(String field, Owner owner, Publish publish) {
        return new Source(Kind.LIST, field, null, owner, publish, false);
    }

    private static Layout source(Source source) {
        return new Layout(source, List.of(), List.of(), null);
    }

    private static Layout hooks(String... methods) {
        return new Layout(
                null,
                List.of(),
                java.util.Arrays.stream(methods)
                        .map(method -> new Hook(method, Event.LIST))
                        .toList(),
                null);
    }

    private static Layout writes(String owner, String field) {
        return new Layout(null, List.of(new Write(owner, field)), List.of(), null);
    }

    private static Layout root(String getter, String descriptor, boolean refresh) {
        return new Layout(null, List.of(), List.of(), new Root(getter, descriptor, refresh));
    }
}

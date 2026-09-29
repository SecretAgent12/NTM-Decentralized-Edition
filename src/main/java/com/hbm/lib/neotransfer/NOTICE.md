# com.hbm.lib.neotransfer — backport of the NeoForge transfer API

## Origin

The Java sources in this directory (and its subpackages) are derived from the
[NeoForge](https://github.com/neoforged/NeoForge) repository, branch `26.2.x`,
package `net.neoforged.neoforge.transfer` (Copyright (c) NeoForged and contributors).

Only the subset of the API used by the mod was taken (43 files including `package-info.java`):
the root package (`ResourceHandler`, `StacksResourceHandler`, `ResourceHandlerUtil`, ...),
`access`, `energy/EnergyHandler`, `fluid/FluidResource`, `fluid/FluidStacksResourceHandler`,
`item` (resources, stack handlers, vanilla container / player inventory wrappers, slot),
`resource` and `transaction`.

## License

These files are licensed under the **GNU Lesser General Public License v2.1 only**
(`SPDX-License-Identifier: LGPL-2.1-only`), the license of the original NeoForge code; see
`LICENSE-LGPL-2.1.txt` in this directory. The modifications made for the backport (by SecretAgent12, 2026) are
distributed under the same license.

The rest of the mod is licensed under LGPL-3.0-only. The files in this package keep
their own license (LGPL-2.1-only) and are not relicensed. Every file keeps its original
copyright header, with an added line marking it as modified.

## Changes

General:

- Package relocated from `net.neoforged.neoforge.transfer(.*)` to `com.hbm.lib.neotransfer(.*)`:
  NeoForge's module owns `net.neoforged.neoforge.*` in 1.21.1, so a split package would not load.
- Adapted to the Minecraft 1.21.1 / NeoForge 21.1 API. Places where the code differs from
  the original are marked with `// backport:` comments.
- Classes, public method names and signatures are unchanged except where 1.21.1 lacks the
  types involved (listed below).

Notable API/semantic changes:

- `ItemResource` / `FluidResource`: the `of(ItemStackTemplate)` / `of(FluidStackTemplate)` and
  `matches(ItemStackTemplate)` / `matches(FluidStackTemplate)` overloads were removed (these
  types do not exist in 1.21.1); the `ItemStack` / `FluidStack` overloads remain.
  The per-item/per-fluid default resource cache (`Item#computeDefaultResource`,
  `Fluid#computeDefaultResource` in 26.x) is a static map instead.
  `ItemResource.CODEC` uses `ItemStack.ITEM_NON_AIR_CODEC`; the component validation uses
  `ItemStack.validateComponents` (1.21.1 has no `validateStrict`).
- `RegisteredResource` no longer extends `net.minecraft.core.TypedInstance` (not in 1.21.1);
  `typeHolder()`, `tags()` and the `is(TagKey/HolderSet/T/Holder/ResourceKey)` methods are
  declared on the interface itself.
- `StacksResourceHandler` / `ItemStackResourceHandler`: implement
  `INBTSerializable<CompoundTag>` instead of `ValueIOSerializable`;
  `serialize(ValueOutput)` / `deserialize(ValueInput)` became
  `serialize(CompoundTag, HolderLookup.Provider)` / `deserialize(CompoundTag, HolderLookup.Provider)`
  with the same keys and codecs (registry-aware `NbtOps`).
- `VanillaContainerWrapper`: 1.21.1 `Container` has neither `setItem(int, ItemStack, boolean insideTransaction)`
  nor `onTransfer(...)`; the plain `setItem` is used (container side effects in `setItem` are not
  suppressed during transactions) and transfer notifications are not sent.
- `PlayerInventoryWrapper`: covers main + armor + offhand (41 slots; 1.21.1 has no body-armor slot
  in the player inventory); armor slot mapping via `EquipmentSlot#getIndex`; selected slot via the
  `Inventory#selected` field; item tossing reproduces `CommonHooks.onPlayerTossEvent` locally so the
  `dropAround` flag is honoured; armor slot capacity is 1.
- `ResourceHandlerSlot` extends `Slot` directly and contains the stack-copy slot logic
  (26.x base class `StackCopySlot(index, x, y)` has no 1.21.1 equivalent with a slot index).
- `StackItemAccess`: overrides `SimpleContainer#setItem(int, ItemStack)` (no 3-argument overload in 1.21.1).
- The code keeps the JSpecify annotations (`org.jspecify.annotations`), which must be on the compile classpath.

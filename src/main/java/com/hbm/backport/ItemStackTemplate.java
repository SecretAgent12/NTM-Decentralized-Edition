// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import com.hbm.items.weapon.sedna.GunTimers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

/**
 * 26.x ItemStackTemplate: an immutable item + count + component patch, the form in
 * which 26.x stores stacks inside recipes and components. 1.21.1 has no such type
 * (it stores ItemStacks), so the backport's own class stands in for it.
 *
 * Serialised exactly like an ItemStack ({id, count, components}).
 *
 * Two things ntm-next added to the vanilla class through MixinItemStackTemplate
 * live here directly: the cached unstable-fuse deadline (IUnstableDeadline) and
 * GunTimers.forSave applied in fromStack.
 */
public final class ItemStackTemplate implements ItemInstance, IUnstableDeadline {

    public static final MapCodec<ItemStackTemplate> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ItemStack.ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(ItemStackTemplate::item),
            ExtraCodecs.intRange(1, 99).optionalFieldOf("count", 1).forGetter(ItemStackTemplate::count),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                    .forGetter(ItemStackTemplate::components))
            .apply(i, ItemStackTemplate::new));

    public static final Codec<ItemStackTemplate> CODEC = Codec.lazyInitialized(MAP_CODEC::codec);

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackTemplate> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(Registries.ITEM), ItemStackTemplate::item,
            ByteBufCodecs.VAR_INT, ItemStackTemplate::count,
            DataComponentPatch.STREAM_CODEC, ItemStackTemplate::components,
            ItemStackTemplate::new);

    private final Holder<Item> item;
    private final int count;
    private final DataComponentPatch components;
    private @Nullable DataComponentMap map;
    private volatile long hbm$unstableDeadline = Long.MIN_VALUE;

    public ItemStackTemplate(Holder<Item> item, int count, DataComponentPatch components) {
        this.item = item;
        this.count = count;
        this.components = components;
    }

    public ItemStackTemplate(Item item, int count, DataComponentPatch components) {
        this(item.builtInRegistryHolder(), count, components);
    }

    public ItemStackTemplate(ItemLike item, int count) {
        this(item.asItem().builtInRegistryHolder(), count, DataComponentPatch.EMPTY);
    }

    public ItemStackTemplate(ItemLike item) {
        this(item, 1);
    }

    public static ItemStackTemplate fromNonEmptyStack(ItemStack stack) {
        if (stack.isEmpty()) throw new IllegalArgumentException("Can't take a template of an empty stack");
        return new ItemStackTemplate(stack.getItemHolder(), stack.getCount(), stack.getComponentsPatch());
    }

    /** Backport: the empty-stack behaviour of 26.x's fromStack is not verified; here an empty stack gives null. */
    public static @Nullable ItemStackTemplate fromStack(ItemStack stack) {
        ItemStack stored = GunTimers.forSave(stack);
        return stored.isEmpty() ? null : fromNonEmptyStack(stored);
    }

    /** Backport helper: a template of the stack, or null for an empty one (no save transforms). */
    public static @Nullable ItemStackTemplate of(ItemStack stack) {
        return stack.isEmpty() ? null : fromNonEmptyStack(stack);
    }

    /** Backport helper: templates of the non-empty stacks, for code iterating 26.x template lists. */
    public static java.util.List<ItemStackTemplate> ofAll(Iterable<ItemStack> stacks) {
        java.util.List<ItemStackTemplate> out = new java.util.ArrayList<>();
        for (ItemStack s : stacks) if (!s.isEmpty()) out.add(fromNonEmptyStack(s));
        return out;
    }

    /** Backport helper for 26.x ItemStack.getCraftingRemainder(): null when there is none. */
    public static @Nullable ItemStackTemplate remainderOf(ItemStack stack) {
        ItemStack r = stack.getCraftingRemainingItem();
        return r.isEmpty() ? null : fromNonEmptyStack(r);
    }

    public Holder<Item> item() {
        return item;
    }

    @Override
    public int count() {
        return count;
    }

    public DataComponentPatch components() {
        return components;
    }

    @Override
    public Holder<Item> typeHolder() {
        return item;
    }

    public Item getItem() {
        return item.value();
    }

    @Override
    public DataComponentMap getComponents() {
        DataComponentMap m = map;
        if (m == null) map = m = PatchedDataComponentMap.fromPatch(item.value().components(), components);
        return m;
    }

    public ItemStack create() {
        return new ItemStack(item, count, components);
    }

    /** A stack of this item with the given count, the template's components, then `patch` on top. */
    public ItemStack apply(int count, DataComponentPatch patch) {
        ItemStack stack = new ItemStack(item, count, components);
        stack.applyComponents(patch);
        return stack;
    }

    public ItemStackTemplate withCount(int count) {
        return new ItemStackTemplate(item, count, components);
    }

    @Override
    public long hbm$unstableDeadline() {
        return hbm$unstableDeadline;
    }

    @Override
    public void hbm$setUnstableDeadline(long value) {
        hbm$unstableDeadline = value;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ItemStackTemplate t
                && count == t.count && item.equals(t.item) && components.equals(t.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, count, components);
    }

    @Override
    public String toString() {
        return count + " " + item.getRegisteredName() + (components.isEmpty() ? "" : " " + components);
    }
}

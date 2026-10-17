// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import com.hbm.backport.ItemStackTemplate;
import net.minecraft.world.item.component.CustomData;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.backport.Nbt;

public final class VanillaBlockItemLayouts {
    private VanillaBlockItemLayouts() {}

    public static void register() {
        BlockItemContents.register(ResourceLocation.withDefaultNamespace("lectern"), new Scalar("Book"));
        BlockItemContents.register(
                ResourceLocation.withDefaultNamespace("jukebox"), new Scalar("RecordItem"));
        BlockItemContents.register(
                ResourceLocation.withDefaultNamespace("brushable_block"), new Scalar("item"));
        BlockItemContents.register(ResourceLocation.withDefaultNamespace("vault"), new EjectionQueue());
    }

    private record Scalar(String field) implements BlockItemContents.Layout {
        @Override
        public CustomData capture(CompoundTag nativeTag) {
            CompoundTag payload = new CompoundTag();
            Tag item = nativeTag.get(field);
            if (item != null) payload.put(field, item.copy());
            return CustomData.of(payload);
        }

        @Override
        public List<BlockItemContents.Entry> index(CustomData physical, RegistryOps<Tag> ops) {
            CompoundTag item = Nbt.getCompoundOrEmpty(physical.copyTag(), field);
            return item.isEmpty() ? List.of() : List.of(entry(item, field, ops));
        }

        @Override
        public void patch(
                CompoundTag physical,
                Object path,
                ItemStack replacement,
                long amount,
                RegistryOps<Tag> ops) {
            if (replacement.isEmpty()) physical.remove(field);
            else physical.put(field, encoded(replacement, amount, ops));
        }

        @Override
        public void strip(ValueOutput nativeTag) {
            nativeTag.discard(field);
        }

        @Override
        public void merge(CompoundTag nativeTag, CustomData physical) {
            Tag item = physical.copyTag().get(field);
            if (item == null) nativeTag.remove(field);
            else nativeTag.put(field, item.copy());
        }
    }

    private static final class EjectionQueue implements BlockItemContents.Layout {
        @Override
        public CustomData capture(CompoundTag nativeTag) {
            CompoundTag payload = new CompoundTag();
            Tag items = Nbt.getCompoundOrEmpty(nativeTag, "server_data").get("items_to_eject");
            if (items != null) payload.put("items_to_eject", items.copy());
            return CustomData.of(payload);
        }

        @Override
        public List<BlockItemContents.Entry> index(CustomData physical, RegistryOps<Tag> ops) {
            ListTag items = Nbt.getListOrEmpty(physical.copyTag(), "items_to_eject");
            var result = new ArrayList<BlockItemContents.Entry>(items.size());
            for (int index = 0; index < items.size(); index++)
                result.add(entry((CompoundTag) items.get(index), index, ops));
            return result;
        }

        @Override
        public void patch(
                CompoundTag physical,
                Object path,
                ItemStack replacement,
                long amount,
                RegistryOps<Tag> ops) {
            ListTag items = Nbt.getList(physical, "items_to_eject").orElseThrow();
            int index = (Integer) path;
            if (replacement.isEmpty()) items.remove(index);
            else items.set(index, encoded(replacement, amount, ops));
        }

        @Override
        public void strip(ValueOutput nativeTag) {
            nativeTag.child("server_data").discard("items_to_eject");
        }

        @Override
        public void merge(CompoundTag nativeTag, CustomData physical) {
            CompoundTag state = Nbt.getCompoundOrEmpty(nativeTag, "server_data");
            state.put("items_to_eject", Nbt.getListOrEmpty(physical.copyTag(), "items_to_eject").copy());
            nativeTag.put("server_data", state);
        }
    }

    private static BlockItemContents.Entry entry(
            CompoundTag item, Object path, RegistryOps<Tag> ops) {
        ItemStackTemplate template =
                ItemStackTemplate.MAP_CODEC.codec().parse(ops, item).getOrThrow();
        return new BlockItemContents.Entry(template.withCount(1), template.count(), path);
    }

    private static Tag encoded(ItemStack item, long amount, RegistryOps<Tag> ops) {
        assert item.getCount() == 1;
        ItemStack full = item.copyWithCount(Math.toIntExact(amount));
        return ItemStack.CODEC.encodeStart(ops, full).getOrThrow(); // backport: 1.21.1 has no ItemStack.MAP_CODEC
    }
}

// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import com.hbm.blockentity.machine.storage.LockedContents;
import com.hbm.items.ModDataComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import com.hbm.backport.ItemInstance;
import net.minecraft.world.item.ItemStack;
import com.hbm.backport.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

public final class ContentFields {
    private ContentFields() {}

    public static class Slots extends ItemContents.Field {
        private volatile DataComponentType<ItemContainerContents> type;
        private final ResourceLocation deferred;

        public Slots(DataComponentType<ItemContainerContents> type, boolean radiation) {
            super(radiation);
            this.type = type;
            deferred = null;
        }

        public Slots(ResourceLocation deferred, boolean radiation) {
            super(radiation);
            this.deferred = deferred;
        }

        @SuppressWarnings("unchecked")
        private DataComponentType<ItemContainerContents> type() {
            if (type == null) {
                if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(deferred))
                    throw new IllegalStateException("Missing contents component: " + deferred);
                type =
                        (DataComponentType<ItemContainerContents>)
                                (DataComponentType<?>)
                                        BuiltInRegistries.DATA_COMPONENT_TYPE.get(deferred);
            }
            return type;
        }

        @Override
        public boolean present(ItemInstance parent) {
            var contents = parent.get(type());
            return contents != null && !contents.items.isEmpty();
        }

        // backport: 1.21.1 ItemContainerContents holds ItemStacks (EMPTY for an empty slot), not
        // Optional<ItemStackTemplate>, so children are passed as the ItemInstance they are
        protected long quantity(ItemInstance parent, int slot, ItemInstance child) {
            return child.count();
        }

        protected void removed(ItemStack parent, int slot) {}

        @Override
        public void visit(
                ItemInstance parent, @Nullable ServerLevel level, ItemContents.Reader reader) {
            var contents = parent.get(type());
            for (int slot = 0; slot < contents.items.size(); slot++) {
                var child = contents.items.get(slot);
                if (!child.isEmpty()) {
                    long quantity = quantity(parent, slot, child);
                    if (quantity > 0) reader.accept(child, quantity);
                }
            }
        }

        @Override
        public boolean rewrite(ItemStack parent, ServerLevel level, ItemContents.Editor editor) {
            var contents = parent.get(type());
            List<ItemStack> replacement = null;
            for (int slot = 0; slot < contents.items.size(); slot++) {
                var entry = contents.items.get(slot);
                if (entry.isEmpty()
                        || quantity(parent, slot, entry) <= 0
                        || !editor.selects(entry)) continue;
                ItemStack child = entry.copy();
                if (!editor.visit(child)) continue;
                if (replacement == null) replacement = new ArrayList<>(contents.items);
                replacement.set(slot, child.isEmpty() ? ItemStack.EMPTY : child);
                if (child.isEmpty()) removed(parent, slot);
            }
            if (replacement == null) return false;
            parent.set(type(), slots(replacement));
            return true;
        }
    }

    // backport: 1.21.1's fromItems does the same trailing-empty trim (EMPTY when nothing is left)
    // and copies the stacks, as 26.x's templates are immutable
    private static ItemContainerContents slots(List<ItemStack> items) {
        return ItemContainerContents.fromItems(items);
    }

    public static final class Scalar extends ItemContents.Field {
        private final DataComponentType<ItemStackTemplate> type;

        public Scalar(DataComponentType<ItemStackTemplate> type, boolean radiation) {
            super(radiation);
            this.type = type;
        }

        @Override
        public boolean present(ItemInstance parent) {
            return parent.get(type) != null;
        }

        @Override
        public void visit(
                ItemInstance parent, @Nullable ServerLevel level, ItemContents.Reader reader) {
            ItemStackTemplate child = parent.get(type);
            reader.accept(child, child.count());
        }

        @Override
        public boolean rewrite(ItemStack parent, ServerLevel level, ItemContents.Editor editor) {
            ItemStackTemplate original = parent.get(type);
            if (!editor.selects(original)) return false;
            ItemStack child = original.create();
            if (!editor.visit(child)) return false;
            if (child.isEmpty()) parent.remove(type);
            else parent.set(type, ItemStackTemplate.fromNonEmptyStack(child));
            return true;
        }
    }

    public static class Sequence extends ItemContents.Field {
        private final DataComponentType<List<ItemStackTemplate>> type;

        public Sequence(DataComponentType<List<ItemStackTemplate>> type, boolean radiation) {
            super(radiation);
            this.type = type;
        }

        @Override
        public boolean present(ItemInstance parent) {
            var contents = parent.get(type);
            return contents != null && !contents.isEmpty();
        }

        @Override
        public void visit(
                ItemInstance parent, @Nullable ServerLevel level, ItemContents.Reader reader) {
            for (ItemStackTemplate child : parent.get(type)) reader.accept(child, child.count());
        }

        @Override
        public boolean rewrite(ItemStack parent, ServerLevel level, ItemContents.Editor editor) {
            var changed = sequence(parent.get(type), -1, editor);
            if (changed == null) return false;
            parent.set(type, List.copyOf(changed.items));
            return true;
        }
    }

    private record Changes(List<ItemStackTemplate> items, int selected) {}

    private static @Nullable Changes sequence(
            List<ItemStackTemplate> items, int selected, ItemContents.Editor editor) {
        List<ItemStackTemplate> replacement = null;
        int nextSelection = selected;
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStackTemplate original = items.get(slot);
            if (!editor.selects(original)) continue;
            ItemStack child = original.create();
            if (!editor.visit(child)) continue;
            if (replacement == null) replacement = new ArrayList<>(items);
            replacement.set(
                    slot, child.isEmpty() ? null : ItemStackTemplate.fromNonEmptyStack(child));
            if (child.isEmpty()) {
                if (slot < selected) nextSelection--;
                else if (slot == selected) nextSelection = -1;
            }
        }
        if (replacement == null) return null;
        replacement.removeIf(item -> item == null);
        return new Changes(replacement, nextSelection);
    }

    public static ItemContents.Field bundle() {
        return new ItemContents.Field(true) {
            @Override
            public boolean present(ItemInstance parent) {
                var contents = parent.get(DataComponents.BUNDLE_CONTENTS);
                return contents != null && !contents.isEmpty();
            }

            @Override
            public void visit(
                    ItemInstance parent, @Nullable ServerLevel level, ItemContents.Reader reader) {
                for (var child : parent.get(DataComponents.BUNDLE_CONTENTS).items())
                    reader.accept(child, child.count());
            }

            @Override
            public boolean rewrite(
                    ItemStack parent, ServerLevel level, ItemContents.Editor editor) {
                BundleContents contents = parent.get(DataComponents.BUNDLE_CONTENTS);
                // backport: 1.21.1 bundles hold ItemStacks and have no selected item
                var changed = sequence(ItemStackTemplate.ofAll(contents.items()), -1, editor);
                if (changed == null) return false;
                parent.set(
                        DataComponents.BUNDLE_CONTENTS,
                        new BundleContents(
                                changed.items.stream().map(ItemStackTemplate::create).toList()));
                return true;
            }
        };
    }

    public static ItemContents.Field projectiles() {
        return new ItemContents.Field(true) {
            @Override
            public boolean present(ItemInstance parent) {
                var contents = parent.get(DataComponents.CHARGED_PROJECTILES);
                return contents != null && !contents.isEmpty();
            }

            @Override
            public void visit(
                    ItemInstance parent, @Nullable ServerLevel level, ItemContents.Reader reader) {
                for (var child : parent.get(DataComponents.CHARGED_PROJECTILES).getItems())
                    reader.accept(child, child.count());
            }

            @Override
            public boolean rewrite(
                    ItemStack parent, ServerLevel level, ItemContents.Editor editor) {
                // backport: 1.21.1 ChargedProjectiles holds ItemStacks (getItems() copies them)
                var changed =
                        sequence(
                                ItemStackTemplate.ofAll(
                                        parent.get(DataComponents.CHARGED_PROJECTILES).getItems()),
                                -1,
                                editor);
                if (changed == null) return false;
                parent.set(
                        DataComponents.CHARGED_PROJECTILES,
                        ChargedProjectiles.of(
                                changed.items.stream().map(ItemStackTemplate::create).toList()));
                return true;
            }
        };
    }

    public static ItemContents.Field packed() {
        return new ItemContents.Field(true) {
            @Override
            public boolean present(ItemInstance parent) {
                return parent.get(ModDataComponents.BLOCK_INVENTORY.get()) != null;
            }

            @Override
            public void visit(
                    ItemInstance parent, @Nullable ServerLevel level, ItemContents.Reader reader) {
                for (var entry : parent.get(ModDataComponents.BLOCK_INVENTORY.get()).entries())
                    reader.accept(entry.item(), entry.amount());
            }

            @Override
            protected void summarize(ItemInstance parent, ItemContents.Summary summary) {
                var contents = parent.get(ModDataComponents.BLOCK_INVENTORY.get());
                summary.merge(
                        contents.minimumDeadline(),
                        contents.references(),
                        contents.opaqueReferences());
            }

            @Override
            public boolean rewrite(
                    ItemStack parent, ServerLevel level, ItemContents.Editor editor) {
                return BlockItemContents.rewrite(parent, editor, level.registryAccess());
            }
        };
    }

    public static ItemContents.Field sealed() {
        return new ItemContents.Field(false) {
            @Override
            public boolean present(ItemInstance parent) {
                return parent.get(ModDataComponents.LOCKED_CONTENTS.get()) != null;
            }

            @Override
            protected void summarize(ItemInstance parent, ItemContents.Summary summary) {
                LockedContents contents = parent.get(ModDataComponents.LOCKED_CONTENTS.get());
                summary.merge(
                        contents.minimumDeadline(), ItemInteriors.EMPTY, contents.hasReferences());
            }

            @Override
            public void visit(
                    ItemInstance parent, @Nullable ServerLevel level, ItemContents.Reader reader) {
                var contents = parent.get(ModDataComponents.LOCKED_CONTENTS.get()).open(level);
                if (contents != null)
                    for (var child : contents.nonEmptyItems()) reader.accept(child, child.count());
            }

            @Override
            public boolean rewrite(
                    ItemStack parent, ServerLevel level, ItemContents.Editor editor) {
                var original = parent.get(ModDataComponents.LOCKED_CONTENTS.get());
                var contents = original.open(level);
                if (contents == null) return false;
                List<ItemStack> replacement = null;
                for (int slot = 0; slot < contents.items.size(); slot++) {
                    var entry = contents.items.get(slot);
                    if (entry.isEmpty() || !editor.selects(entry)) continue;
                    ItemStack child = entry.copy();
                    if (!editor.visit(child)) continue;
                    if (replacement == null) replacement = new ArrayList<>(contents.items);
                    replacement.set(slot, child.isEmpty() ? ItemStack.EMPTY : child);
                }
                if (replacement == null) return false;
                ItemContainerContents changed = slots(replacement);
                if (changed == ItemContainerContents.EMPTY)
                    parent.remove(ModDataComponents.LOCKED_CONTENTS.get());
                else
                    parent.set(
                            ModDataComponents.LOCKED_CONTENTS.get(),
                            LockedContents.seal(level, changed));
                return true;
            }
        };
    }
}

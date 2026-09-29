// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Backport of 26.x's InteractionResult.
 *
 * 1.21.2 turned InteractionResult from an enum into a small sealed hierarchy and
 * dropped InteractionResultHolder and ItemInteractionResult in its favour. The
 * tree returns this shape from every use/useOn/useItemOn/useWithoutItem; the
 * compat classes convert at the boundary to whichever of the three 1.21.1 types
 * vanilla expects there (see InteractionResults).
 */
public sealed interface InteractionResult
        permits InteractionResult.Success, InteractionResult.Fail, InteractionResult.Pass,
                InteractionResult.TryEmptyHandInteraction {

    Success SUCCESS = new Success(SwingSource.CLIENT, ItemContext.DEFAULT);
    Success SUCCESS_SERVER = new Success(SwingSource.SERVER, ItemContext.DEFAULT);
    Success CONSUME = new Success(SwingSource.NONE, ItemContext.DEFAULT);
    Fail FAIL = new Fail();
    Pass PASS = new Pass();
    TryEmptyHandInteraction TRY_WITH_EMPTY_HAND = new TryEmptyHandInteraction();

    default boolean consumesAction() {
        return false;
    }

    enum SwingSource {
        NONE,
        CLIENT,
        SERVER
    }

    record ItemContext(boolean wasItemInteraction, @Nullable ItemStack heldItemTransformedTo) {
        public static final ItemContext NONE = new ItemContext(false, null);
        public static final ItemContext DEFAULT = new ItemContext(true, null);
    }

    record Success(SwingSource swingSource, ItemContext itemContext) implements InteractionResult {
        @Override
        public boolean consumesAction() {
            return true;
        }

        public Success heldItemTransformedTo(ItemStack stack) {
            return new Success(swingSource, new ItemContext(true, stack));
        }

        public Success withoutItem() {
            return new Success(swingSource, ItemContext.NONE);
        }

        public boolean wasItemInteraction() {
            return itemContext.wasItemInteraction();
        }

        public @Nullable ItemStack heldItemTransformedTo() {
            return itemContext.heldItemTransformedTo();
        }
    }

    record Fail() implements InteractionResult {}

    record Pass() implements InteractionResult {}

    record TryEmptyHandInteraction() implements InteractionResult {}
}

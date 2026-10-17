// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import com.hbm.backport.IItemInteriorCache;
import com.hbm.backport.ItemInstance;
import net.minecraft.world.item.ItemStack;
import com.hbm.backport.ItemStackTemplate;

/**
 * Backport: the field accesses were inline bytecode (tenon-asm) in ntm-next on
 * fields its mixins add to PatchedDataComponentMap and ItemStackTemplate;
 * rewritten as the plain Java they encode through the IItemInteriorCache duck
 * interface (implemented by MixinPatchedDataComponentMap and by the backport's
 * own ItemStackTemplate).
 */
public final class ItemInteriorData {
    private ItemInteriorData() {}

    public static Object[] cached(ItemInstance stack) {
        if (stack instanceof ItemStack live) {
            var components = live.components;
            return ((IItemInteriorCache) (Object) components).hbm$interiorReferences();
        }
        if (stack instanceof ItemStackTemplate template) {
            return template.hbm$interiorReferences();
        }
        return null;
    }

    public static void cache(ItemInstance stack, Object[] references) {
        if (stack instanceof ItemStack live) {
            var components = live.components;
            ((IItemInteriorCache) (Object) components).hbm$setInteriorReferences(references);
        } else if (stack instanceof ItemStackTemplate template) {
            template.hbm$setInteriorReferences(references);
        }
    }

    public static boolean opaque(ItemInstance stack) {
        if (stack instanceof ItemStack live) {
            var components = live.components;
            return ((IItemInteriorCache) (Object) components).hbm$opaqueReferences();
        }
        if (stack instanceof ItemStackTemplate template) {
            return template.hbm$opaqueReferences();
        }
        return false;
    }

    public static void opaque(ItemInstance stack, boolean opaque) {
        if (stack instanceof ItemStack live) {
            var components = live.components;
            ((IItemInteriorCache) (Object) components).hbm$setOpaqueReferences(opaque);
        } else if (stack instanceof ItemStackTemplate template) {
            template.hbm$setOpaqueReferences(opaque);
        }
    }
}

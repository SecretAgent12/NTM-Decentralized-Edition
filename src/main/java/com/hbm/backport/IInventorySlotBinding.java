// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

/**
 * Backport: accessor for the `hbm$inventorySource` / `hbm$inventorySlot` fields
 * MixinPatchedDataComponentMap adds (which inventory slot a live stack sits in).
 * Replaces ntm-next's raw getfield/putfield bytecode (tenon-asm) in
 * InventorySlotBinding; see ISectionGeneration.
 */
public interface IInventorySlotBinding {
    Object hbm$inventorySource();

    void hbm$setInventorySource(Object source);

    int hbm$inventorySlot();

    void hbm$setInventorySlot(int slot);
}

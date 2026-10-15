// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

/**
 * Backport: accessor for the `hbm$inventorySources` field MixinBlockEntity adds
 * (the tracked inventory sources bound to a block entity). Replaces ntm-next's
 * raw getfield/putfield bytecode (tenon-asm) in InventoryChanges; see
 * ISectionGeneration.
 */
public interface IInventorySources {
    Object hbm$inventorySources();

    void hbm$setInventorySources(Object sources);
}

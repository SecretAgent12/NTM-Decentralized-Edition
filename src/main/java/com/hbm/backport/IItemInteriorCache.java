// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

/**
 * Backport: accessor for the `hbm$interiorReferences` / `hbm$opaqueReferences`
 * caches that MixinPatchedDataComponentMap adds to component maps and that
 * ItemStackTemplate (the backport's own class) carries directly. Replaces
 * ntm-next's raw getfield/putfield bytecode (tenon-asm) in ItemInteriorData;
 * see ISectionGeneration.
 */
public interface IItemInteriorCache {
    Object[] hbm$interiorReferences();

    void hbm$setInteriorReferences(Object[] references);

    boolean hbm$opaqueReferences();

    void hbm$setOpaqueReferences(boolean opaque);
}

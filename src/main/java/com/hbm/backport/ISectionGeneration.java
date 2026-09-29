// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

/**
 * Backport: accessor for the `hbm$generation` counter MixinLevelChunkSection adds.
 *
 * ntm-next read and wrote that mixin-added field with raw getfield/putfield
 * bytecode (tenon-asm), because javac cannot see a field that only exists once
 * the mixin is applied. The usual Mixin answer is a duck interface the mixin
 * implements, which is what this is.
 */
public interface ISectionGeneration {
    long hbm$generation();

    void hbm$setGeneration(long generation);
}

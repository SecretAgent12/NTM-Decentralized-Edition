// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

/**
 * Backport: accessor for the `hbm$unstableDeadline` cache the component-map
 * mixins add. Replaces ntm-next's raw getfield/putfield bytecode (tenon-asm);
 * see ISectionGeneration.
 */
public interface IUnstableDeadline {
    long hbm$unstableDeadline();

    void hbm$setUnstableDeadline(long deadline);
}

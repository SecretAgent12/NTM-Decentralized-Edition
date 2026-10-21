// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

/**
 * backport-fix: BF-104 a machine whose input slots take automated input (inserters, hoppers, pipes) one item per
 * slot per pass, so a whole stack is spread over the slots instead of landing in the first free one. In 1.7.10 the
 * shredder spreads only items that arrive one at a time; a stack from an extractor with a stack upgrade went into
 * one slot. This is a quality-of-life change: the slot rules of the machine (canPlaceItemThroughFace) still decide
 * which slot may take the next item.
 *
 * <p>Through MachineItemHandler: an insert into one slot is capped at one item, an insert without a slot goes
 * round-robin until the stack is in or no slot takes more. Foreign handlers that insert slot by slot (pipes of
 * other mods) move up to one item per slot per operation.
 */
public interface SpreadInput {

    /** true if automated input into this slot is capped at one item per insert. */
    boolean spreadsInput(int slot);
}

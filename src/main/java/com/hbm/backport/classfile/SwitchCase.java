// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.classfile;

/** java.lang.classfile.instruction.SwitchCase (Java 24). */
public record SwitchCase(int caseValue, Label target) {
    public static SwitchCase of(int caseValue, Label target) {
        return new SwitchCase(caseValue, target);
    }
}

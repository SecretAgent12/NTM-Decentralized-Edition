// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.classfile;

/** java.lang.classfile.Label (Java 24) over an ASM label. */
public final class Label {
    final org.objectweb.asm.Label asm = new org.objectweb.asm.Label();
}

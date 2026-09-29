// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.classfile;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The part of java.lang.classfile.CodeBuilder (Java 24, not in Java 21) that the MSES
 * compiler uses, emitting through ASM instead. Same fluent calls, same instructions;
 * ClassDesc / MethodTypeDesc (java.lang.constant, Java 12+) name the types as before.
 */
public final class CodeBuilder implements Opcodes {

    private final MethodVisitor mv;
    private final List<Runnable> handlers = new ArrayList<>();

    public CodeBuilder(MethodVisitor mv) {
        this.mv = mv;
    }

    /** Emits the buffered exception table entries and closes the method. */
    public void finish() {
        handlers.forEach(Runnable::run);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static String internal(ClassDesc d) {
        String s = d.descriptorString();
        return s.startsWith("L") ? s.substring(1, s.length() - 1) : s;
    }

    private CodeBuilder insn(int op) {
        mv.visitInsn(op);
        return this;
    }

    private CodeBuilder var(int op, int slot) {
        mv.visitVarInsn(op, slot);
        return this;
    }

    private CodeBuilder jump(int op, Label target) {
        mv.visitJumpInsn(op, target.asm);
        return this;
    }

    public Label newLabel() {
        return new Label();
    }

    public CodeBuilder labelBinding(Label label) {
        mv.visitLabel(label.asm);
        return this;
    }

    public CodeBuilder aload(int slot) { return var(ALOAD, slot); }
    public CodeBuilder iload(int slot) { return var(ILOAD, slot); }
    public CodeBuilder dload(int slot) { return var(DLOAD, slot); }
    public CodeBuilder istore(int slot) { return var(ISTORE, slot); }
    public CodeBuilder dstore(int slot) { return var(DSTORE, slot); }

    public CodeBuilder iinc(int slot, int by) {
        mv.visitIincInsn(slot, by);
        return this;
    }

    public CodeBuilder ldc(ConstantDesc value) {
        mv.visitLdcInsn(value);
        return this;
    }

    public CodeBuilder iconst_m1() { return insn(ICONST_M1); }
    public CodeBuilder iconst_0() { return insn(ICONST_0); }
    public CodeBuilder iconst_1() { return insn(ICONST_1); }
    public CodeBuilder pop() { return insn(POP); }
    public CodeBuilder iand() { return insn(IAND); }
    public CodeBuilder ior() { return insn(IOR); }
    public CodeBuilder ishl() { return insn(ISHL); }
    public CodeBuilder iushr() { return insn(IUSHR); }
    public CodeBuilder idiv() { return insn(IDIV); }
    public CodeBuilder dadd() { return insn(DADD); }
    public CodeBuilder dsub() { return insn(DSUB); }
    public CodeBuilder dmul() { return insn(DMUL); }
    public CodeBuilder ddiv() { return insn(DDIV); }
    public CodeBuilder dneg() { return insn(DNEG); }
    public CodeBuilder dcmpl() { return insn(DCMPL); }
    public CodeBuilder l2i() { return insn(L2I); }
    public CodeBuilder l2d() { return insn(L2D); }
    public CodeBuilder return_() { return insn(RETURN); }
    public CodeBuilder ireturn() { return insn(IRETURN); }

    public CodeBuilder ifeq(Label t) { return jump(IFEQ, t); }
    public CodeBuilder ifne(Label t) { return jump(IFNE, t); }
    public CodeBuilder iflt(Label t) { return jump(IFLT, t); }
    public CodeBuilder ifle(Label t) { return jump(IFLE, t); }
    public CodeBuilder if_icmpeq(Label t) { return jump(IF_ICMPEQ, t); }
    public CodeBuilder if_icmpge(Label t) { return jump(IF_ICMPGE, t); }
    public CodeBuilder goto_(Label t) { return jump(GOTO, t); }

    public CodeBuilder tableswitch(int low, int high, Label dflt, List<SwitchCase> cases) {
        org.objectweb.asm.Label[] targets = new org.objectweb.asm.Label[high - low + 1];
        for (int i = 0; i < targets.length; i++) targets[i] = dflt.asm;
        for (SwitchCase c : cases) targets[c.caseValue() - low] = c.target().asm;
        mv.visitTableSwitchInsn(low, high, dflt.asm, targets);
        return this;
    }

    public CodeBuilder getfield(ClassDesc owner, String name, ClassDesc type) {
        mv.visitFieldInsn(GETFIELD, internal(owner), name, type.descriptorString());
        return this;
    }

    public CodeBuilder putfield(ClassDesc owner, String name, ClassDesc type) {
        mv.visitFieldInsn(PUTFIELD, internal(owner), name, type.descriptorString());
        return this;
    }

    private CodeBuilder invoke(int op, ClassDesc owner, String name, MethodTypeDesc type, boolean itf) {
        mv.visitMethodInsn(op, internal(owner), name, type.descriptorString(), itf);
        return this;
    }

    public CodeBuilder invokestatic(ClassDesc owner, String name, MethodTypeDesc type) {
        return invoke(INVOKESTATIC, owner, name, type, false);
    }

    public CodeBuilder invokevirtual(ClassDesc owner, String name, MethodTypeDesc type) {
        return invoke(INVOKEVIRTUAL, owner, name, type, false);
    }

    public CodeBuilder invokespecial(ClassDesc owner, String name, MethodTypeDesc type) {
        return invoke(INVOKESPECIAL, owner, name, type, false);
    }

    public CodeBuilder invokeinterface(ClassDesc owner, String name, MethodTypeDesc type) {
        return invoke(INVOKEINTERFACE, owner, name, type, true);
    }

    public CodeBuilder invokedynamic(DynamicCallSiteDesc site) {
        DirectMethodHandleDesc bsm = (DirectMethodHandleDesc) site.bootstrapMethod();
        Handle handle = new Handle(bsm.refKind(), internal(bsm.owner()), bsm.methodName(),
                bsm.lookupDescriptor(), bsm.isOwnerInterface());
        Object[] args = site.bootstrapArgs();
        mv.visitInvokeDynamicInsn(site.invocationName(), site.invocationType().descriptorString(), handle, args);
        return this;
    }

    public CodeBuilder exceptionCatch(Label start, Label end, Label handler, ClassDesc type) {
        handlers.add(() -> mv.visitTryCatchBlock(start.asm, end.asm, handler.asm, internal(type)));
        return this;
    }
}

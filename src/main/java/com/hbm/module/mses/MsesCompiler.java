// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.module.mses;

import com.hbm.util.Calculator;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.hbm.module.mses.CompiledMses.*;

public final class MsesCompiler {
    private MsesCompiler() {}

    private static final int MAX_SOURCE_UNITS = 65536;
    private static final int MAX_LINES = 32768;
    private static final int CACHE_SIZE = 64;
    private static final Map<SourceKey, WeakReference<MsesProgram>> CACHE =
            new LinkedHashMap<>(16, .75f, true);

    public static MsesProgram compile(String[] source) {
        SourceKey key = new SourceKey(source.clone());
        synchronized (CACHE) {
            WeakReference<MsesProgram> ref = CACHE.get(key);
            MsesProgram cached = ref == null ? null : ref.get();
            if (cached != null) return cached;
        }
        Unit unit = decode(key.lines);
        MsesProgram result = new MsesProgram(new Interpreter(unit), unit.slots);
        synchronized (CACHE) {
            CACHE.put(key, new WeakReference<>(result));
            while (CACHE.size() > CACHE_SIZE) CACHE.remove(CACHE.keySet().iterator().next());
        }
        return result;
    }

    private static void validate(String[] source) {
        if (source.length > MAX_LINES) throw new IllegalArgumentException("too many MSES lines");
        long units = source.length;
        for (String line : source) {
            if (line == null) continue;
            units += line.length();
            int modifiedUtf8 = 0;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                modifiedUtf8 += c >= 1 && c <= 127 ? 1 : c <= 2047 ? 2 : 3;
            }
            if (modifiedUtf8 > 60000)
                throw new IllegalArgumentException("MSES line too large for the constant pool");
        }
        if (units > MAX_SOURCE_UNITS) throw new IllegalArgumentException("MSES source too large");
    }

    private static final class SourceKey {
        final String[] lines;
        final int hash;

        SourceKey(String[] lines) {
            this.lines = lines;
            hash = Arrays.hashCode(lines);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof SourceKey k && Arrays.equals(lines, k.lines);
        }
    }

    private enum Op {
        STATUS,
        NULL_LINE,
        BUFFER,
        BUFFER_PLAIN,
        CONST_DOUBLE,
        CONST_INT,
        LOAD,
        SAVE,
        CLOCK,
        JUMP,
        JUMPIF,
        JUMPNOT,
        EVAL,
        EVAL_BUFFER,
        ROUND,
        CONCAT,
        EQ,
        COMPARE,
        SPLITTER,
        SPLIT,
        SPLITCOUNT,
        PUSH_BUFFER,
        PUSH,
        POP,
        PEEK,
        LENGTH,
        FIRST,
        LAST,
        SEND,
        LISTEN,
        POLL,
        WORLDTIME
    }

    private static final class Insn {
        final Op op;
        final String argument;
        final MsesTemplate template;
        int parameter;
        int target = -1;
        double number;
        MsesExpression.Plan expression;

        Insn(Op op, String argument, int parameter) {
            this.op = op;
            this.argument = argument;
            this.parameter = parameter;
            template = argument == null ? null : MsesTemplate.parse(argument);
        }
    }

    private record Unit(Insn[] code, String[] slots, Map<String, Integer> slotOf) {}

    private static Insn status(int code) {
        return new Insn(Op.STATUS, null, code);
    }

    private static Insn op(Op op) {
        return new Insn(op, null, 0);
    }

    private static Insn arg(Op op, String line, int offset) {
        return line.length() <= offset
                ? status(PARAMETER_ERROR)
                : new Insn(op, line.substring(offset), 0);
    }

    private static Insn integer(Op op, String line, int offset) {
        if (line.length() <= offset) return status(PARAMETER_ERROR);
        try {
            return new Insn(op, null, Integer.parseInt(line.substring(offset)));
        } catch (NumberFormatException ex) {
            return status(PARAMETER_ERROR);
        }
    }

    private static Insn substitutableInteger(Op op, String line, int offset) {
        Insn ins = arg(op, line, offset);
        if (ins.op == Op.STATUS || !ins.template.constant()) return ins;
        try {
            ins.parameter = Integer.parseInt(ins.template.constantValue());
            return ins;
        } catch (NumberFormatException ex) {
            return status(PARAMETER_ERROR);
        }
    }

    private static Unit decode(String[] lines) {
        validate(lines);
        Map<String, Integer> destinations = new HashMap<>();
        for (int i = 0; i < lines.length; i++) {
            String s = lines[i];

            if (s != null && s.startsWith("dest ") && s.length() > 5)
                destinations.put(s.substring(5), i);
        }
        Insn[] code = new Insn[lines.length];
        Map<String, Integer> slotOf = new LinkedHashMap<>();
        for (int i = 0; i < lines.length; i++) {
            Insn ins = decode(lines[i], destinations);
            code[i] = ins;
            if (ins.op == Op.LOAD || ins.op == Op.SAVE)
                slotOf.putIfAbsent(ins.argument, slotOf.size());
            else if (ins.template != null) {
                for (MsesTemplate.Part part : ins.template.parts()) {
                    if (part.variable() && !part.text().equals("buffer")) {
                        slotOf.putIfAbsent(part.text(), slotOf.size());
                    }
                }
            }
        }
        return new Unit(code, slotOf.keySet().toArray(String[]::new), slotOf);
    }

    private static Insn decode(String line, Map<String, Integer> destinations) {
        if (line == null) return op(Op.NULL_LINE);
        String s = line.toLowerCase(Locale.US);
        if (line.isEmpty() || s.startsWith("dest ") || s.startsWith("# ")) return status(SKIP);
        Insn exact =
                switch (s) {
                    case "nop" -> status(OK);
                    case "endtick" -> status(END_TICK);
                    case "shutdown" -> status(SHUTDOWN);
                    case "eval" -> op(Op.EVAL_BUFFER);
                    case "evalr" -> new Insn(Op.EVAL_BUFFER, null, 1);
                    case "floor", "rounddown" -> new Insn(Op.ROUND, null, 0);
                    case "ceil", "roundup" -> new Insn(Op.ROUND, null, 1);
                    case "round", "nearest" -> new Insn(Op.ROUND, null, 2);
                    case "splitcount" -> op(Op.SPLITCOUNT);
                    case "push" -> op(Op.PUSH_BUFFER);
                    case "pop" -> op(Op.POP);
                    case "peek" -> op(Op.PEEK);
                    case "length" -> op(Op.LENGTH);
                    case "worldtime" -> op(Op.WORLDTIME);
                    default -> null;
                };
        if (exact != null) return exact;
        if (s.startsWith("clockspeed ")) return integer(Op.CLOCK, line, 11);
        if (s.startsWith("split ")) return substitutableInteger(Op.SPLIT, line, 6);
        if (s.startsWith("first ")) return substitutableInteger(Op.FIRST, line, 6);
        if (s.startsWith("last ")) return substitutableInteger(Op.LAST, line, 5);
        if (s.startsWith("jmp ") || s.startsWith("jmpif ") || s.startsWith("jmpnot ")) {
            Op kind =
                    s.startsWith("jmp ")
                            ? Op.JUMP
                            : s.startsWith("jmpif ") ? Op.JUMPIF : Op.JUMPNOT;
            Insn ins = arg(kind, line, kind == Op.JUMP ? 4 : kind == Op.JUMPIF ? 6 : 7);
            if (ins.template != null && ins.template.constant()) {
                ins.target = destinations.getOrDefault(ins.template.constantValue(), -1);
            }
            return ins;
        }
        if (s.startsWith("eval ") || s.startsWith("evalr ")) {
            boolean rounded = s.startsWith("evalr ");
            Insn ins = arg(Op.EVAL, line, rounded ? 6 : 5);
            if (ins.op != Op.EVAL) return ins;
            ins.parameter = rounded ? 1 : 0;
            String expression =
                    ins.template.constant() ? ins.template.constantValue() : ins.argument;
            ins.expression = MsesExpression.tryParse(expression);

            if (ins.template.constant() && canFoldLiteral(expression)) {
                double value;
                try {
                    value = Calculator.evaluateExpression(expression);
                } catch (RuntimeException ex) {
                    return status(PARAMETER_ERROR);
                }
                if (rounded) return new Insn(Op.CONST_INT, null, (int) Math.round(value));
                Insn folded = new Insn(Op.CONST_DOUBLE, null, 0);
                folded.number = value;
                return folded;
            }
            return ins;
        }
        if (s.startsWith("load ")) return arg(Op.LOAD, line, 5);
        if (s.startsWith("save ")) return arg(Op.SAVE, line, 5);
        if (s.startsWith("buffer ")) {
            Insn ins = arg(Op.BUFFER, line, 7);
            if (ins.op == Op.BUFFER
                    && ins.argument.length() <= MsesState.MAX_BUFFER_LENGTH
                    && MsesRuntime.plainNumber(ins.argument, true)) {
                Insn plain = new Insn(Op.BUFFER_PLAIN, ins.argument, 0);
                plain.number = Double.parseDouble(ins.argument);
                return plain;
            }
            return ins;
        }
        if (s.startsWith("concat ")) return arg(Op.CONCAT, line, 7);
        if (s.startsWith("eq ")) return arg(Op.EQ, line, 3);
        if (s.startsWith("gtb ")
                || s.startsWith("ltb ")
                || s.startsWith("geb ")
                || s.startsWith("leb ")) {
            Insn ins = arg(Op.COMPARE, line, 4);
            if (ins.op != Op.COMPARE) return ins;
            ins.parameter =
                    s.startsWith("gtb ")
                            ? 0
                            : s.startsWith("ltb ") ? 1 : s.startsWith("geb ") ? 2 : 3;
            if (ins.template.constant()) {
                try {
                    ins.number = Double.parseDouble(ins.template.constantValue());
                } catch (NumberFormatException ex) {

                    return status(PARAMETER_ERROR);
                }
            }
            return ins;
        }
        if (s.startsWith("send ")) return arg(Op.SEND, line, 5);
        if (s.startsWith("listen ")) return arg(Op.LISTEN, line, 7);
        if (s.startsWith("poll ")) return arg(Op.POLL, line, 5);
        if (s.startsWith("splitter ")) return arg(Op.SPLITTER, line, 9);
        if (s.startsWith("push ")) return arg(Op.PUSH, line, 5);
        return status(UNRECOGNIZED_COMMAND);
    }

    private static boolean canFoldLiteral(String expression) {
        if (expression.length() > 2048 || expression.indexOf('!') >= 0) return false;
        int depth = 0, powers = 0;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '(' && ++depth > 48) return false;
            if (ch == ')') depth--;
            if (ch == '^' && ++powers > 32) return false;
        }
        return true;
    }

    /**
     * backport: runs a decoded program directly instead of compiling it to a hidden JVM class.
     * NTM: NEXT 1.0.0 translated MSES into bytecode and defined it with defineHiddenClass; CurseForge
     * rejects mods that generate classes at runtime, so DE interprets the same decoded instructions.
     * The semantics (instruction budget, clock speed, error handling, jumps, expressions) follow the
     * compiled form line by line, only slower.
     */
    private static final class Interpreter implements CompiledMses {
        private static final int INSTRUCTION_BUDGET = 100;

        private final Unit unit;

        Interpreter(Unit unit) {
            this.unit = unit;
        }

        @Override
        public void run(MsesState state, Host host) {
            Insn[] code = unit.code;
            int length = code.length;
            int cost = 0;
            int remaining = INSTRUCTION_BUDGET;
            while (true) {
                if (cost >= state.clockSpeed || remaining <= 0) return;
                int current = state.current;
                if (current == length) {
                    host.msesEndOfProgram(false);
                    return;
                }
                if (current < 0 || current > length) {
                    host.msesEndOfProgram(true);
                    return;
                }
                Insn ins = code[current];
                remaining--;
                int result;
                try {
                    state.current = current + 1;
                    result = execute(state, ins);
                    host.msesAfterInstruction(current, result);
                } catch (Exception ex) {
                    host.msesEvaluationFailed();
                    cost++;
                    continue;
                }
                if (ins.op == Op.STATUS && ins.parameter == END_TICK) return;
                if (ins.op == Op.CLOCK) {
                    if (result != SKIP) cost++;
                } else if (!(ins.op == Op.STATUS && ins.parameter == SKIP)) {
                    cost++;
                }
            }
        }

        private int execute(MsesState state, Insn ins) {
            boolean needsBuffer =
                    ins.op == Op.CONCAT || ins.op == Op.EQ || ins.op == Op.COMPARE || ins.op == Op.SEND;
            if (needsBuffer && state.bufferEmpty()) return PARAMETER_ERROR;
            return switch (ins.op) {
                case STATUS -> ins.parameter;
                case NULL_LINE -> MsesRuntime.nullLine();
                case BUFFER -> {
                    state.writeBuffer(ins.argument);
                    yield OK;
                }
                case BUFFER_PLAIN -> {
                    state.writePlain(ins.argument, ins.number);
                    yield OK;
                }
                case CONST_DOUBLE -> {
                    state.writeDouble(ins.number, Double.toString(ins.number));
                    yield OK;
                }
                case CONST_INT -> {
                    state.writeInt(ins.parameter);
                    yield OK;
                }
                case LOAD -> state.load(unit.slotOf.get(ins.argument));
                case SAVE -> state.save(unit.slotOf.get(ins.argument));
                case CLOCK -> MsesRuntime.clockspeed(state, ins.parameter);
                case JUMP, JUMPIF, JUMPNOT -> jump(state, ins);
                case EVAL -> evaluate(state, ins);
                case EVAL_BUFFER -> MsesRuntime.evaluateBuffer(state, ins.parameter != 0);
                case ROUND -> MsesRuntime.round(state, ins.parameter);
                case CONCAT -> {
                    state.writeBuffer(template(state, ins.template, false));
                    yield OK;
                }
                case EQ -> MsesRuntime.eq(state, template(state, ins.template, false));
                case SEND -> MsesRuntime.send(state, template(state, ins.template, false));
                case LISTEN -> MsesRuntime.listen(state, template(state, ins.template, false));
                case POLL -> MsesRuntime.poll(state, template(state, ins.template, false));
                case SPLITTER -> MsesRuntime.splitter(state, template(state, ins.template, false));
                case PUSH -> MsesRuntime.push(state, template(state, ins.template, false));
                case COMPARE -> compare(state, ins);
                case SPLIT ->
                        ins.template.constant()
                                ? MsesRuntime.split(state, ins.parameter)
                                : MsesRuntime.split(state, template(state, ins.template, true));
                case FIRST, LAST ->
                        ins.template.constant()
                                ? MsesRuntime.substring(state, ins.parameter, ins.op == Op.LAST)
                                : MsesRuntime.substring(
                                        state, template(state, ins.template, true), ins.op == Op.LAST);
                case SPLITCOUNT -> MsesRuntime.splitcount(state);
                case PUSH_BUFFER -> MsesRuntime.pushBuffer(state);
                case POP -> MsesRuntime.pop(state);
                case PEEK -> MsesRuntime.peek(state);
                case LENGTH -> MsesRuntime.length(state);
                case WORLDTIME -> MsesRuntime.worldtime(state);
            };
        }

        private int jump(MsesState state, Insn ins) {
            if (ins.op == Op.JUMPIF && !state.bufferIsTrue()) return OK;
            if (ins.op == Op.JUMPNOT && state.bufferIsTrue()) return OK;
            if (ins.template.constant()) {
                if (ins.target < 0) return PARAMETER_ERROR;
                state.current = ins.target;
                return OK;
            }
            return MsesRuntime.jump(state, template(state, ins.template, false));
        }

        private int compare(MsesState state, Insn ins) {
            try {
                double buffer = state.bufferJava();
                List<MsesTemplate.Part> parts = ins.template.parts();
                double value;
                if (ins.template.constant()) value = ins.number;
                else if (parts.size() == 1 && parts.getFirst().text().equals("buffer"))
                    value = state.bufferJava();
                else if (parts.size() == 1)
                    value = state.slotJava(unit.slotOf.get(parts.getFirst().text()));
                else value = Double.parseDouble(template(state, ins.template, false));
                return MsesRuntime.compare(state, buffer, value, ins.parameter);
            } catch (Exception ex) {
                return PARAMETER_ERROR;
            }
        }

        private int evaluate(MsesState state, Insn ins) {
            MsesExpression.Plan plan = ins.expression;
            if (plan != null) {
                double[] inputs = new double[plan.inputs().size()];
                boolean plain = true;
                for (int i = 0; i < inputs.length; i++) {
                    MsesExpression.Input input = plan.inputs().get(i);
                    double v =
                            input.name().equals("buffer")
                                    ? state.bufferPlain(input.allowNegative())
                                    : state.slotPlain(
                                            unit.slotOf.get(input.name()), input.allowNegative());
                    if (Double.isNaN(v)) {
                        plain = false;
                        break;
                    }
                    inputs[i] = v;
                }
                if (plain) {
                    double value = node(plan.root(), inputs);
                    if (ins.parameter != 0) state.writeInt((int) Math.round(value));
                    else state.writeDouble(value);
                    return OK;
                }
            }
            return MsesRuntime.evaluate(
                    state, template(state, ins.template, true), ins.parameter != 0);
        }

        private static double node(MsesExpression.Node node, double[] inputs) {
            if (node instanceof MsesExpression.Constant n) return n.value();
            if (node instanceof MsesExpression.Variable n)
                return n.negate() ? -inputs[n.index()] : inputs[n.index()];
            if (node instanceof MsesExpression.Binary n) {
                double l = node(n.left(), inputs), r = node(n.right(), inputs);
                return switch (n.operator()) {
                    case '+' -> l + r;
                    case '-' -> l - r;
                    case '*' -> l * r;
                    case '/' -> l / r;
                    default -> throw new AssertionError(n.operator());
                };
            }
            if (node instanceof MsesExpression.Function n) {
                double x = node(n.argument(), inputs);
                return switch (n.name()) {
                    case "round" -> (double) Math.round(x);
                    case "log" -> Math.log10(x);
                    case "ln" -> Math.log(x);
                    case "sqrt" -> Math.sqrt(x);
                    case "sin" -> Math.sin(x);
                    case "cos" -> Math.cos(x);
                    case "tan" -> Math.tan(x);
                    case "asin" -> Math.asin(x);
                    case "acos" -> Math.acos(x);
                    case "atan" -> Math.atan(x);
                    case "ceil" -> Math.ceil(x);
                    case "floor" -> Math.floor(x);
                    default -> throw new AssertionError(n.name());
                };
            }
            throw new AssertionError(node);
        }

        private String template(MsesState state, MsesTemplate t, boolean numeric) {
            if (t.constant()) return t.constantValue();
            if (t.parts().size() > 24) return MsesRuntime.substitute(state, t.source(), numeric);
            StringBuilder out = new StringBuilder();
            for (MsesTemplate.Part part : t.parts()) {
                if (!part.variable()) out.append(part.text());
                else if (part.text().equals("buffer")) out.append(state.bufferText(numeric));
                else out.append(state.slotText(unit.slotOf.get(part.text()), numeric));
            }
            return out.toString();
        }
    }
}

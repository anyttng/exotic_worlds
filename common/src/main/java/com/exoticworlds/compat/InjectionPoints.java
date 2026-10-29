package com.exoticworlds.compat;

import java.lang.classfile.Annotation;
import java.lang.classfile.CodeElement;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.util.List;
import java.util.function.ToIntFunction;

import com.exoticworlds.compat.MixinTargetCheck.Reason;
import com.exoticworlds.compat.MixinTargetCheck.Refusal;

import org.jspecify.annotations.Nullable;

final class InjectionPoints {
    record Selection(String owner, List<MethodModel> bodies, List<String> selfTargets) {
    }

    private static final String TARGET_KEY = "target";
    private static final String ORDINAL_KEY = "ordinal";
    private static final String OPCODE_KEY = "opcode";

    private static final String AT_INVOKE = "INVOKE";
    private static final String AT_FIELD = "FIELD";
    private static final String AT_NEW = "NEW";

    private static final int ANY_ORDINAL = -1;
    private static final int ANY_OPCODE = -1;

    private InjectionPoints() {
    }

    static @Nullable Refusal check(String mixinClassName, Selection selection, Annotation at) {
        String target = MixinAnnotations.element(at, TARGET_KEY).map(MixinAnnotations::asString).orElse("");
        int needed = Math.max(MixinAnnotations.integer(at, ORDINAL_KEY, ANY_ORDINAL), 0) + 1;
        Reason reason;
        ToIntFunction<MethodModel> counter;
        switch (MixinAnnotations.string(at, MixinAnnotations.VALUE_KEY)) {
            case AT_INVOKE -> {
                reason = Reason.MISSING_INVOKE;
                MemberRef wanted = MemberRef.parse(target);
                counter = body -> countInvokes(body, wanted, selection);
            }
            case AT_FIELD -> {
                reason = Reason.MISSING_FIELD_ACCESS;
                MemberRef wanted = MemberRef.parse(target);
                int opcode = MixinAnnotations.integer(at, OPCODE_KEY, ANY_OPCODE);
                counter = body -> countFieldAccesses(body, wanted, opcode, selection);
            }
            case AT_NEW -> {
                reason = Reason.MISSING_NEW;
                counter = body -> countNews(body, target);
            }
            default -> {
                return null;
            }
        }

        for (MethodModel body : selection.bodies()) {
            if (counter.applyAsInt(body) >= needed) {
                return null;
            }
        }

        return new Refusal(mixinClassName,
                selection.owner() + "#" + selection.bodies().getFirst().methodName().stringValue() + "@" + target,
                reason);
    }

    private static String merged(String owner, Selection selection, MemberRef wanted) {
        return owner.equals(selection.owner()) && wanted.owner() != null
                && selection.selfTargets().contains(wanted.owner())
                ? wanted.owner()
                : owner;
    }

    private static int countInvokes(MethodModel body, MemberRef wanted, Selection selection) {
        int count = 0;
        for (CodeElement element : codeOf(body)) {
            if (element instanceof InvokeInstruction invoke
                    && wanted.matches(merged(invoke.owner().asInternalName(), selection, wanted),
                            invoke.name().stringValue(),
                            invoke.type().stringValue())) {
                count++;
            }
        }

        return count;
    }

    private static int countFieldAccesses(MethodModel body, MemberRef wanted, int opcode, Selection selection) {
        int count = 0;
        for (CodeElement element : codeOf(body)) {
            if (element instanceof FieldInstruction access
                    && (opcode == ANY_OPCODE || access.opcode().bytecode() == opcode)
                    && wanted.matches(merged(access.owner().asInternalName(), selection, wanted),
                            access.name().stringValue(),
                            access.type().stringValue())) {
                count++;
            }
        }

        return count;
    }

    private static int countNews(MethodModel body, String target) {
        boolean byConstructor = target.startsWith("(");
        String created = byConstructor ? MemberRef.unwrapDescriptor(target.substring(target.indexOf(')') + 1))
                : MemberRef.unwrapDescriptor(target);
        String constructor = byConstructor ? MemberRef.parametersOf(target) + MemberRef.VOID_RETURN : null;
        int count = 0;
        for (CodeElement element : codeOf(body)) {
            if (byConstructor && element instanceof InvokeInstruction invoke
                    && invoke.opcode() == Opcode.INVOKESPECIAL
                    && invoke.owner().asInternalName().equals(created)
                    && invoke.name().equalsString(MemberRef.CONSTRUCTOR)
                    && invoke.type().equalsString(constructor)) {
                count++;
            } else if (!byConstructor && element instanceof NewObjectInstruction creation
                    && creation.className().asInternalName().equals(created)) {
                count++;
            }
        }

        return count;
    }

    private static Iterable<CodeElement> codeOf(MethodModel method) {
        return method.code().<Iterable<CodeElement>>map(code -> code).orElse(List.of());
    }
}

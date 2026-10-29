package com.exoticworlds.compat;

import java.util.List;
import java.util.function.ToIntFunction;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import com.exoticworlds.compat.MixinTargetCheck.Reason;
import com.exoticworlds.compat.MixinTargetCheck.Refusal;

final class InjectionPoints {
    record Selection(String owner, List<MethodNode> bodies, List<String> selfTargets) {
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

    static @Nullable Refusal check(String mixinClassName, Selection selection, AnnotationNode at) {
        String target = MixinAnnotations.element(at, TARGET_KEY).map(MixinAnnotations::asString).orElse("");
        int needed = Math.max(MixinAnnotations.integer(at, ORDINAL_KEY, ANY_ORDINAL), 0) + 1;
        Reason reason;
        ToIntFunction<MethodNode> counter;
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

        for (MethodNode body : selection.bodies()) {
            if (counter.applyAsInt(body) >= needed) {
                return null;
            }
        }

        return new Refusal(mixinClassName, selection.owner() + "#" + selection.bodies().get(0).name + "@" + target,
                reason);
    }

    private static String merged(String owner, Selection selection, MemberRef wanted) {
        return owner.equals(selection.owner()) && wanted.owner() != null
                && selection.selfTargets().contains(wanted.owner())
                ? wanted.owner()
                : owner;
    }

    private static int countInvokes(MethodNode body, MemberRef wanted, Selection selection) {
        int count = 0;
        for (AbstractInsnNode element : body.instructions) {
            if (element instanceof MethodInsnNode invoke
                    && wanted.matches(merged(invoke.owner, selection, wanted), invoke.name, invoke.desc)) {
                count++;
            }
        }

        return count;
    }

    private static int countFieldAccesses(MethodNode body, MemberRef wanted, int opcode, Selection selection) {
        int count = 0;
        for (AbstractInsnNode element : body.instructions) {
            if (element instanceof FieldInsnNode access
                    && (opcode == ANY_OPCODE || access.getOpcode() == opcode)
                    && wanted.matches(merged(access.owner, selection, wanted), access.name, access.desc)) {
                count++;
            }
        }

        return count;
    }

    private static int countNews(MethodNode body, String target) {
        boolean byConstructor = target.startsWith("(");
        String created = byConstructor ? MemberRef.unwrapDescriptor(target.substring(target.indexOf(')') + 1))
                : MemberRef.unwrapDescriptor(target);
        String constructor = byConstructor ? MemberRef.parametersOf(target) + MemberRef.VOID_RETURN : null;
        int count = 0;
        for (AbstractInsnNode element : body.instructions) {
            if (byConstructor && element instanceof MethodInsnNode invoke
                    && invoke.getOpcode() == Opcodes.INVOKESPECIAL
                    && invoke.owner.equals(created)
                    && invoke.name.equals(MemberRef.CONSTRUCTOR)
                    && invoke.desc.equals(constructor)) {
                count++;
            } else if (!byConstructor && element instanceof TypeInsnNode creation
                    && creation.getOpcode() == Opcodes.NEW
                    && creation.desc.equals(created)) {
                count++;
            }
        }

        return count;
    }
}

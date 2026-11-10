package com.exoticworlds.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

// 1.21 runs Mojmap on NeoForge and intermediary on Fabric, so a member or a whole class can be spelled per loader.
final class LoaderSpellings {
    enum Namespace {
        MOJMAP,
        INTERMEDIARY,
        UNKNOWN
    }

    private static final Pattern INTERMEDIARY_METHOD = Pattern.compile("method_\\d+");
    private static final Pattern INTERMEDIARY_CLASS = Pattern.compile("net/minecraft/class_\\d+");
    private static final String GAME_PACKAGE = "net/minecraft/";
    private static final String INTERMEDIARY_LEVEL = "net/minecraft/class_1937.class";
    private static final String MOJMAP_LEVEL = "net/minecraft/world/level/Level.class";

    private LoaderSpellings() {
    }

    // A method named twice, once per loader's spelling, holds when either one is found.
    static boolean twinSelected(String selector, List<String> selectors, Predicate<String> found) {
        int parameters = parameterCount(selector);
        for (String other : selectors) {
            if (!other.equals(selector) && spelledApart(selector, other)
                    && (parameters < 0 || parameterCount(other) < 0 || parameterCount(other) == parameters)
                    && found.test(other)) {
                return true;
            }
        }

        return false;
    }

    private static boolean spelledApart(String selector, String other) {
        MemberRef selected = MemberRef.parse(selector);
        MemberRef twin = MemberRef.parse(other);
        if (intermediaryName(selected) != intermediaryName(twin)) {
            return true;
        }

        return selected.name().equals(twin.name()) && intermediaryDescriptor(selected) != intermediaryDescriptor(twin);
    }

    private static boolean intermediaryName(MemberRef member) {
        return INTERMEDIARY_METHOD.matcher(member.name()).matches();
    }

    private static boolean intermediaryDescriptor(MemberRef member) {
        return member.descriptor() != null && INTERMEDIARY_CLASS.matcher(member.descriptor()).find();
    }

    static int parameterCount(String selector) {
        String descriptor = MemberRef.parse(selector).descriptor();
        return descriptor == null || !descriptor.startsWith("(") ? -1 : Type.getArgumentTypes(descriptor).length;
    }

    static Namespace runtimeOf(ClassLoader classLoader) {
        return classLoader.getResource(INTERMEDIARY_LEVEL) != null ? Namespace.INTERMEDIARY
                : classLoader.getResource(MOJMAP_LEVEL) != null ? Namespace.MOJMAP : Namespace.UNKNOWN;
    }

    static Namespace namespaceOf(ClassNode model) {
        List<String> spellings = new ArrayList<>();
        model.fields.forEach(field -> spellings.add(field.desc));
        for (MethodNode method : model.methods) {
            spellings.add(method.desc);
            for (AbstractInsnNode element : method.instructions) {
                if (element instanceof MethodInsnNode invoke) {
                    spellings.add(invoke.owner);
                    spellings.add(invoke.desc);
                } else if (element instanceof FieldInsnNode access) {
                    spellings.add(access.owner);
                    spellings.add(access.desc);
                }
            }
        }

        boolean intermediary = false;
        boolean mojmap = false;
        for (String spelling : spellings) {
            for (int index = spelling.indexOf(GAME_PACKAGE); index >= 0;
                    index = spelling.indexOf(GAME_PACKAGE, index + 1)) {
                if (INTERMEDIARY_CLASS.matcher(spelling).region(index, spelling.length()).lookingAt()) {
                    intermediary = true;
                } else {
                    mojmap = true;
                }
            }
        }

        return intermediary == mojmap ? Namespace.UNKNOWN : intermediary ? Namespace.INTERMEDIARY : Namespace.MOJMAP;
    }
}

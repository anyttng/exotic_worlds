package com.exoticworlds.compat.gate;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.exoticworlds.compat.gate.ClassFiles.Loaded;
import com.exoticworlds.compat.gate.MixinTargetCheck.Reason;
import com.exoticworlds.compat.gate.MixinTargetCheck.Refusal;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

final class ReferenceCheck {
    private static final String OBJECT = "java/lang/Object";
    private static final String ARRAY_PREFIX = "[";
    private static final Set<String> OBJECT_METHODS = objectMethods();

    private final ClassFiles classFiles;
    private final List<String> modPackages;

    ReferenceCheck(ClassFiles classFiles, List<String> modPackages) {
        this.classFiles = classFiles;
        this.modPackages = modPackages;
    }

    List<Refusal> check(String className) {
        ClassNode model = this.classFiles.load(className.replace('.', '/')).model();
        if (model == null) {
            return List.of();
        }

        Set<Refusal> refusals = new LinkedHashSet<>();
        if (model.superName != null) {
            requireClass(className, model.superName, refusals);
        }

        for (String type : model.interfaces) {
            requireClass(className, type, refusals);
        }

        for (MethodNode method : model.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                checkInstruction(className, instruction, refusals);
            }

            for (TryCatchBlockNode handler : method.tryCatchBlocks) {
                if (handler.type != null) {
                    requireClass(className, handler.type, refusals);
                }
            }
        }

        return new ArrayList<>(refusals);
    }

    private void checkInstruction(String className, AbstractInsnNode instruction, Set<Refusal> refusals) {
        switch (instruction) {
            case MethodInsnNode invoke -> requireMethod(className, invoke.owner, invoke.name, invoke.desc, refusals);
            case FieldInsnNode access -> requireField(className, access.owner, access.name, access.desc, refusals);
            case TypeInsnNode type -> requireClass(className, type.desc, refusals);
            case MultiANewArrayInsnNode array -> requireClass(className, array.desc, refusals);
            case LdcInsnNode constant when constant.cst instanceof Type type
                    && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) ->
                    requireClass(className, type.getInternalName(), refusals);
            case InvokeDynamicInsnNode dynamic -> {
                requireClass(className,
                        MemberRef.unwrapDescriptor(Type.getReturnType(dynamic.desc).getDescriptor()), refusals);
                for (Object argument : dynamic.bsmArgs) {
                    if (argument instanceof Handle handle) {
                        requireHandle(className, handle, refusals);
                    }
                }
            }
            default -> {
            }
        }
    }

    private void requireHandle(String className, Handle handle, Set<Refusal> refusals) {
        switch (handle.getTag()) {
            case Opcodes.H_GETFIELD, Opcodes.H_GETSTATIC, Opcodes.H_PUTFIELD, Opcodes.H_PUTSTATIC ->
                    requireField(className, handle.getOwner(), handle.getName(), handle.getDesc(), refusals);
            default -> requireMethod(className, handle.getOwner(), handle.getName(), handle.getDesc(), refusals);
        }
    }

    private void requireClass(String className, String type, Set<Refusal> refusals) {
        String element = elementOf(type);
        if (element != null && inMod(element) && !this.classFiles.load(element).present()) {
            refusals.add(new Refusal(className, element, Reason.MISSING_CLASS));
        }
    }

    private void requireMethod(String className, String owner, String name, String descriptor,
            Set<Refusal> refusals) {
        if (owner.startsWith(ARRAY_PREFIX)) {
            requireClass(className, owner, refusals);
            return;
        }

        if (!inMod(owner)) {
            return;
        }

        if (!this.classFiles.load(owner).present()) {
            refusals.add(new Refusal(className, owner, Reason.MISSING_CLASS));
            return;
        }

        boolean constructor = name.equals(MemberRef.CONSTRUCTOR);
        Predicate<ClassNode> declares = model -> declaresMethod(model, name, descriptor);
        boolean found = constructor ? declaredBy(owner, declares) : resolves(owner, declares,
                OBJECT_METHODS.contains(name + descriptor));
        if (!found) {
            refusals.add(new Refusal(className, owner + "#" + name + descriptor, Reason.MISSING_METHOD));
        }
    }

    private void requireField(String className, String owner, String name, String descriptor,
            Set<Refusal> refusals) {
        if (!inMod(owner)) {
            return;
        }

        if (!this.classFiles.load(owner).present()) {
            refusals.add(new Refusal(className, owner, Reason.MISSING_CLASS));
            return;
        }

        if (!resolves(owner, model -> declaresField(model, name, descriptor), false)) {
            refusals.add(new Refusal(className, owner + "#" + name + ":" + descriptor, Reason.MISSING_FIELD));
        }
    }

    private boolean declaredBy(String owner, Predicate<ClassNode> declares) {
        ClassNode model = this.classFiles.load(owner).model();
        return model == null || declares.test(model);
    }

    // A supertype outside the mod's packages is never loaded during mixin bootstrap, so what it may declare counts.
    private boolean resolves(String owner, Predicate<ClassNode> declares, boolean onObject) {
        if (owner.equals(OBJECT)) {
            return onObject;
        }

        if (!inMod(owner)) {
            return true;
        }

        Loaded loaded = this.classFiles.load(owner);
        if (!loaded.present()) {
            return false;
        }

        ClassNode model = loaded.model();
        if (model == null || declares.test(model)) {
            return true;
        }

        if (model.superName != null && resolves(model.superName, declares, onObject)) {
            return true;
        }

        for (String type : model.interfaces) {
            if (resolves(type, declares, false)) {
                return true;
            }
        }

        return false;
    }

    private static boolean declaresMethod(ClassNode model, String name, String descriptor) {
        for (MethodNode method : model.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)) {
                return true;
            }
        }

        return false;
    }

    private static boolean declaresField(ClassNode model, String name, String descriptor) {
        for (FieldNode field : model.fields) {
            if (field.name.equals(name) && field.desc.equals(descriptor)) {
                return true;
            }
        }

        return false;
    }

    private boolean inMod(String internalName) {
        for (String modPackage : this.modPackages) {
            if (internalName.startsWith(modPackage)) {
                return true;
            }
        }

        return false;
    }

    private static @Nullable String elementOf(String type) {
        if (!type.startsWith(ARRAY_PREFIX)) {
            return type;
        }

        String element = type.substring(type.lastIndexOf(ARRAY_PREFIX) + 1);
        return element.startsWith("L") && element.endsWith(";") ? MemberRef.unwrapDescriptor(element) : null;
    }

    private static Set<String> objectMethods() {
        Set<String> methods = new LinkedHashSet<>();
        for (Method method : Object.class.getDeclaredMethods()) {
            methods.add(method.getName()
                    + MethodType.methodType(method.getReturnType(), method.getParameterTypes()).toMethodDescriptorString());
        }

        return Set.copyOf(methods);
    }
}

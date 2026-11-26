package com.exoticworlds.compat.gate;

import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.ExceptionCatch;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.NewMultiArrayInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
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

final class ReferenceCheck {
    private static final String OBJECT = "java/lang/Object";
    private static final String ARRAY_PREFIX = "[";
    private static final Set<String> OBJECT_METHODS = readObjectMethods();

    private final ClassFiles classFiles;
    private final List<String> modPackages;

    ReferenceCheck(ClassFiles classFiles, List<String> modPackages) {
        this.classFiles = classFiles;
        this.modPackages = modPackages;
    }

    List<Refusal> check(String className) {
        ClassModel model = this.classFiles.load(className.replace('.', '/')).model();
        if (model == null) {
            return List.of();
        }

        Set<Refusal> refusals = new LinkedHashSet<>();
        model.superclass().ifPresent(type -> requireClass(className, type.asInternalName(), refusals));
        for (ClassEntry type : model.interfaces()) {
            requireClass(className, type.asInternalName(), refusals);
        }

        for (MethodModel method : model.methods()) {
            method.code().ifPresent(code -> {
                for (CodeElement element : code) {
                    checkElement(className, element, refusals);
                }
            });
        }

        return new ArrayList<>(refusals);
    }

    private void checkElement(String className, CodeElement element, Set<Refusal> refusals) {
        switch (element) {
            case InvokeInstruction invoke -> requireMethod(className, invoke.owner().asInternalName(),
                    invoke.name().stringValue(), invoke.type().stringValue(), refusals);
            case FieldInstruction access -> requireField(className, access.owner().asInternalName(),
                    access.name().stringValue(), access.type().stringValue(), refusals);
            case NewObjectInstruction creation -> requireClass(className, creation.className().asInternalName(),
                    refusals);
            case TypeCheckInstruction check -> requireClass(className, check.type().asInternalName(), refusals);
            case NewReferenceArrayInstruction array -> requireClass(className,
                    array.componentType().asInternalName(), refusals);
            case NewMultiArrayInstruction array -> requireClass(className, array.arrayType().asInternalName(),
                    refusals);
            case ConstantInstruction.LoadConstantInstruction constant
                    when constant.constantEntry() instanceof ClassEntry type ->
                    requireClass(className, type.asInternalName(), refusals);
            case ExceptionCatch handler -> handler.catchType()
                    .ifPresent(type -> requireClass(className, type.asInternalName(), refusals));
            case InvokeDynamicInstruction dynamic -> {
                requireClass(className,
                        MemberRef.unwrapDescriptor(dynamic.typeSymbol().returnType().descriptorString()), refusals);
                for (ConstantDesc argument : dynamic.bootstrapArgs()) {
                    if (argument instanceof DirectMethodHandleDesc handle) {
                        requireHandle(className, handle, refusals);
                    }
                }
            }
            default -> {
            }
        }
    }

    private void requireHandle(String className, DirectMethodHandleDesc handle, Set<Refusal> refusals) {
        String owner = MemberRef.unwrapDescriptor(handle.owner().descriptorString());
        switch (handle.kind()) {
            case GETTER, SETTER, STATIC_GETTER, STATIC_SETTER ->
                    requireField(className, owner, handle.methodName(), handle.lookupDescriptor(), refusals);
            default -> requireMethod(className, owner, handle.methodName(), handle.lookupDescriptor(), refusals);
        }
    }

    private void requireClass(String className, String type, Set<Refusal> refusals) {
        String element = elementOf(type);
        if (element != null && isInMod(element) && !this.classFiles.load(element).isPresent()) {
            refusals.add(new Refusal(className, element, Reason.MISSING_CLASS));
        }
    }

    private void requireMethod(String className, String owner, String name, String descriptor,
            Set<Refusal> refusals) {
        if (owner.startsWith(ARRAY_PREFIX)) {
            requireClass(className, owner, refusals);
            return;
        }

        if (!isInMod(owner)) {
            return;
        }

        if (!this.classFiles.load(owner).isPresent()) {
            refusals.add(new Refusal(className, owner, Reason.MISSING_CLASS));
            return;
        }

        boolean isConstructor = name.equals(MemberRef.CONSTRUCTOR);
        Predicate<ClassModel> declares = model -> hasDeclaredMethod(model, name, descriptor);
        boolean isFound = isConstructor ? isDeclaredBy(owner, declares) : isResolving(owner, declares,
                OBJECT_METHODS.contains(name + descriptor));
        if (!isFound) {
            refusals.add(new Refusal(className, owner + "#" + name + descriptor, Reason.MISSING_METHOD));
        }
    }

    private void requireField(String className, String owner, String name, String descriptor,
            Set<Refusal> refusals) {
        if (!isInMod(owner)) {
            return;
        }

        if (!this.classFiles.load(owner).isPresent()) {
            refusals.add(new Refusal(className, owner, Reason.MISSING_CLASS));
            return;
        }

        if (!isResolving(owner, model -> hasDeclaredField(model, name, descriptor), false)) {
            refusals.add(new Refusal(className, owner + "#" + name + ":" + descriptor, Reason.MISSING_FIELD));
        }
    }

    private boolean isDeclaredBy(String owner, Predicate<ClassModel> declares) {
        ClassModel model = this.classFiles.load(owner).model();
        return model == null || declares.test(model);
    }

    // A supertype outside the mod's packages is never loaded during mixin bootstrap, so what it may declare counts.
    private boolean isResolving(String owner, Predicate<ClassModel> declares, boolean isOnObject) {
        if (owner.equals(OBJECT)) {
            return isOnObject;
        }

        if (!isInMod(owner)) {
            return true;
        }

        Loaded loaded = this.classFiles.load(owner);
        if (!loaded.isPresent()) {
            return false;
        }

        ClassModel model = loaded.model();
        if (model == null || declares.test(model)) {
            return true;
        }

        if (model.superclass().isPresent()
                && isResolving(model.superclass().get().asInternalName(), declares, isOnObject)) {
            return true;
        }

        for (ClassEntry type : model.interfaces()) {
            if (isResolving(type.asInternalName(), declares, false)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasDeclaredMethod(ClassModel model, String name, String descriptor) {
        for (MethodModel method : model.methods()) {
            if (method.methodName().equalsString(name) && method.methodType().equalsString(descriptor)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasDeclaredField(ClassModel model, String name, String descriptor) {
        for (FieldModel field : model.fields()) {
            if (field.fieldName().equalsString(name) && field.fieldType().equalsString(descriptor)) {
                return true;
            }
        }

        return false;
    }

    private boolean isInMod(String internalName) {
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

    private static Set<String> readObjectMethods() {
        Set<String> methods = new LinkedHashSet<>();
        for (Method method : Object.class.getDeclaredMethods()) {
            methods.add(method.getName()
                    + MethodType.methodType(method.getReturnType(), method.getParameterTypes()).toMethodDescriptorString());
        }

        return Set.copyOf(methods);
    }
}

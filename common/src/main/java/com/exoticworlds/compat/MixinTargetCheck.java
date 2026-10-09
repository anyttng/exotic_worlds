package com.exoticworlds.compat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class MixinTargetCheck {
    public enum Reason {
        MISSING_MIXIN,
        MISSING_CLASS,
        MISSING_METHOD,
        MISSING_FIELD,
        MISSING_INVOKE,
        MISSING_FIELD_ACCESS,
        MISSING_NEW;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Refusal(String mixin, String member, Reason reason) {
    }

    private record Loaded(boolean present, ClassModel model) {
    }

    private record Selection(String owner, List<MethodModel> bodies) {
    }

    private static final String CLASS_SUFFIX = ".class";
    private static final List<String> GAME_PACKAGES = List.of("net/minecraft/", "com/mojang/");

    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String PSEUDO = "Lorg/spongepowered/asm/mixin/Pseudo;";
    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String ACCESSOR = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
    private static final String TARGET_HANDLER = "Lcom/bawnorton/mixinsquared/TargetHandler;";

    private static final String VALUE_KEY = "value";
    private static final String TARGETS_KEY = "targets";
    private static final String METHOD_KEY = "method";
    private static final String AT_KEY = "at";
    private static final String SLICE_KEY = "slice";
    private static final String SLICE_FROM_KEY = "from";
    private static final String SLICE_TO_KEY = "to";
    private static final String TARGET_KEY = "target";
    private static final String ORDINAL_KEY = "ordinal";
    private static final String OPCODE_KEY = "opcode";
    private static final String PREFIX_KEY = "prefix";
    private static final String ALIASES_KEY = "aliases";
    private static final String HANDLER_MIXIN_KEY = "mixin";
    private static final String HANDLER_NAME_KEY = "name";

    private static final String AT_INVOKE = "INVOKE";
    private static final String AT_FIELD = "FIELD";
    private static final String AT_NEW = "NEW";

    private static final String HANDLER_SELECTOR = "@MixinSquared:Handler";
    private static final String DEFAULT_SHADOW_PREFIX = "shadow$";
    private static final String CONSTRUCTOR = "<init>";
    private static final String VOID_RETURN = "V";
    private static final List<String> ACCESSOR_PREFIXES = List.of("get", "set", "is");
    private static final List<String> INVOKER_PREFIXES = List.of("call", "invoke");
    private static final int ANY_ORDINAL = -1;
    private static final int ANY_OPCODE = -1;

    private final ClassLoader classLoader;
    private final Map<String, String> bodySources;
    private final Map<String, Loaded> loaded = new HashMap<>();

    public MixinTargetCheck(ClassLoader classLoader, Map<String, String> bodySources) {
        this.classLoader = classLoader;
        this.bodySources = bodySources;
    }

    public List<Refusal> check(String mixinClassName) {
        Loaded own = load(internalName(mixinClassName));
        if (!own.present()) {
            return List.of(new Refusal(mixinClassName, internalName(mixinClassName), Reason.MISSING_MIXIN));
        }

        if (own.model() == null) {
            return List.of();
        }

        List<Annotation> classAnnotations = MixinAnnotations.of(own.model());
        Optional<Annotation> mixin = MixinAnnotations.find(classAnnotations, MIXIN);
        if (mixin.isEmpty()) {
            return List.of();
        }

        boolean pseudo = MixinAnnotations.find(classAnnotations, PSEUDO).isPresent();
        List<String> targets = targetsOf(mixin.get());
        List<String> present = new ArrayList<>();
        List<Refusal> refusals = new ArrayList<>();
        for (String target : targets) {
            if (isGame(target) || load(target).present()) {
                present.add(target);
            } else if (!pseudo) {
                refusals.add(new Refusal(mixinClassName, target, Reason.MISSING_CLASS));
            }
        }

        if (present.isEmpty() && !targets.isEmpty() && refusals.isEmpty()) {
            refusals.add(new Refusal(mixinClassName, targets.getFirst(), Reason.MISSING_CLASS));
        }

        for (String target : present) {
            checkMembers(mixinClassName, own.model(), target, refusals);
        }

        return refusals;
    }

    private void checkMembers(String mixinClassName, ClassModel mixin, String target, List<Refusal> refusals) {
        ClassModel targetModel = isGame(target) ? null : load(target).model();

        for (FieldModel field : mixin.fields()) {
            Optional<Annotation> shadow = MixinAnnotations.find(MixinAnnotations.of(field), SHADOW);
            if (shadow.isPresent() && targetModel != null && !hasField(targetModel,
                    shadowNames(shadow.get(), field.fieldName().stringValue(), false),
                    field.fieldType().stringValue())) {
                refusals.add(new Refusal(mixinClassName, target + "#" + field.fieldName().stringValue(),
                        Reason.MISSING_FIELD));
            }
        }

        for (MethodModel method : mixin.methods()) {
            List<Annotation> annotations = MixinAnnotations.of(method);
            String name = method.methodName().stringValue();
            String descriptor = method.methodType().stringValue();
            for (Annotation annotation : annotations) {
                switch (annotation.className().stringValue()) {
                    case SHADOW -> {
                        if (targetModel != null && !hasMethod(targetModel, shadowNames(annotation, name, true),
                                descriptor::equals)) {
                            refusals.add(new Refusal(mixinClassName, target + "#" + name + descriptor,
                                    Reason.MISSING_METHOD));
                        }
                    }
                    case ACCESSOR -> {
                        String field = generatedName(annotation, name, ACCESSOR_PREFIXES);
                        if (targetModel != null && !hasField(targetModel, decapitalizedPair(field), null)) {
                            refusals.add(new Refusal(mixinClassName, target + "#" + field, Reason.MISSING_FIELD));
                        }
                    }
                    case INVOKER -> {
                        String invoked = generatedName(annotation, name, INVOKER_PREFIXES);
                        String parameters = parametersOf(descriptor);
                        if (targetModel != null && !hasMethod(targetModel, decapitalizedPair(invoked),
                                candidate -> parametersOf(candidate).equals(parameters))) {
                            refusals.add(new Refusal(mixinClassName, target + "#" + invoked + parameters,
                                    Reason.MISSING_METHOD));
                        }
                    }
                    default -> {
                        if (MixinAnnotations.element(annotation, METHOD_KEY).isPresent()) {
                            checkInjector(mixinClassName, annotation, annotations, target, refusals);
                        }
                    }
                }
            }
        }
    }

    private void checkInjector(String mixinClassName, Annotation injector, List<Annotation> siblings, String target,
            List<Refusal> refusals) {
        List<Selection> selections = new ArrayList<>();
        for (String selector : MixinAnnotations.strings(injector, METHOD_KEY)) {
            if (selector.equals(HANDLER_SELECTOR)) {
                Optional<Annotation> handler = MixinAnnotations.find(siblings, TARGET_HANDLER);
                if (handler.isEmpty()) {
                    continue;
                }

                String bodyOwner = internalName(MixinAnnotations.string(handler.get(), HANDLER_MIXIN_KEY));
                String handlerName = MixinAnnotations.string(handler.get(), HANDLER_NAME_KEY);
                Loaded owner = load(bodyOwner);
                if (!owner.present()) {
                    refusals.add(new Refusal(mixinClassName, bodyOwner, Reason.MISSING_CLASS));
                    return;
                }

                if (owner.model() == null) {
                    return;
                }

                List<MethodModel> found = methods(owner.model(), handlerName::equals, any -> true);
                if (found.isEmpty()) {
                    refusals.add(new Refusal(mixinClassName, bodyOwner + "#" + handlerName, Reason.MISSING_METHOD));
                    return;
                }

                selections.add(new Selection(bodyOwner, found));
                continue;
            }

            String bodyOwner = bodyOwnerOf(mixinClassName, target);
            if (isGame(bodyOwner)) {
                return;
            }

            ClassModel owner = load(bodyOwner).model();
            if (owner == null) {
                return;
            }

            MemberRef selected = MemberRef.parse(selector);
            List<MethodModel> found = methods(owner, selected.name()::equals,
                    candidate -> selected.descriptor() == null || selected.descriptor().equals(candidate));
            if (found.isEmpty()) {
                refusals.add(new Refusal(mixinClassName, bodyOwner + "#" + selector, Reason.MISSING_METHOD));
                return;
            }

            selections.add(new Selection(bodyOwner, found));
        }

        List<Annotation> points = new ArrayList<>(MixinAnnotations.nested(injector, AT_KEY));
        for (Annotation slice : MixinAnnotations.nested(injector, SLICE_KEY)) {
            points.addAll(MixinAnnotations.nested(slice, SLICE_FROM_KEY));
            points.addAll(MixinAnnotations.nested(slice, SLICE_TO_KEY));
        }

        for (Selection selection : selections) {
            for (Annotation at : points) {
                checkAt(mixinClassName, selection, at, refusals);
            }
        }
    }

    private static void checkAt(String mixinClassName, Selection selection, Annotation at, List<Refusal> refusals) {
        String target = MixinAnnotations.element(at, TARGET_KEY).map(MixinAnnotations::asString).orElse("");
        int needed = Math.max(MixinAnnotations.integer(at, ORDINAL_KEY, ANY_ORDINAL), 0) + 1;
        Reason reason;
        ToIntFunction<MethodModel> counter;
        switch (MixinAnnotations.string(at, VALUE_KEY)) {
            case AT_INVOKE -> {
                reason = Reason.MISSING_INVOKE;
                MemberRef wanted = MemberRef.parse(target);
                counter = body -> countInvokes(body, wanted);
            }
            case AT_FIELD -> {
                reason = Reason.MISSING_FIELD_ACCESS;
                MemberRef wanted = MemberRef.parse(target);
                int opcode = MixinAnnotations.integer(at, OPCODE_KEY, ANY_OPCODE);
                counter = body -> countFieldAccesses(body, wanted, opcode);
            }
            case AT_NEW -> {
                reason = Reason.MISSING_NEW;
                counter = body -> countNews(body, target);
            }
            default -> {
                return;
            }
        }

        for (MethodModel body : selection.bodies()) {
            if (counter.applyAsInt(body) >= needed) {
                return;
            }
        }

        refusals.add(new Refusal(mixinClassName,
                selection.owner() + "#" + selection.bodies().getFirst().methodName().stringValue() + "@" + target,
                reason));
    }

    private String bodyOwnerOf(String mixinClassName, String target) {
        String substitute = this.bodySources.get(mixinClassName);
        return substitute != null && load(substitute).present() ? substitute : target;
    }

    private static int countInvokes(MethodModel body, MemberRef wanted) {
        int count = 0;
        for (CodeElement element : codeOf(body)) {
            if (element instanceof InvokeInstruction invoke
                    && wanted.matches(invoke.owner().asInternalName(), invoke.name().stringValue(),
                            invoke.type().stringValue())) {
                count++;
            }
        }

        return count;
    }

    private static int countFieldAccesses(MethodModel body, MemberRef wanted, int opcode) {
        int count = 0;
        for (CodeElement element : codeOf(body)) {
            if (element instanceof FieldInstruction access
                    && (opcode == ANY_OPCODE || access.opcode().bytecode() == opcode)
                    && wanted.matches(access.owner().asInternalName(), access.name().stringValue(),
                            access.type().stringValue())) {
                count++;
            }
        }

        return count;
    }

    private static int countNews(MethodModel body, String target) {
        boolean byConstructor = target.startsWith("(");
        String created = byConstructor ? unwrapDescriptor(target.substring(target.indexOf(')') + 1))
                : unwrapDescriptor(target);
        String constructor = byConstructor ? parametersOf(target) + VOID_RETURN : null;
        int count = 0;
        for (CodeElement element : codeOf(body)) {
            if (byConstructor && element instanceof InvokeInstruction invoke
                    && invoke.opcode() == Opcode.INVOKESPECIAL
                    && invoke.owner().asInternalName().equals(created)
                    && invoke.name().equalsString(CONSTRUCTOR)
                    && invoke.type().equalsString(constructor)) {
                count++;
            } else if (!byConstructor && element instanceof NewObjectInstruction creation
                    && creation.className().asInternalName().equals(created)) {
                count++;
            }
        }

        return count;
    }

    private static List<MethodModel> methods(ClassModel owner, Predicate<String> name, Predicate<String> descriptor) {
        List<MethodModel> found = new ArrayList<>();
        for (MethodModel method : owner.methods()) {
            if (name.test(method.methodName().stringValue()) && descriptor.test(method.methodType().stringValue())) {
                found.add(method);
            }
        }

        return found;
    }

    private static boolean hasMethod(ClassModel owner, List<String> names, Predicate<String> descriptor) {
        return !methods(owner, names::contains, descriptor).isEmpty();
    }

    private static boolean hasField(ClassModel owner, List<String> names, String descriptor) {
        for (FieldModel field : owner.fields()) {
            if (names.contains(field.fieldName().stringValue())
                    && (descriptor == null || field.fieldType().equalsString(descriptor))) {
                return true;
            }
        }

        return false;
    }

    private static List<String> shadowNames(Annotation shadow, String name, boolean method) {
        List<String> names = new ArrayList<>();
        String prefix = MixinAnnotations.element(shadow, PREFIX_KEY).map(MixinAnnotations::asString)
                .orElse(DEFAULT_SHADOW_PREFIX);
        names.add(method && name.startsWith(prefix) ? name.substring(prefix.length()) : name);
        names.addAll(MixinAnnotations.strings(shadow, ALIASES_KEY));
        return names;
    }

    private static String generatedName(Annotation annotation, String methodName, List<String> prefixes) {
        String explicit = MixinAnnotations.element(annotation, VALUE_KEY).map(MixinAnnotations::asString).orElse("");
        if (!explicit.isEmpty()) {
            return explicit;
        }

        for (String prefix : prefixes) {
            if (methodName.startsWith(prefix) && methodName.length() > prefix.length()) {
                return methodName.substring(prefix.length());
            }
        }

        return methodName;
    }

    private static List<String> decapitalizedPair(String name) {
        if (name.isEmpty() || name.equals(CONSTRUCTOR)) {
            return List.of(name);
        }

        return List.of(name, Character.toLowerCase(name.charAt(0)) + name.substring(1));
    }

    private static String parametersOf(String descriptor) {
        return descriptor.substring(0, descriptor.indexOf(')') + 1);
    }

    private static String unwrapDescriptor(String type) {
        return type.startsWith("L") && type.endsWith(";") ? type.substring(1, type.length() - 1) : type;
    }

    private static boolean isGame(String internalName) {
        for (String gamePackage : GAME_PACKAGES) {
            if (internalName.startsWith(gamePackage)) {
                return true;
            }
        }

        return false;
    }

    private static String internalName(String className) {
        return className.replace('.', '/');
    }

    private static List<String> targetsOf(Annotation mixin) {
        List<String> targets = new ArrayList<>();
        for (AnnotationValue value : MixinAnnotations.values(mixin, VALUE_KEY)) {
            if (value instanceof AnnotationValue.OfClass type) {
                targets.add(unwrapDescriptor(type.classSymbol().descriptorString()));
            }
        }

        for (String target : MixinAnnotations.strings(mixin, TARGETS_KEY)) {
            targets.add(internalName(target));
        }

        return targets;
    }

    private static Iterable<CodeElement> codeOf(MethodModel method) {
        return method.code().<Iterable<CodeElement>>map(code -> code).orElse(List.of());
    }

    private Loaded load(String internalName) {
        return this.loaded.computeIfAbsent(internalName, this::read);
    }

    private Loaded read(String internalName) {
        try (InputStream classFile = this.classLoader.getResourceAsStream(internalName + CLASS_SUFFIX)) {
            if (classFile == null) {
                return new Loaded(false, null);
            }

            return new Loaded(true, ClassFile.of().parse(classFile.readAllBytes()));
        } catch (IOException | RuntimeException unreadable) {
            return new Loaded(true, null);
        }
    }
}

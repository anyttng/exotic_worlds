package com.exoticworlds.compat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.exoticworlds.compat.InjectionPoints.Selection;

import org.jspecify.annotations.Nullable;

public final class MixinTargetCheck {
    public enum Reason {
        MISSING_MIXIN,
        MISSING_CLASS,
        MISSING_METHOD,
        MISSING_FIELD,
        MISSING_INVOKE,
        MISSING_FIELD_ACCESS,
        MISSING_NEW,
        WRONG_CALLBACK;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Refusal(String mixin, String member, Reason reason) {
    }

    private record Loaded(boolean present, @Nullable ClassModel model) {
    }

    private static final String CLASS_SUFFIX = ".class";
    private static final List<String> GAME_PACKAGES = List.of("net/minecraft/", "com/mojang/");

    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String PSEUDO = "Lorg/spongepowered/asm/mixin/Pseudo;";
    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String ACCESSOR = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
    private static final String TARGET_HANDLER = "Lcom/bawnorton/mixinsquared/TargetHandler;";
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String CALLBACK_INFO = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;";
    private static final String CALLBACK_INFO_RETURNABLE =
            "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;";

    private static final String TARGETS_KEY = "targets";
    private static final String METHOD_KEY = "method";
    private static final String AT_KEY = "at";
    private static final String SLICE_KEY = "slice";
    private static final String SLICE_FROM_KEY = "from";
    private static final String SLICE_TO_KEY = "to";
    private static final String PREFIX_KEY = "prefix";
    private static final String ALIASES_KEY = "aliases";
    private static final String HANDLER_MIXIN_KEY = "mixin";
    private static final String HANDLER_NAME_KEY = "name";

    private static final String HANDLER_SELECTOR = "@MixinSquared:Handler";
    private static final String DEFAULT_SHADOW_PREFIX = "shadow$";
    private static final List<String> ACCESSOR_PREFIXES = List.of("get", "set", "is");
    private static final List<String> INVOKER_PREFIXES = List.of("call", "invoke");

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
        if (targetModel != null) {
            checkShadowFields(mixinClassName, mixin, target, targetModel, refusals);
        }

        for (MethodModel method : mixin.methods()) {
            List<Annotation> annotations = MixinAnnotations.of(method);
            String name = method.methodName().stringValue();
            String descriptor = method.methodType().stringValue();
            for (Annotation annotation : annotations) {
                switch (annotation.className().stringValue()) {
                    case SHADOW, ACCESSOR, INVOKER -> {
                        Refusal refusal = targetModel == null ? null
                                : checkGenerated(mixinClassName, annotation, name, descriptor, target, targetModel);
                        if (refusal != null) {
                            refusals.add(refusal);
                        }
                    }
                    default -> {
                        if (MixinAnnotations.element(annotation, METHOD_KEY).isPresent()) {
                            checkInjector(mixinClassName, annotation, annotations, descriptor, target, refusals);
                        }
                    }
                }
            }
        }
    }

    private static void checkShadowFields(String mixinClassName, ClassModel mixin, String target,
            ClassModel targetModel, List<Refusal> refusals) {
        for (FieldModel field : mixin.fields()) {
            Optional<Annotation> shadow = MixinAnnotations.find(MixinAnnotations.of(field), SHADOW);
            if (shadow.isPresent() && !hasField(targetModel,
                    shadowNames(shadow.get(), field.fieldName().stringValue(), false),
                    field.fieldType().stringValue())) {
                refusals.add(new Refusal(mixinClassName, target + "#" + field.fieldName().stringValue(),
                        Reason.MISSING_FIELD));
            }
        }
    }

    private static @Nullable Refusal checkGenerated(String mixinClassName, Annotation annotation, String name,
            String descriptor, String target, ClassModel targetModel) {
        return switch (annotation.className().stringValue()) {
            case SHADOW -> hasMethod(targetModel, shadowNames(annotation, name, true), descriptor::equals) ? null
                    : new Refusal(mixinClassName, target + "#" + name + descriptor, Reason.MISSING_METHOD);
            case ACCESSOR -> {
                String field = generatedName(annotation, name, ACCESSOR_PREFIXES);
                yield hasField(targetModel, decapitalizedPair(field), null) ? null
                        : new Refusal(mixinClassName, target + "#" + field, Reason.MISSING_FIELD);
            }
            default -> {
                String invoked = generatedName(annotation, name, INVOKER_PREFIXES);
                String parameters = MemberRef.parametersOf(descriptor);
                yield hasMethod(targetModel, decapitalizedPair(invoked),
                        candidate -> MemberRef.parametersOf(candidate).equals(parameters)) ? null
                        : new Refusal(mixinClassName, target + "#" + invoked + parameters, Reason.MISSING_METHOD);
            }
        };
    }

    private void checkInjector(String mixinClassName, Annotation injector, List<Annotation> siblings,
            String handlerDescriptor, String target, List<Refusal> refusals) {
        List<Selection> selections = selections(mixinClassName, injector, siblings, target, refusals);
        if (selections == null) {
            return;
        }

        if (injector.className().equalsString(INJECT)) {
            checkCallback(mixinClassName, handlerDescriptor, selections, refusals);
        }

        List<Annotation> points = points(injector);
        for (Selection selection : selections) {
            for (Annotation at : points) {
                Refusal refusal = InjectionPoints.check(mixinClassName, selection, at);
                if (refusal != null) {
                    refusals.add(refusal);
                }
            }
        }
    }

    private @Nullable List<Selection> selections(String mixinClassName, Annotation injector,
            List<Annotation> siblings, String target, List<Refusal> refusals) {
        List<Selection> selections = new ArrayList<>();
        for (String selector : MixinAnnotations.strings(injector, METHOD_KEY)) {
            Selection selection;
            if (selector.equals(HANDLER_SELECTOR)) {
                Optional<Annotation> handler = MixinAnnotations.find(siblings, TARGET_HANDLER);
                if (handler.isEmpty()) {
                    continue;
                }

                selection = handlerSelection(mixinClassName, handler.get(), refusals);
            } else {
                selection = plainSelection(mixinClassName, selector, target, refusals);
            }

            if (selection == null) {
                return null;
            }

            selections.add(selection);
        }

        return selections;
    }

    private @Nullable Selection handlerSelection(String mixinClassName, Annotation handler, List<Refusal> refusals) {
        String bodyOwner = internalName(MixinAnnotations.string(handler, HANDLER_MIXIN_KEY));
        String handlerName = MixinAnnotations.string(handler, HANDLER_NAME_KEY);
        Loaded owner = load(bodyOwner);
        if (!owner.present()) {
            refusals.add(new Refusal(mixinClassName, bodyOwner, Reason.MISSING_CLASS));
            return null;
        }

        if (owner.model() == null) {
            return null;
        }

        List<MethodModel> found = methods(owner.model(), handlerName::equals, any -> true);
        if (found.isEmpty()) {
            refusals.add(new Refusal(mixinClassName, bodyOwner + "#" + handlerName, Reason.MISSING_METHOD));
            return null;
        }

        List<String> selfTargets = MixinAnnotations.find(MixinAnnotations.of(owner.model()), MIXIN)
                .map(MixinTargetCheck::targetsOf)
                .orElse(List.of());
        return new Selection(bodyOwner, found, selfTargets);
    }

    private @Nullable Selection plainSelection(String mixinClassName, String selector, String target,
            List<Refusal> refusals) {
        String bodyOwner = bodyOwnerOf(mixinClassName, target);
        if (isGame(bodyOwner)) {
            return null;
        }

        ClassModel owner = load(bodyOwner).model();
        if (owner == null) {
            return null;
        }

        MemberRef selected = MemberRef.parse(selector);
        List<MethodModel> found = methods(owner,
                name -> selected.name().isEmpty() || selected.name().equals(name),
                candidate -> selected.descriptor() == null || selected.descriptor().equals(candidate));
        if (found.isEmpty()) {
            refusals.add(new Refusal(mixinClassName, bodyOwner + "#" + selector, Reason.MISSING_METHOD));
            return null;
        }

        return new Selection(bodyOwner, found, List.of());
    }

    private static List<Annotation> points(Annotation injector) {
        List<Annotation> points = new ArrayList<>(MixinAnnotations.nested(injector, AT_KEY));
        for (Annotation slice : MixinAnnotations.nested(injector, SLICE_KEY)) {
            points.addAll(MixinAnnotations.nested(slice, SLICE_FROM_KEY));
            points.addAll(MixinAnnotations.nested(slice, SLICE_TO_KEY));
        }

        return points;
    }

    private static void checkCallback(String mixinClassName, String handlerDescriptor, List<Selection> selections,
            List<Refusal> refusals) {
        String parameters = MemberRef.parametersOf(handlerDescriptor);
        for (Selection selection : selections) {
            for (MethodModel body : selection.bodies()) {
                boolean returns = !body.methodTypeSymbol().returnType().descriptorString()
                        .equals(MemberRef.VOID_RETURN);
                if (!parameters.contains(returns ? CALLBACK_INFO_RETURNABLE : CALLBACK_INFO)) {
                    String member = selection.owner() + "#" + body.methodName().stringValue()
                            + body.methodType().stringValue();
                    refusals.add(new Refusal(mixinClassName, member, Reason.WRONG_CALLBACK));
                    return;
                }
            }
        }
    }

    private String bodyOwnerOf(String mixinClassName, String target) {
        String substitute = this.bodySources.get(mixinClassName);
        return substitute != null && load(substitute).present() ? substitute : target;
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

    private static boolean hasField(ClassModel owner, List<String> names, @Nullable String descriptor) {
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
        String explicit = MixinAnnotations.element(annotation, MixinAnnotations.VALUE_KEY).map(MixinAnnotations::asString).orElse("");
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
        if (name.isEmpty() || name.equals(MemberRef.CONSTRUCTOR)) {
            return List.of(name);
        }

        return List.of(name, Character.toLowerCase(name.charAt(0)) + name.substring(1));
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
        for (AnnotationValue value : MixinAnnotations.values(mixin, MixinAnnotations.VALUE_KEY)) {
            if (value instanceof AnnotationValue.OfClass type) {
                targets.add(MemberRef.unwrapDescriptor(type.classSymbol().descriptorString()));
            }
        }

        for (String target : MixinAnnotations.strings(mixin, TARGETS_KEY)) {
            targets.add(internalName(target));
        }

        return targets;
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

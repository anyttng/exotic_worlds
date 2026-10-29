package com.exoticworlds.compat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import com.exoticworlds.compat.InjectionPoints.Selection;

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

    private record Loaded(boolean present, @Nullable ClassNode model) {
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
    private LoaderSpellings.@Nullable Namespace runtime;

    public MixinTargetCheck(ClassLoader classLoader, Map<String, String> bodySources) {
        this.classLoader = classLoader;
        this.bodySources = bodySources;
    }

    public List<Refusal> check(String mixinClassName) {
        Loaded own = load(internalName(mixinClassName));
        if (!own.present()) {
            return List.of(new Refusal(mixinClassName, internalName(mixinClassName), Reason.MISSING_MIXIN));
        }

        ClassNode ownModel = own.model();
        if (ownModel == null) {
            return List.of();
        }

        List<AnnotationNode> classAnnotations = MixinAnnotations.of(ownModel);
        Optional<AnnotationNode> mixin = MixinAnnotations.find(classAnnotations, MIXIN);
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
            refusals.add(new Refusal(mixinClassName, targets.get(0), Reason.MISSING_CLASS));
        }

        for (String target : present) {
            if (!isOtherLoaders(target)) {
                checkMembers(mixinClassName, ownModel, target, refusals);
            }
        }

        return refusals;
    }

    private void checkMembers(String mixinClassName, ClassNode mixin, String target, List<Refusal> refusals) {
        ClassNode targetModel = isGame(target) ? null : load(target).model();
        if (targetModel != null) {
            checkShadowFields(mixinClassName, mixin, target, targetModel, refusals);
        }

        for (MethodNode method : mixin.methods) {
            List<AnnotationNode> annotations = MixinAnnotations.of(method);
            String name = method.name;
            String descriptor = method.desc;
            for (AnnotationNode annotation : annotations) {
                switch (annotation.desc) {
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

    private static void checkShadowFields(String mixinClassName, ClassNode mixin, String target,
            ClassNode targetModel, List<Refusal> refusals) {
        for (FieldNode field : mixin.fields) {
            Optional<AnnotationNode> shadow = MixinAnnotations.find(MixinAnnotations.of(field),
                    SHADOW);
            if (shadow.isPresent()
                    && !hasField(targetModel, shadowNames(shadow.get(), field.name, false), field.desc)) {
                refusals.add(new Refusal(mixinClassName, target + "#" + field.name, Reason.MISSING_FIELD));
            }
        }
    }

    private static @Nullable Refusal checkGenerated(String mixinClassName, AnnotationNode annotation, String name,
            String descriptor, String target, ClassNode targetModel) {
        return switch (annotation.desc) {
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

    private void checkInjector(String mixinClassName, AnnotationNode injector, List<AnnotationNode> siblings,
            String handlerDescriptor, String target, List<Refusal> refusals) {
        List<Selection> selections = selections(mixinClassName, injector, siblings, target, refusals);
        if (selections == null) {
            return;
        }

        if (injector.desc.equals(INJECT)) {
            checkCallback(mixinClassName, handlerDescriptor, selections, refusals);
        }

        List<AnnotationNode> points = points(injector);
        for (Selection selection : selections) {
            for (AnnotationNode at : points) {
                Refusal refusal = InjectionPoints.check(mixinClassName, selection, at);
                if (refusal != null) {
                    refusals.add(refusal);
                }
            }
        }
    }

    private @Nullable List<Selection> selections(String mixinClassName, AnnotationNode injector,
            List<AnnotationNode> siblings, String target, List<Refusal> refusals) {
        List<Selection> selections = new ArrayList<>();
        List<String> selectors = MixinAnnotations.strings(injector, METHOD_KEY);
        for (String selector : selectors) {
            Selection selection;
            if (selector.equals(HANDLER_SELECTOR)) {
                Optional<AnnotationNode> handler = MixinAnnotations.find(siblings, TARGET_HANDLER);
                if (handler.isEmpty()) {
                    continue;
                }

                selection = handlerSelection(mixinClassName, handler.get(), refusals);
            } else if (twinSelected(mixinClassName, selector, selectors, target)) {
                continue;
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

    private @Nullable Selection handlerSelection(String mixinClassName, AnnotationNode handler,
            List<Refusal> refusals) {
        String bodyOwner = internalName(MixinAnnotations.string(handler, HANDLER_MIXIN_KEY));
        String handlerName = MixinAnnotations.string(handler, HANDLER_NAME_KEY);
        Loaded owner = load(bodyOwner);
        if (!owner.present()) {
            refusals.add(new Refusal(mixinClassName, bodyOwner, Reason.MISSING_CLASS));
            return null;
        }

        ClassNode ownerModel = owner.model();
        if (ownerModel == null) {
            return null;
        }

        List<MethodNode> found = methods(ownerModel, handlerName::equals, any -> true);
        if (found.isEmpty()) {
            refusals.add(new Refusal(mixinClassName, bodyOwner + "#" + handlerName, Reason.MISSING_METHOD));
            return null;
        }

        List<String> selfTargets = MixinAnnotations.find(MixinAnnotations.of(ownerModel), MIXIN)
                .map(MixinTargetCheck::targetsOf)
                .orElse(List.of());
        return new Selection(bodyOwner, found, selfTargets);
    }

    private boolean twinSelected(String mixinClassName, String selector, List<String> selectors, String target) {
        String bodyOwner = bodyOwnerOf(mixinClassName, target);
        ClassNode owner = isGame(bodyOwner) ? null : load(bodyOwner).model();
        return owner != null && selected(owner, selector).isEmpty()
                && LoaderSpellings.twinSelected(selector, selectors,
                        other -> !other.equals(HANDLER_SELECTOR) && !selected(owner, other).isEmpty());
    }

    private @Nullable Selection plainSelection(String mixinClassName, String selector, String target,
            List<Refusal> refusals) {
        String bodyOwner = bodyOwnerOf(mixinClassName, target);
        if (isGame(bodyOwner)) {
            return null;
        }

        ClassNode owner = load(bodyOwner).model();
        if (owner == null) {
            return null;
        }

        List<MethodNode> found = selected(owner, selector);
        if (found.isEmpty()) {
            refusals.add(new Refusal(mixinClassName, bodyOwner + "#" + selector, Reason.MISSING_METHOD));
            return null;
        }

        return new Selection(bodyOwner, found, List.of());
    }

    private static List<AnnotationNode> points(AnnotationNode injector) {
        List<AnnotationNode> points = new ArrayList<>(MixinAnnotations.nested(injector, AT_KEY));
        for (AnnotationNode slice : MixinAnnotations.nested(injector, SLICE_KEY)) {
            points.addAll(MixinAnnotations.nested(slice, SLICE_FROM_KEY));
            points.addAll(MixinAnnotations.nested(slice, SLICE_TO_KEY));
        }

        return points;
    }

    private static List<MethodNode> selected(ClassNode owner, String selector) {
        MemberRef selected = MemberRef.parse(selector);
        return methods(owner, name -> selected.name().isEmpty() || selected.name().equals(name),
                candidate -> selected.descriptor() == null || selected.descriptor().equals(candidate));
    }

    private static void checkCallback(String mixinClassName, String handlerDescriptor, List<Selection> selections,
            List<Refusal> refusals) {
        String parameters = MemberRef.parametersOf(handlerDescriptor);
        for (Selection selection : selections) {
            for (MethodNode body : selection.bodies()) {
                boolean returns = !Type.getReturnType(body.desc).getDescriptor().equals(MemberRef.VOID_RETURN);
                if (!parameters.contains(returns ? CALLBACK_INFO_RETURNABLE : CALLBACK_INFO)) {
                    String member = selection.owner() + "#" + body.name + body.desc;
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

    private boolean isOtherLoaders(String target) {
        ClassNode model = isGame(target) ? null : load(target).model();
        if (model == null) {
            return false;
        }

        LoaderSpellings.Namespace spelled = LoaderSpellings.namespaceOf(model);
        return spelled != LoaderSpellings.Namespace.UNKNOWN && runtime() != LoaderSpellings.Namespace.UNKNOWN
                && spelled != runtime();
    }

    private LoaderSpellings.Namespace runtime() {
        if (this.runtime == null) {
            this.runtime = LoaderSpellings.runtimeOf(this.classLoader);
        }

        return this.runtime;
    }

    private static List<MethodNode> methods(ClassNode owner, Predicate<String> name, Predicate<String> descriptor) {
        List<MethodNode> found = new ArrayList<>();
        for (MethodNode method : owner.methods) {
            if (name.test(method.name) && descriptor.test(method.desc)) {
                found.add(method);
            }
        }

        return found;
    }

    private static boolean hasMethod(ClassNode owner, List<String> names, Predicate<String> descriptor) {
        return !methods(owner, names::contains, descriptor).isEmpty();
    }

    private static boolean hasField(ClassNode owner, List<String> names, @Nullable String descriptor) {
        for (FieldNode field : owner.fields) {
            if (names.contains(field.name) && (descriptor == null || field.desc.equals(descriptor))) {
                return true;
            }
        }

        return false;
    }

    private static List<String> shadowNames(AnnotationNode shadow, String name, boolean method) {
        List<String> names = new ArrayList<>();
        String prefix = MixinAnnotations.element(shadow, PREFIX_KEY).map(MixinAnnotations::asString).orElse(DEFAULT_SHADOW_PREFIX);
        names.add(method && name.startsWith(prefix) ? name.substring(prefix.length()) : name);
        names.addAll(MixinAnnotations.strings(shadow, ALIASES_KEY));
        return names;
    }

    private static String generatedName(AnnotationNode annotation, String methodName, List<String> prefixes) {
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

    private static List<String> targetsOf(AnnotationNode mixin) {
        List<String> targets = new ArrayList<>();
        for (Object value : MixinAnnotations.values(mixin, MixinAnnotations.VALUE_KEY)) {
            if (value instanceof Type type) {
                targets.add(type.getInternalName());
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

            ClassNode model = new ClassNode();
            new ClassReader(classFile.readAllBytes()).accept(model, ClassReader.SKIP_FRAMES);
            return new Loaded(true, model);
        } catch (IOException | RuntimeException unreadable) {
            return new Loaded(true, null);
        }
    }
}

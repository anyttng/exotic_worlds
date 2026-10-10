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
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

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

    private record Loaded(boolean present, @Nullable ClassNode model) {
    }

    private record MemberRef(@Nullable String owner, String name, @Nullable String descriptor) {
    }

    private static final String CLASS_SUFFIX = ".class";
    private static final List<String> GAME_PACKAGES = List.of("net/minecraft/", "com/mojang/");
    private static final Pattern INTERMEDIARY_METHOD = Pattern.compile("method_\\d+");
    private static final Pattern INTERMEDIARY_CLASS = Pattern.compile("net/minecraft/class_\\d+");
    private static final String GAME_PACKAGE = "net/minecraft/";
    private static final String INTERMEDIARY_LEVEL = "net/minecraft/class_1937.class";
    private static final String MOJMAP_LEVEL = "net/minecraft/world/level/Level.class";

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
    private static final String ANY_QUANTIFIER = "*";
    private static final String PLUS_QUANTIFIER = "+";
    private static final char QUANTIFIER_OPEN = '{';
    private static final String DEFAULT_SHADOW_PREFIX = "shadow$";
    private static final String CONSTRUCTOR = "<init>";
    private static final String VOID_RETURN = "V";
    private static final List<String> ACCESSOR_PREFIXES = List.of("get", "set", "is");
    private static final List<String> INVOKER_PREFIXES = List.of("call", "invoke");
    private static final int ANY_ORDINAL = -1;
    private static final int ANY_OPCODE = -1;
    private static final int NAME_VALUE_PAIR = 2;

    private final ClassLoader classLoader;
    private final Map<String, String> bodySources;
    private final Map<String, Loaded> loaded = new HashMap<>();
    private @Nullable Namespace runtime;

    private enum Namespace {
        MOJMAP,
        INTERMEDIARY,
        UNKNOWN
    }

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

        List<AnnotationNode> classAnnotations = annotationsOf(ownModel.visibleAnnotations,
                ownModel.invisibleAnnotations);
        Optional<AnnotationNode> mixin = find(classAnnotations, MIXIN);
        if (mixin.isEmpty()) {
            return List.of();
        }

        boolean pseudo = find(classAnnotations, PSEUDO).isPresent();
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

        for (FieldNode field : mixin.fields) {
            Optional<AnnotationNode> shadow = find(annotationsOf(field.visibleAnnotations, field.invisibleAnnotations),
                    SHADOW);
            if (shadow.isPresent() && targetModel != null
                    && !hasField(targetModel, shadowNames(shadow.get(), field.name, false), field.desc)) {
                refusals.add(new Refusal(mixinClassName, target + "#" + field.name, Reason.MISSING_FIELD));
            }
        }

        for (MethodNode method : mixin.methods) {
            List<AnnotationNode> annotations = annotationsOf(method.visibleAnnotations, method.invisibleAnnotations);
            String name = method.name;
            String descriptor = method.desc;
            for (AnnotationNode annotation : annotations) {
                switch (annotation.desc) {
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
                        if (element(annotation, METHOD_KEY).isPresent()) {
                            checkInjector(mixinClassName, annotation, annotations, target, refusals);
                        }
                    }
                }
            }
        }
    }

    private void checkInjector(String mixinClassName, AnnotationNode injector, List<AnnotationNode> siblings,
            String target, List<Refusal> refusals) {
        List<MethodNode> bodies = new ArrayList<>();
        String bodyOwner = null;
        List<String> selfTargets = List.of();
        List<String> selectors = strings(injector, METHOD_KEY);
        for (String selector : selectors) {
            if (selector.equals(HANDLER_SELECTOR)) {
                Optional<AnnotationNode> handler = find(siblings, TARGET_HANDLER);
                if (handler.isEmpty()) {
                    continue;
                }

                bodyOwner = internalName(string(handler.get(), HANDLER_MIXIN_KEY));
                String handlerName = string(handler.get(), HANDLER_NAME_KEY);
                Loaded owner = load(bodyOwner);
                if (!owner.present()) {
                    refusals.add(new Refusal(mixinClassName, bodyOwner, Reason.MISSING_CLASS));
                    return;
                }

                ClassNode ownerModel = owner.model();
                if (ownerModel == null) {
                    return;
                }

                List<MethodNode> found = methods(ownerModel, handlerName::equals, any -> true);
                if (found.isEmpty()) {
                    refusals.add(new Refusal(mixinClassName, bodyOwner + "#" + handlerName, Reason.MISSING_METHOD));
                    return;
                }

                bodies.addAll(found);
                selfTargets = find(annotationsOf(ownerModel.visibleAnnotations, ownerModel.invisibleAnnotations), MIXIN)
                        .map(MixinTargetCheck::targetsOf)
                        .orElse(List.of());
                continue;
            }

            bodyOwner = bodyOwnerOf(mixinClassName, target);
            if (isGame(bodyOwner)) {
                return;
            }

            ClassNode owner = load(bodyOwner).model();
            if (owner == null) {
                return;
            }

            List<MethodNode> found = selected(owner, selector);
            if (found.isEmpty()) {
                if (twinSelected(owner, selector, selectors)) {
                    continue;
                }

                refusals.add(new Refusal(mixinClassName, bodyOwner + "#" + selector, Reason.MISSING_METHOD));
                return;
            }

            bodies.addAll(found);
        }

        if (bodies.isEmpty()) {
            return;
        }

        for (AnnotationNode at : nested(injector, AT_KEY)) {
            checkAt(mixinClassName, bodyOwner, selfTargets, at, bodies, refusals);
        }
    }

    private static List<MethodNode> selected(ClassNode owner, String selector) {
        MemberRef selected = parse(selector);
        return methods(owner, name -> selected.name().isEmpty() || selected.name().equals(name),
                candidate -> selected.descriptor() == null || selected.descriptor().equals(candidate));
    }

    // 1.21 names a method twice where its loaders spell it apart (Mojmap and intermediary): one of the pair suffices.
    private static boolean twinSelected(ClassNode owner, String selector, List<String> selectors) {
        boolean intermediary = isIntermediary(selector);
        int parameters = parameterCount(selector);
        for (String other : selectors) {
            if (!other.equals(selector) && !other.equals(HANDLER_SELECTOR) && isIntermediary(other) != intermediary
                    && (parameters < 0 || parameterCount(other) < 0 || parameterCount(other) == parameters)
                    && !selected(owner, other).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private static boolean isIntermediary(String selector) {
        MemberRef selected = parse(selector);
        return INTERMEDIARY_METHOD.matcher(selected.name()).matches()
                || selected.descriptor() != null && INTERMEDIARY_CLASS.matcher(selected.descriptor()).find();
    }

    private static int parameterCount(String selector) {
        String descriptor = parse(selector).descriptor();
        return descriptor == null || !descriptor.startsWith("(") ? -1 : Type.getArgumentTypes(descriptor).length;
    }

    private static void checkAt(String mixinClassName, @Nullable String bodyOwner, List<String> selfTargets,
            AnnotationNode at, List<MethodNode> bodies, List<Refusal> refusals) {
        String target = element(at, TARGET_KEY).map(MixinTargetCheck::asString).orElse("");
        int needed = Math.max(integer(at, ORDINAL_KEY, ANY_ORDINAL), 0) + 1;
        Reason reason;
        int found;
        switch (string(at, VALUE_KEY)) {
            case AT_INVOKE -> {
                reason = Reason.MISSING_INVOKE;
                found = countInvokes(bodies, parse(target), bodyOwner, selfTargets);
            }
            case AT_FIELD -> {
                reason = Reason.MISSING_FIELD_ACCESS;
                found = countFieldAccesses(bodies, parse(target), integer(at, OPCODE_KEY, ANY_OPCODE), bodyOwner,
                        selfTargets);
            }
            case AT_NEW -> {
                reason = Reason.MISSING_NEW;
                found = countNews(bodies, target);
            }
            default -> {
                return;
            }
        }

        if (found < needed) {
            refusals.add(new Refusal(mixinClassName, bodyOwner + "#" + bodies.get(0).name + "@" + target, reason));
        }
    }

    private String bodyOwnerOf(String mixinClassName, String target) {
        String substitute = this.bodySources.get(mixinClassName);
        return substitute != null && load(substitute).present() ? substitute : target;
    }

    private static int countInvokes(List<MethodNode> bodies, MemberRef wanted, @Nullable String bodyOwner,
            List<String> selfTargets) {
        int count = 0;
        for (MethodNode body : bodies) {
            for (AbstractInsnNode element : body.instructions) {
                if (element instanceof MethodInsnNode invoke && matches(wanted,
                        merged(invoke.owner, bodyOwner, selfTargets, wanted), invoke.name, invoke.desc)) {
                    count++;
                }
            }
        }

        return count;
    }

    private static int countFieldAccesses(List<MethodNode> bodies, MemberRef wanted, int opcode,
            @Nullable String bodyOwner, List<String> selfTargets) {
        int count = 0;
        for (MethodNode body : bodies) {
            for (AbstractInsnNode element : body.instructions) {
                if (element instanceof FieldInsnNode access
                        && (opcode == ANY_OPCODE || access.getOpcode() == opcode)
                        && matches(wanted, merged(access.owner, bodyOwner, selfTargets, wanted), access.name,
                                access.desc)) {
                    count++;
                }
            }
        }

        return count;
    }

    private static int countNews(List<MethodNode> bodies, String target) {
        boolean byConstructor = target.startsWith("(");
        String created = byConstructor ? unwrapDescriptor(target.substring(target.indexOf(')') + 1))
                : unwrapDescriptor(target);
        String constructor = byConstructor ? parametersOf(target) + VOID_RETURN : null;
        int count = 0;
        for (MethodNode body : bodies) {
            for (AbstractInsnNode element : body.instructions) {
                if (byConstructor && element instanceof MethodInsnNode invoke
                        && invoke.getOpcode() == Opcodes.INVOKESPECIAL
                        && invoke.owner.equals(created)
                        && invoke.name.equals(CONSTRUCTOR)
                        && invoke.desc.equals(constructor)) {
                    count++;
                } else if (!byConstructor && element instanceof TypeInsnNode creation
                        && creation.getOpcode() == Opcodes.NEW
                        && creation.desc.equals(created)) {
                    count++;
                }
            }
        }

        return count;
    }

    // A foreign mixin's call to its own shadow names the mixin as owner until Mixin merges it into its target.
    private static String merged(String owner, @Nullable String bodyOwner, List<String> selfTargets,
            MemberRef wanted) {
        return owner.equals(bodyOwner) && wanted.owner() != null && selfTargets.contains(wanted.owner())
                ? wanted.owner()
                : owner;
    }

    private boolean isOtherLoaders(String target) {
        ClassNode model = isGame(target) ? null : load(target).model();
        if (model == null) {
            return false;
        }

        Namespace spelled = namespaceOf(model);
        return spelled != Namespace.UNKNOWN && runtime() != Namespace.UNKNOWN && spelled != runtime();
    }

    private Namespace runtime() {
        if (this.runtime == null) {
            this.runtime = this.classLoader.getResource(INTERMEDIARY_LEVEL) != null ? Namespace.INTERMEDIARY
                    : this.classLoader.getResource(MOJMAP_LEVEL) != null ? Namespace.MOJMAP : Namespace.UNKNOWN;
        }

        return this.runtime;
    }

    private static Namespace namespaceOf(ClassNode model) {
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

    private static boolean matches(MemberRef wanted, String owner, String name, String descriptor) {
        return wanted.name().equals(name)
                && (wanted.owner() == null || wanted.owner().equals(owner))
                && (wanted.descriptor() == null || wanted.descriptor().equals(descriptor));
    }

    private static MemberRef parse(String reference) {
        String owner = null;
        String rest = reference;
        int semicolon = rest.indexOf(';');
        int paren = rest.indexOf('(');
        if (rest.startsWith("L") && semicolon > 0 && (paren < 0 || semicolon < paren)) {
            owner = rest.substring(1, semicolon);
            rest = rest.substring(semicolon + 1);
        }

        int colon = rest.indexOf(':');
        paren = rest.indexOf('(');
        if (colon >= 0 && (paren < 0 || colon < paren)) {
            return new MemberRef(owner, withoutQuantifier(rest.substring(0, colon)), rest.substring(colon + 1));
        }

        if (paren >= 0) {
            return new MemberRef(owner, withoutQuantifier(rest.substring(0, paren)), rest.substring(paren));
        }

        return new MemberRef(owner, withoutQuantifier(rest), null);
    }

    private static String withoutQuantifier(String name) {
        int brace = name.indexOf(QUANTIFIER_OPEN);
        if (brace >= 0) {
            return name.substring(0, brace);
        }

        return name.endsWith(ANY_QUANTIFIER) || name.endsWith(PLUS_QUANTIFIER)
                ? name.substring(0, name.length() - 1)
                : name;
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
        String prefix = element(shadow, PREFIX_KEY).map(MixinTargetCheck::asString).orElse(DEFAULT_SHADOW_PREFIX);
        names.add(method && name.startsWith(prefix) ? name.substring(prefix.length()) : name);
        names.addAll(strings(shadow, ALIASES_KEY));
        return names;
    }

    private static String generatedName(AnnotationNode annotation, String methodName, List<String> prefixes) {
        String explicit = element(annotation, VALUE_KEY).map(MixinTargetCheck::asString).orElse("");
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

    private static List<String> targetsOf(AnnotationNode mixin) {
        List<String> targets = new ArrayList<>();
        for (Object value : values(mixin, VALUE_KEY)) {
            if (value instanceof Type type) {
                targets.add(type.getInternalName());
            }
        }

        for (String target : strings(mixin, TARGETS_KEY)) {
            targets.add(internalName(target));
        }

        return targets;
    }

    private static List<AnnotationNode> annotationsOf(@Nullable List<AnnotationNode> visible,
            @Nullable List<AnnotationNode> invisible) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (visible != null) {
            annotations.addAll(visible);
        }

        if (invisible != null) {
            annotations.addAll(invisible);
        }

        return annotations;
    }

    private static Optional<AnnotationNode> find(List<AnnotationNode> annotations, String descriptor) {
        for (AnnotationNode annotation : annotations) {
            if (annotation.desc.equals(descriptor)) {
                return Optional.of(annotation);
            }
        }

        return Optional.empty();
    }

    private static Optional<Object> element(AnnotationNode annotation, String name) {
        if (annotation.values == null) {
            return Optional.empty();
        }

        for (int index = 0; index + 1 < annotation.values.size(); index += NAME_VALUE_PAIR) {
            if (name.equals(annotation.values.get(index))) {
                return Optional.of(annotation.values.get(index + 1));
            }
        }

        return Optional.empty();
    }

    private static List<?> values(AnnotationNode annotation, String name) {
        return element(annotation, name)
                .<List<?>>map(value -> value instanceof List<?> array ? array : List.of(value))
                .orElse(List.of());
    }

    private static List<String> strings(AnnotationNode annotation, String name) {
        List<String> strings = new ArrayList<>();
        for (Object value : values(annotation, name)) {
            strings.add(asString(value));
        }

        return strings;
    }

    private static List<AnnotationNode> nested(AnnotationNode annotation, String name) {
        List<AnnotationNode> nested = new ArrayList<>();
        for (Object value : values(annotation, name)) {
            if (value instanceof AnnotationNode inner) {
                nested.add(inner);
            }
        }

        return nested;
    }

    private static String string(AnnotationNode annotation, String name) {
        return element(annotation, name).map(MixinTargetCheck::asString).orElse("");
    }

    private static int integer(AnnotationNode annotation, String name, int fallback) {
        return element(annotation, name)
                .map(value -> value instanceof Integer number ? number : fallback)
                .orElse(fallback);
    }

    private static String asString(Object value) {
        return value instanceof String string ? string : "";
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

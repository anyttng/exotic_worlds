package com.exoticworlds.compat.gate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment.Side;

import com.exoticworlds.compat.gate.MixinTargetCheck.Reason;
import com.exoticworlds.compat.gate.MixinTargetCheck.Refusal;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class ModPresence {
    static final String CLASS_INDEX = "exotic_worlds.compat.classes.txt";

    private static final String PACKAGE_KEY = "package";
    private static final String PLUGIN_KEY = "plugin";
    private static final String COMMON_MIXINS_KEY = "mixins";
    private static final String CLIENT_MIXINS_KEY = "client";
    private static final String SERVER_MIXINS_KEY = "server";
    private static final List<String> MIXIN_LIST_KEYS = List.of(COMMON_MIXINS_KEY, CLIENT_MIXINS_KEY,
            SERVER_MIXINS_KEY);
    private static final String GATE_PLUGIN = ModPresenceGatePlugin.class.getName().replace('.', '/');
    private static final char PACKAGE_SEPARATOR = '/';

    private static final Object EVALUATION = new Object();

    private static volatile @Nullable Side runningSide;

    private final Logger logger;
    private final ClassLoader classLoader;
    private final String gateLabel;
    private final List<String> resources;
    private final @Nullable String config;
    private final Predicate<String> covering;
    private final Map<String, String> bodySources;
    private final Map<String, BooleanSupplier> withheld;
    private final List<ModPresence> required;
    private final List<Supplier<ModPresence>> requiredAny;
    private final List<String> modPackages;
    private final Set<String> optional;
    private final @Nullable Predicate<String> scanning;
    private final Supplier<@Nullable Side> side;

    private @Nullable MixinConfig mixinConfig;
    private @Nullable OwnChecks ownChecks;
    private boolean isProbed;
    private boolean isAdmitted;

    public static Builder gate(Logger logger, String gateLabel) {
        return new Builder(logger, gateLabel);
    }

    public static void runningSide(Side side) {
        runningSide = side;
    }

    public static boolean hasResource(String resource) {
        return hasResource(ModPresence.class.getClassLoader(), resource);
    }

    static boolean hasResource(ClassLoader classLoader, String resource) {
        return classLoader.getResource(resource) != null;
    }

    private ModPresence(Builder builder, ClassLoader classLoader, Supplier<@Nullable Side> side) {
        this.logger = builder.logger;
        this.classLoader = classLoader;
        this.gateLabel = builder.gateLabel;
        this.resources = List.copyOf(builder.resources);
        this.config = builder.config;
        this.covering = builder.covering;
        this.bodySources = Map.copyOf(builder.bodySources);
        this.withheld = Map.copyOf(builder.withheld);
        this.required = List.copyOf(builder.required);
        this.requiredAny = List.copyOf(builder.requiredAny);
        this.modPackages = List.copyOf(builder.modPackages);
        this.optional = Set.copyOf(builder.optional);
        this.scanning = builder.scanning;
        this.side = side;
    }

    public boolean isCovering(String mixinClassName) {
        if (this.config == null) {
            return false;
        }

        String prefix = getMixinPackage() + ".";
        return mixinClassName.startsWith(prefix) && this.covering.test(mixinClassName.substring(prefix.length()));
    }

    public boolean isAdmitting(String mixinClassName) {
        return isAdmitted() && !isWithheld(mixinClassName.substring(getMixinPackage().length() + 1));
    }

    private boolean isWithheld(String mixin) {
        BooleanSupplier condition = this.withheld.get(mixin);
        return condition != null && condition.getAsBoolean();
    }

    public boolean isAdmitted() {
        synchronized (EVALUATION) {
            if (!this.isProbed) {
                this.isAdmitted = admit();
                this.isProbed = true;
            }

            return this.isAdmitted;
        }
    }

    private boolean admit() {
        if (!hasAllResources()) {
            this.logger.info("{}=false", this.gateLabel);
            return false;
        }

        if (!areRequiredAdmitted() || !isAnyRequiredPassing()) {
            return false;
        }

        OwnChecks checks = getOwnChecks();
        for (Refusal refusal : checks.refusals()) {
            this.logger.warn("{}=true admitted=false mixin={} member={} reason={}", this.gateLabel,
                    refusal.mixin(), refusal.member(), refusal.reason());
        }

        if (checks.refusals().isEmpty()) {
            this.logger.info("{}=true admitted=true mixins={} classes={}", this.gateLabel, checks.mixinCount(),
                    checks.classCount());
        }

        return checks.refusals().isEmpty();
    }

    private boolean isPassing() {
        synchronized (EVALUATION) {
            return hasAllResources() && getOwnChecks().refusals().isEmpty();
        }
    }

    private boolean hasAllResources() {
        return this.resources.stream().allMatch(resource -> hasResource(this.classLoader, resource));
    }

    private boolean areRequiredAdmitted() {
        for (ModPresence gate : this.required) {
            if (!gate.isAdmitted()) {
                this.logger.warn("{}=true admitted=false requires={}", this.gateLabel, gate.gateLabel);
                return false;
            }
        }

        return true;
    }

    private boolean isAnyRequiredPassing() {
        List<ModPresence> gates = this.requiredAny.stream().map(Supplier::get).toList();
        if (gates.isEmpty() || gates.stream().anyMatch(ModPresence::isPassing)) {
            return true;
        }

        this.logger.warn("{}=true admitted=false requires_any={}", this.gateLabel,
                gates.stream().map(gate -> gate.gateLabel).toList());
        return false;
    }

    private OwnChecks getOwnChecks() {
        OwnChecks checks = this.ownChecks;
        if (checks == null) {
            checks = checkOwn(getCoveredMixins());
            this.ownChecks = checks;
        }

        return checks;
    }

    private OwnChecks checkOwn(List<String> covered) {
        Map<String, String> bodySourcesByClass = new HashMap<>();
        this.bodySources.forEach((mixin, owner) -> bodySourcesByClass.put(getMixinPackage() + "." + mixin, owner));
        ClassFiles classFiles = new ClassFiles(this.classLoader);
        MixinTargetCheck check = new MixinTargetCheck(classFiles, bodySourcesByClass);

        List<Refusal> refusals = new ArrayList<>();
        for (String mixin : covered) {
            refusals.addAll(check.check(mixin));
        }

        List<String> scanned = scanClasses(classFiles, refusals);
        ReferenceCheck references = new ReferenceCheck(classFiles, this.modPackages);
        for (String scannedClass : scanned) {
            refusals.addAll(references.check(scannedClass));
        }

        return new OwnChecks(covered.size(), scanned.size(), List.copyOf(refusals));
    }

    private record OwnChecks(int mixinCount, int classCount, List<Refusal> refusals) {
    }

    List<String> scanOwnedClasses() {
        return scanOwnedClasses(new ClassFiles(this.classLoader), new ArrayList<>()).stream()
                .map(ModPresence::className)
                .toList();
    }

    List<String> scanClasses() {
        return scanClasses(new ClassFiles(this.classLoader), new ArrayList<>());
    }

    private List<String> scanClasses(ClassFiles classFiles, List<Refusal> refusals) {
        String root = this.modPackages.isEmpty() ? "" : packageOf(pluginOf(this.config));
        Set<String> unapplied = getUnappliedMixins();
        return scanOwnedClasses(classFiles, refusals).stream()
                .filter(owned -> !this.optional.contains(owned.substring(root.length())))
                .filter(owned -> !isWithheldMixin(className(outerOf(owned))))
                .filter(owned -> !unapplied.contains(className(outerOf(owned))))
                .map(ModPresence::className)
                .toList();
    }

    private Set<String> getUnappliedMixins() {
        if (this.config == null) {
            return Set.of();
        }

        List<String> applied = getAppliedKeys();
        List<String> skipped = MIXIN_LIST_KEYS.stream().filter(key -> !applied.contains(key)).toList();
        Set<String> unapplied = new HashSet<>();
        for (String mixin : loadMixinConfig().mixins(skipped)) {
            unapplied.add(getMixinPackage() + "." + mixin);
        }

        for (String mixin : loadMixinConfig().mixins(applied)) {
            unapplied.remove(getMixinPackage() + "." + mixin);
        }

        return unapplied;
    }

    private List<String> getAppliedKeys() {
        Side current = this.side.get();
        if (current == null) {
            return MIXIN_LIST_KEYS;
        }

        return switch (current) {
            case CLIENT -> List.of(COMMON_MIXINS_KEY, CLIENT_MIXINS_KEY);
            case SERVER -> List.of(COMMON_MIXINS_KEY, SERVER_MIXINS_KEY);
            default -> List.of(COMMON_MIXINS_KEY);
        };
    }

    private boolean isWithheldMixin(String outerClass) {
        String prefix = getMixinPackage() + ".";
        return outerClass.startsWith(prefix) && isWithheld(outerClass.substring(prefix.length()));
    }

    private boolean isClaiming(String root, String listed) {
        Predicate<String> scanned = this.scanning;
        if (scanned == null) {
            return true;
        }

        String outerClass = className(outerOf(listed));
        if (outerClass.startsWith(getMixinPackage() + ".")) {
            return isCovering(outerClass);
        }

        return scanned.test(outerClass.substring(root.length()));
    }

    private static String outerOf(String internalName) {
        int nested = internalName.indexOf('$', internalName.lastIndexOf(PACKAGE_SEPARATOR) + 1);
        return nested < 0 ? internalName : internalName.substring(0, nested);
    }

    private static String className(String internalName) {
        return internalName.replace(PACKAGE_SEPARATOR, '.');
    }

    private String pluginOf(@Nullable String config) {
        String checked = Objects.requireNonNull(config, "a gate checking the mod's packages reads its config");
        return Objects.requireNonNull(loadMixinConfig().plugin(), checked + " names no plugin");
    }

    private List<String> scanOwnedClasses(ClassFiles classFiles, List<Refusal> refusals) {
        if (this.modPackages.isEmpty()) {
            return List.of();
        }

        String root = packageOf(pluginOf(this.config));
        List<String> index = readIndex();
        if (index == null) {
            refusals.add(new Refusal(Objects.requireNonNull(this.config), CLASS_INDEX, Reason.MISSING_INDEX));
            return List.of();
        }

        List<String> nestedRoots = new ArrayList<>();
        for (String listed : index) {
            String listedPackage = packageOf(listed);
            if (listedPackage.startsWith(root) && !listedPackage.equals(root) && isGatePlugin(classFiles, listed)) {
                nestedRoots.add(listedPackage);
            }
        }

        List<String> owned = new ArrayList<>();
        for (String listed : index) {
            if (listed.startsWith(root) && nestedRoots.stream().noneMatch(listed::startsWith)
                    && isClaiming(root, listed)) {
                owned.add(listed);
            }
        }

        return owned;
    }

    private static boolean isGatePlugin(ClassFiles classFiles, String internalName) {
        ClassFiles.Loaded loaded = classFiles.load(internalName);
        return loaded.model() != null && loaded.model().superclass()
                .filter(type -> type.asInternalName().equals(GATE_PLUGIN)).isPresent();
    }

    private static String packageOf(String className) {
        String internalName = className.replace('.', PACKAGE_SEPARATOR);
        return internalName.substring(0, internalName.lastIndexOf(PACKAGE_SEPARATOR) + 1);
    }

    private @Nullable List<String> readIndex() {
        try (InputStream stream = this.classLoader.getResourceAsStream(CLASS_INDEX)) {
            if (stream == null) {
                return null;
            }

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty())
                    .toList();
        } catch (IOException unreadable) {
            return null;
        }
    }

    private List<String> getCoveredMixins() {
        if (this.config == null) {
            return List.of();
        }

        List<String> covered = new ArrayList<>();
        for (String mixin : loadMixinConfig().mixins(getAppliedKeys())) {
            if (this.covering.test(mixin) && !isWithheld(mixin)) {
                covered.add(getMixinPackage() + "." + mixin);
            }
        }

        return covered;
    }

    private String getMixinPackage() {
        return loadMixinConfig().mixinPackage();
    }

    private synchronized MixinConfig loadMixinConfig() {
        MixinConfig read = this.mixinConfig;
        if (read == null) {
            read = readConfig(Objects.requireNonNull(this.config, "a gate without a mixin config covers no mixin"));
            this.mixinConfig = read;
        }

        return read;
    }

    private MixinConfig readConfig(String config) {
        try (InputStream stream = this.classLoader.getResourceAsStream(config)) {
            if (stream == null) {
                throw new IllegalStateException("mixin config " + config + " is not on the classpath");
            }

            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                Map<String, List<String>> blocks = mixinLists(json);
                String plugin = json.has(PLUGIN_KEY) ? json.get(PLUGIN_KEY).getAsString() : null;
                return new MixinConfig(json.get(PACKAGE_KEY).getAsString(), plugin, blocks);
            }
        } catch (IOException unreadable) {
            throw new IllegalStateException("mixin config " + config + " cannot be read", unreadable);
        }
    }

    private static Map<String, List<String>> mixinLists(JsonObject json) {
        Map<String, List<String>> blocks = new HashMap<>();
        for (String key : MIXIN_LIST_KEYS) {
            List<String> mixins = new ArrayList<>();
            if (json.get(key) instanceof JsonArray list) {
                for (JsonElement mixin : list) {
                    mixins.add(mixin.getAsString());
                }
            }

            blocks.put(key, List.copyOf(mixins));
        }

        return Map.copyOf(blocks);
    }

    private record MixinConfig(String mixinPackage, @Nullable String plugin, Map<String, List<String>> blocks) {
        List<String> mixins(List<String> keys) {
            List<String> mixins = new ArrayList<>();
            for (String key : keys) {
                mixins.addAll(this.blocks.get(key));
            }

            return mixins;
        }
    }

    public static final class Builder {
        private final Logger logger;
        private final String gateLabel;
        private final List<String> resources = new ArrayList<>();
        private final Map<String, String> bodySources = new HashMap<>();
        private final Map<String, BooleanSupplier> withheld = new HashMap<>();
        private final List<ModPresence> required = new ArrayList<>();
        private final List<Supplier<ModPresence>> requiredAny = new ArrayList<>();
        private final List<String> modPackages = new ArrayList<>();
        private final Set<String> optional = new HashSet<>();

        private @Nullable String config;
        private @Nullable Predicate<String> scanning;
        private Predicate<String> covering = mixin -> true;

        private Builder(Logger logger, String gateLabel) {
            this.logger = logger;
            this.gateLabel = gateLabel;
        }

        public Builder probing(String... resources) {
            this.resources.addAll(List.of(resources));
            return this;
        }

        public Builder checking(String config, Predicate<String> covering) {
            this.config = config;
            this.covering = covering;
            return this;
        }

        public Builder checking(String config) {
            return checking(config, mixin -> true);
        }

        public Builder bodyFrom(String mixin, String owner) {
            this.bodySources.put(mixin, owner);
            return this;
        }

        public Builder withholding(String mixin, BooleanSupplier when) {
            this.withheld.put(mixin, when);
            return this;
        }

        public Builder requiring(ModPresence gate) {
            this.required.add(gate);
            return this;
        }

        public Builder requiringAny(List<Supplier<ModPresence>> gates) {
            this.requiredAny.addAll(gates);
            return this;
        }

        public Builder modPackages(String... packages) {
            this.modPackages.addAll(List.of(packages));
            return this;
        }

        public Builder optional(String holder) {
            this.optional.add(holder);
            return this;
        }

        public Builder scanning(Predicate<String> scanned) {
            this.scanning = scanned;
            return this;
        }

        public ModPresence build() {
            return build(ModPresence.class.getClassLoader());
        }

        ModPresence build(ClassLoader classLoader) {
            return build(classLoader, () -> runningSide);
        }

        ModPresence build(ClassLoader classLoader, Supplier<@Nullable Side> side) {
            return new ModPresence(this, classLoader, side);
        }
    }
}

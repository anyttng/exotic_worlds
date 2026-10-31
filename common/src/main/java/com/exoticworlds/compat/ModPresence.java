package com.exoticworlds.compat;

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

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.exoticworlds.compat.MixinTargetCheck.Reason;
import com.exoticworlds.compat.MixinTargetCheck.Refusal;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class ModPresence {
    static final String CLASS_INDEX = "exotic_worlds.compat.classes.txt";

    private static final String PACKAGE_KEY = "package";
    private static final String PLUGIN_KEY = "plugin";
    private static final List<String> MIXIN_LIST_KEYS = List.of("mixins", "client", "server");
    private static final String GATE_PLUGIN = ModPresenceGatePlugin.class.getName().replace('.', '/');
    private static final char PACKAGE_SEPARATOR = '/';

    private final Logger logger;
    private final ClassLoader classLoader;
    private final String gateLabel;
    private final List<String> resources;
    private final @Nullable String config;
    private final Predicate<String> covering;
    private final Map<String, String> bodySources;
    private final Map<String, BooleanSupplier> withheld;
    private final List<ModPresence> required;
    private final List<String> modPackages;
    private final Set<String> optional;
    private final @Nullable Predicate<String> scanning;

    private @Nullable MixinConfig mixinConfig;
    private boolean probed;
    private boolean admitted;

    public static Builder gate(Logger logger, String gateLabel) {
        return new Builder(logger, gateLabel);
    }

    public static boolean probe(String resource) {
        return probe(ModPresence.class.getClassLoader(), resource);
    }

    static boolean probe(ClassLoader classLoader, String resource) {
        return classLoader.getResource(resource) != null;
    }

    private ModPresence(Builder builder, ClassLoader classLoader) {
        this.logger = builder.logger;
        this.classLoader = classLoader;
        this.gateLabel = builder.gateLabel;
        this.resources = List.copyOf(builder.resources);
        this.config = builder.config;
        this.covering = builder.covering;
        this.bodySources = Map.copyOf(builder.bodySources);
        this.withheld = Map.copyOf(builder.withheld);
        this.required = List.copyOf(builder.required);
        this.modPackages = List.copyOf(builder.modPackages);
        this.optional = Set.copyOf(builder.optional);
        this.scanning = builder.scanning;
    }

    public boolean covers(String mixinClassName) {
        if (this.config == null) {
            return false;
        }

        String prefix = mixinPackage() + ".";
        return mixinClassName.startsWith(prefix) && this.covering.test(mixinClassName.substring(prefix.length()));
    }

    public boolean admits(String mixinClassName) {
        return admitted() && !withheld(mixinClassName.substring(mixinPackage().length() + 1));
    }

    private boolean withheld(String mixin) {
        BooleanSupplier condition = this.withheld.get(mixin);
        return condition != null && condition.getAsBoolean();
    }

    public synchronized boolean admitted() {
        if (!this.probed) {
            this.admitted = resolve();
            this.probed = true;
        }

        return this.admitted;
    }

    private boolean resolve() {
        for (String resource : this.resources) {
            if (!probe(this.classLoader, resource)) {
                this.logger.info("{}=false", this.gateLabel);
                return false;
            }
        }

        return requiredAdmitted() && admits(coveredMixins());
    }

    private boolean requiredAdmitted() {
        for (ModPresence gate : this.required) {
            if (!gate.admitted()) {
                this.logger.warn("{}=true admitted=false requires={}", this.gateLabel, gate.gateLabel);
                return false;
            }
        }

        return true;
    }

    private boolean admits(List<String> covered) {
        Map<String, String> bodySourcesByClass = new HashMap<>();
        this.bodySources.forEach((mixin, owner) -> bodySourcesByClass.put(mixinPackage() + "." + mixin, owner));
        ClassFiles classFiles = new ClassFiles(this.classLoader);
        MixinTargetCheck check = new MixinTargetCheck(classFiles, bodySourcesByClass);

        List<Refusal> refusals = new ArrayList<>();
        for (String mixin : covered) {
            refusals.addAll(check.check(mixin));
        }

        List<String> scanned = scannedClasses(classFiles, refusals);
        ReferenceCheck references = new ReferenceCheck(classFiles, this.modPackages);
        for (String scannedClass : scanned) {
            refusals.addAll(references.check(scannedClass));
        }

        for (Refusal refusal : refusals) {
            this.logger.warn("{}=true admitted=false mixin={} member={} reason={}", this.gateLabel,
                    refusal.mixin(), refusal.member(), refusal.reason());
        }

        if (refusals.isEmpty()) {
            this.logger.info("{}=true admitted=true mixins={} classes={}", this.gateLabel, covered.size(),
                    scanned.size());
        }

        return refusals.isEmpty();
    }

    List<String> ownedClasses() {
        return ownedClasses(new ClassFiles(this.classLoader), new ArrayList<>()).stream()
                .map(ModPresence::className)
                .toList();
    }

    List<String> scannedClasses() {
        return scannedClasses(new ClassFiles(this.classLoader), new ArrayList<>());
    }

    private List<String> scannedClasses(ClassFiles classFiles, List<Refusal> refusals) {
        String root = this.modPackages.isEmpty() ? "" : packageOf(pluginOf(this.config));
        return ownedClasses(classFiles, refusals).stream()
                .filter(owned -> !this.optional.contains(owned.substring(root.length())))
                .filter(owned -> !withheldMixin(className(outerOf(owned))))
                .map(ModPresence::className)
                .toList();
    }

    private boolean withheldMixin(String outerClass) {
        String prefix = mixinPackage() + ".";
        return outerClass.startsWith(prefix) && withheld(outerClass.substring(prefix.length()));
    }

    private boolean claims(String root, String listed) {
        Predicate<String> scanned = this.scanning;
        if (scanned == null) {
            return true;
        }

        String outerClass = className(outerOf(listed));
        if (outerClass.startsWith(mixinPackage() + ".")) {
            return covers(outerClass);
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
        return Objects.requireNonNull(mixinConfig().plugin(), checked + " names no plugin");
    }

    private List<String> ownedClasses(ClassFiles classFiles, List<Refusal> refusals) {
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
            if (listedPackage.startsWith(root) && !listedPackage.equals(root) && gatePlugin(classFiles, listed)) {
                nestedRoots.add(listedPackage);
            }
        }

        List<String> owned = new ArrayList<>();
        for (String listed : index) {
            if (listed.startsWith(root) && nestedRoots.stream().noneMatch(listed::startsWith)
                    && claims(root, listed)) {
                owned.add(listed);
            }
        }

        return owned;
    }

    private static boolean gatePlugin(ClassFiles classFiles, String internalName) {
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

    private List<String> coveredMixins() {
        if (this.config == null) {
            return List.of();
        }

        List<String> covered = new ArrayList<>();
        for (String mixin : configMixins()) {
            if (this.covering.test(mixin) && !withheld(mixin)) {
                covered.add(mixinPackage() + "." + mixin);
            }
        }

        return covered;
    }

    private String mixinPackage() {
        return mixinConfig().mixinPackage();
    }

    private List<String> configMixins() {
        return mixinConfig().mixins();
    }

    private synchronized MixinConfig mixinConfig() {
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
                List<String> mixins = new ArrayList<>();
                for (String key : MIXIN_LIST_KEYS) {
                    if (json.get(key) instanceof JsonArray list) {
                        for (JsonElement mixin : list) {
                            mixins.add(mixin.getAsString());
                        }
                    }
                }

                String plugin = json.has(PLUGIN_KEY) ? json.get(PLUGIN_KEY).getAsString() : null;
                return new MixinConfig(json.get(PACKAGE_KEY).getAsString(), plugin, List.copyOf(mixins));
            }
        } catch (IOException unreadable) {
            throw new IllegalStateException("mixin config " + config + " cannot be read", unreadable);
        }
    }

    private record MixinConfig(String mixinPackage, @Nullable String plugin, List<String> mixins) {
    }

    public static final class Builder {
        private final Logger logger;
        private final String gateLabel;
        private final List<String> resources = new ArrayList<>();
        private final Map<String, String> bodySources = new HashMap<>();
        private final Map<String, BooleanSupplier> withheld = new HashMap<>();
        private final List<ModPresence> required = new ArrayList<>();
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
            return new ModPresence(this, classLoader);
        }
    }
}

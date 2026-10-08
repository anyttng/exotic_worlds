package com.exoticworlds.compat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import org.slf4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class ModPresence {
    private static final String PACKAGE_KEY = "package";
    private static final List<String> MIXIN_LIST_KEYS = List.of("mixins", "client", "server");

    private final Logger logger;
    private final ClassLoader classLoader;
    private final String gateLabel;
    private final List<String> resources;
    private final String config;
    private final Predicate<String> covering;
    private final Map<String, String> bodySources;
    private final Map<String, BooleanSupplier> withheld;

    private String mixinPackage;
    private List<String> configMixins;
    private boolean probed;
    private boolean present;

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
    }

    public boolean covers(String mixinClassName) {
        if (this.config == null) {
            return false;
        }

        String prefix = mixinPackage() + ".";
        return mixinClassName.startsWith(prefix) && this.covering.test(mixinClassName.substring(prefix.length()));
    }

    public boolean admits(String mixinClassName) {
        return present() && !withheld(mixinClassName.substring(mixinPackage().length() + 1));
    }

    private boolean withheld(String mixin) {
        BooleanSupplier condition = this.withheld.get(mixin);
        return condition != null && condition.getAsBoolean();
    }

    public synchronized boolean present() {
        if (!this.probed) {
            this.present = resolve();
            this.probed = true;
        }

        return this.present;
    }

    private boolean resolve() {
        for (String resource : this.resources) {
            if (!probe(this.classLoader, resource)) {
                this.logger.info("{}=false", this.gateLabel);
                return false;
            }
        }

        List<String> covered = coveredMixins();
        Map<String, String> bodySourcesByClass = new HashMap<>();
        this.bodySources.forEach((mixin, owner) -> bodySourcesByClass.put(mixinPackage() + "." + mixin, owner));
        MixinTargetCheck check = new MixinTargetCheck(this.classLoader, bodySourcesByClass);

        boolean admitted = true;
        for (String mixin : covered) {
            for (MixinTargetCheck.Refusal refusal : check.check(mixin)) {
                this.logger.warn("{}=true admitted=false mixin={} member={} reason={}", this.gateLabel,
                        refusal.mixin(), refusal.member(), refusal.reason());
                admitted = false;
            }
        }

        if (admitted) {
            this.logger.info("{}=true admitted=true mixins={}", this.gateLabel, covered.size());
        }

        return admitted;
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

    private synchronized String mixinPackage() {
        readConfig();
        return this.mixinPackage;
    }

    private synchronized List<String> configMixins() {
        readConfig();
        return this.configMixins;
    }

    private void readConfig() {
        if (this.configMixins != null) {
            return;
        }

        try (InputStream stream = this.classLoader.getResourceAsStream(this.config)) {
            if (stream == null) {
                throw new IllegalStateException("mixin config " + this.config + " is not on the classpath");
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

                this.mixinPackage = json.get(PACKAGE_KEY).getAsString();
                this.configMixins = List.copyOf(mixins);
            }
        } catch (IOException unreadable) {
            throw new IllegalStateException("mixin config " + this.config + " cannot be read", unreadable);
        }
    }

    public static final class Builder {
        private final Logger logger;
        private final String gateLabel;
        private final List<String> resources = new ArrayList<>();
        private final Map<String, String> bodySources = new HashMap<>();
        private final Map<String, BooleanSupplier> withheld = new HashMap<>();

        private String config;
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

        public ModPresence build() {
            return build(ModPresence.class.getClassLoader());
        }

        ModPresence build(ClassLoader classLoader) {
            return new ModPresence(this, classLoader);
        }
    }
}

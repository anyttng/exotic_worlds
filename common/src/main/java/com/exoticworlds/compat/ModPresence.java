package com.exoticworlds.compat;

import java.util.List;

import org.slf4j.Logger;

public final class ModPresence {
    private final Logger logger;
    private final ClassLoader classLoader;
    private final String resource;
    private final String gateLabel;
    private final List<ModSymbol> required;

    private boolean probed;
    private boolean present;

    public static ModPresence of(Logger logger, String resource, String gateLabel, ModSymbol... required) {
        return new ModPresence(logger, ModPresence.class.getClassLoader(), resource, gateLabel, List.of(required));
    }

    public static boolean probe(String resource) {
        return probe(ModPresence.class.getClassLoader(), resource);
    }

    static boolean probe(ClassLoader classLoader, String resource) {
        return classLoader.getResource(resource) != null;
    }

    ModPresence(Logger logger, ClassLoader classLoader, String resource, String gateLabel,
            List<ModSymbol> required) {
        this.logger = logger;
        this.classLoader = classLoader;
        this.resource = resource;
        this.gateLabel = gateLabel;
        this.required = required;
    }

    public synchronized boolean present() {
        if (!this.probed) {
            this.present = resolve();
            this.probed = true;
        }

        return this.present;
    }

    private boolean resolve() {
        if (!probe(this.classLoader, this.resource)) {
            this.logger.info("{}=false", this.gateLabel);
            return false;
        }

        if (this.required.isEmpty()) {
            this.logger.info("{}=true", this.gateLabel);
            return true;
        }

        for (ModSymbol symbol : this.required) {
            if (!symbol.carriedBy(this.classLoader)) {
                this.logger.warn("{}=true symbol_present=false symbol={}", this.gateLabel, symbol);
                return false;
            }
        }

        this.logger.info("{}=true symbol_present=true symbols={}", this.gateLabel, this.required);
        return true;
    }
}

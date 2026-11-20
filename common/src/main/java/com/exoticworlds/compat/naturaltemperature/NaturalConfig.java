package com.exoticworlds.compat.naturaltemperature;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.gate.ModPresence;

public interface NaturalConfig {
    int generationMode();

    double equatorialDistance();

    double equatorOffset();

    boolean loopingWorld();

    double waveFrequency();

    @Nullable String customModeId(int slot);

    static NaturalConfig current() {
        return Loaded.CONFIG;
    }

    final class Loaded {
        private static final String NEOFORGE_CONFIG = "com/naturaltemperature/Config.class";

        private static final NaturalConfig CONFIG = ModPresence.probe(NEOFORGE_CONFIG)
                ? new NeoForgeNaturalConfig()
                : new FabricNaturalConfig();

        private Loaded() {
        }
    }
}

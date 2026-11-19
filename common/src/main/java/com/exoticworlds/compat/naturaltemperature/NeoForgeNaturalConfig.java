package com.exoticworlds.compat.naturaltemperature;

import org.jspecify.annotations.Nullable;

import com.naturaltemperature.Config;

final class NeoForgeNaturalConfig implements NaturalConfig {
    @Override
    public int generationMode() {
        return Config.generation_mode;
    }

    @Override
    public double equatorialDistance() {
        return Config.equatorial_distance;
    }

    @Override
    public double equatorOffset() {
        return Config.equator_offset;
    }

    @Override
    public boolean loopingWorld() {
        return Config.looping_world;
    }

    @Override
    public @Nullable String customModeId(int slot) {
        return switch (slot) {
            case 1 -> Config.custom_mode_id_1;
            case 2 -> Config.custom_mode_id_2;
            case 3 -> Config.custom_mode_id_3;
            case 4 -> Config.custom_mode_id_4;
            case 5 -> Config.custom_mode_id_5;
            default -> null;
        };
    }
}

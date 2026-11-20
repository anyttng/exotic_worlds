package com.exoticworlds.compat.naturaltemperature;

import org.jspecify.annotations.Nullable;

import naturaltemperature.ModConfig;
import naturaltemperature.NaturalTemperature;

final class FabricNaturalConfig implements NaturalConfig {
    @Override
    public int generationMode() {
        return NaturalTemperature.CONFIG.generation_mode;
    }

    @Override
    public double equatorialDistance() {
        return NaturalTemperature.CONFIG.equatorial_distance;
    }

    @Override
    public double equatorOffset() {
        return NaturalTemperature.CONFIG.equator_offset;
    }

    @Override
    public boolean loopingWorld() {
        return NaturalTemperature.CONFIG.looping_world;
    }

    @Override
    public double waveFrequency() {
        return NaturalTemperature.CONFIG.wave_frequency;
    }

    @Override
    public @Nullable String customModeId(int slot) {
        ModConfig config = NaturalTemperature.CONFIG;
        return switch (slot) {
            case 1 -> config.custom_mode_id_1;
            case 2 -> config.custom_mode_id_2;
            case 3 -> config.custom_mode_id_3;
            case 4 -> config.custom_mode_id_4;
            case 5 -> config.custom_mode_id_5;
            default -> null;
        };
    }
}

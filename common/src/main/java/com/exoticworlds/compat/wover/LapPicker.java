package com.exoticworlds.compat.wover;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.betterx.wover.generator.api.biomesource.WoverBiomePicker;

import net.minecraft.world.level.levelgen.WorldgenRandom;

public record LapPicker<T>(Function<WorldgenRandom, T> pick, BiFunction<T, WorldgenRandom, T> subBiome) {
    public static LapPicker<WoverBiomePicker.PickableBiome> of(WoverBiomePicker picker) {
        return new LapPicker<>(picker::getBiome, WoverBiomePicker.PickableBiome::getSubBiome);
    }
}

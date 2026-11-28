package com.exoticworlds.compat.wover;

import java.util.function.BiFunction;
import java.util.function.Function;

import net.minecraft.world.level.levelgen.WorldgenRandom;

public record LapPicker<T>(Function<WorldgenRandom, T> pick, BiFunction<T, WorldgenRandom, T> subBiome) {
}

package com.exoticworlds.shape.torus;

import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class CoastFields {
    private static final Set<ResourceKey<NormalNoise>> KEYS = Set.of(
            Noises.CONTINENTALNESS,
            Noises.CONTINENTALNESS_LARGE);

    public static boolean isCoast(ResourceKey<NormalNoise> key) {
        return KEYS.contains(key);
    }

    private CoastFields() {
    }
}

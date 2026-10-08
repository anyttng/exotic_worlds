package com.exoticworlds.shape.noise;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.exoticworlds.core.DensityFunctionNodes;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.generator.NoiseFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class DensityNoises {
    public static List<NoiseFunction> matching(DensityFunction function, Predicate<ResourceKey<NormalNoise>> key) {
        List<NoiseFunction> found = new ArrayList<>();
        DensityFunctionNodes.forEach(function, node -> {
            if (node instanceof NoiseFunction noise && noise.noise().unwrapKey().filter(key).isPresent()) {
                found.add(noise);
            }
        });
        return found;
    }

    private DensityNoises() {
    }
}

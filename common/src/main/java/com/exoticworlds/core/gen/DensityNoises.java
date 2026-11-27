package com.exoticworlds.core.gen;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.NoiseNode;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.generator.NoiseFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class DensityNoises {
    public static List<Holder<NormalNoise>> noisesUnder(DensityFunction function) {
        List<Holder<NormalNoise>> found = new ArrayList<>();
        DensityFunctionNodes.forEach(function, node -> {
            if (node instanceof NoiseFunction noise) {
                found.add(noise.noise());
            } else if (node instanceof NoiseNode noise) {
                found.add(noise.exotic_worlds$getNoise());
            }
        });
        return found;
    }

    public static @Nullable NoiseFunction firstNoiseFunctionUnder(DensityFunction function) {
        return (NoiseFunction) DensityFunctionNodes.first(function, NoiseFunction.class::isInstance);
    }

    private DensityNoises() {
    }
}

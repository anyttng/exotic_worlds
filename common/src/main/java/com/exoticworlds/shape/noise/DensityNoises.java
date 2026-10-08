package com.exoticworlds.shape.noise;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.DensityFunction.Visitor;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class DensityNoises {

    public record ScaledNoise(NoiseHolder noise, double xzScale, double yScale) {
    }

    public static List<ScaledNoise> scaledMatching(DensityFunction function,
            Predicate<ResourceKey<NormalNoise.NoiseParameters>> key) {
        List<ScaledNoise> found = new ArrayList<>();
        function.mapAll(new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction input) {
                ScaledNoise scaled = switch (input) {
                    case DensityFunctions.ShiftedNoise shifted ->
                            new ScaledNoise(shifted.noise(), shifted.xzScale(), shifted.yScale());
                    case DensityFunctions.Noise plain -> new ScaledNoise(plain.noise(), plain.xzScale(), plain.yScale());
                    default -> null;
                };
                if (scaled != null && scaled.noise().noiseData().unwrapKey().filter(key).isPresent()) {
                    found.add(scaled);
                }

                return input;
            }

            @Override
            public NoiseHolder visitNoise(NoiseHolder noise) {
                return noise;
            }
        });

        return found;
    }

    public static List<NoiseHolder> matching(DensityFunction function,
            Predicate<ResourceKey<NormalNoise.NoiseParameters>> key) {
        List<NoiseHolder> found = new ArrayList<>();
        function.mapAll(new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction input) {
                return input;
            }

            @Override
            public NoiseHolder visitNoise(NoiseHolder noise) {
                if (noise.noiseData().unwrapKey().filter(key).isPresent()) {
                    found.add(noise);
                }

                return noise;
            }
        });

        return found;
    }

    private DensityNoises() {
    }
}

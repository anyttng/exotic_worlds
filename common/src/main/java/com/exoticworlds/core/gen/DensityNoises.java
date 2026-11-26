package com.exoticworlds.core.gen;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.DensityFunction.Visitor;
import net.minecraft.world.level.levelgen.DensityFunctions;

public final class DensityNoises {

    public record ScaledNoise(NoiseHolder noise, double xzScale, double yScale) {
    }

    public static List<NoiseHolder> noisesUnder(DensityFunction function) {
        List<NoiseHolder> found = new ArrayList<>();
        function.mapAll(new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction input) {
                if (isShift(input)) {
                    found.removeLast();
                }

                return input;
            }

            @Override
            public NoiseHolder visitNoise(NoiseHolder noise) {
                found.add(noise);
                return noise;
            }
        });

        return found;
    }

    private static boolean isShift(DensityFunction function) {
        return function instanceof DensityFunctions.Shift || function instanceof DensityFunctions.ShiftA
                || function instanceof DensityFunctions.ShiftB;
    }

    public static @Nullable ScaledNoise firstScaledNoiseUnder(DensityFunction function) {
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
                if (scaled != null) {
                    found.add(scaled);
                }

                return input;
            }

            @Override
            public NoiseHolder visitNoise(NoiseHolder noise) {
                return noise;
            }
        });

        return found.isEmpty() ? null : found.getFirst();
    }

    private DensityNoises() {
    }
}

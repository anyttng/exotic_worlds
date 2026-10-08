package com.exoticworlds.engine.noise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.exoticworlds.accessors.NoiseScaleRungs;
import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class NoiseScaleLadder {
    public static final NoiseScaleLadder NONE = new NoiseScaleLadder(Map.of());

    private static final long FLOOR_CELLS = LapFloor.TWO_CELLS.period;

    private final Map<NormalNoise, Map<Double, Double>> separated;

    private NoiseScaleLadder(Map<NormalNoise, Map<Double, Double>> separated) {
        this.separated = separated;
    }

    public static NoiseScaleLadder of(WorldFold fold, List<DensityFunction> roots) {
        int lap = ClimateScaleCompression.lapBlocks(fold);
        if (lap == ClimateScaleCompression.UNBOUNDED_LAP) {
            return NONE;
        }

        Map<NormalNoise, Map<Double, Double>> separated = new IdentityHashMap<>();
        for (Scales scales : scalesOf(roots).values()) {
            if (scales.ascending().size() < 2) {
                continue;
            }

            double[] frequencies = frequencies(scales.parameters());
            if (collides(lap, scales.ascending(), frequencies)) {
                separated.put(scales.noise(), rungs(lap, scales.ascending(), lowestFrequency(frequencies)));
            }
        }

        return separated.isEmpty() ? NONE : new NoiseScaleLadder(separated);
    }

    public static double installedScale(DensityFunction.NoiseHolder holder, double xzScale) {
        return holder.noise() instanceof NoiseScaleRungs rungs ? rungOf(rungs.toroidal$scaleRungs(), xzScale) : xzScale;
    }

    public double separated(NormalNoise noise, double xzScale) {
        return rungOf(this.separated.getOrDefault(noise, Map.of()), xzScale);
    }

    public void install() {
        this.separated.forEach((noise, rungs) -> ((NoiseScaleRungs) noise).toroidal$scaleRungs(rungs));
    }

    private static double rungOf(Map<Double, Double> rungs, double xzScale) {
        Double rung = rungs.get(xzScale);
        return rung == null ? xzScale : rung;
    }

    @SuppressWarnings("deprecation")
    private static Map<NormalNoise, Scales> scalesOf(List<DensityFunction> roots) {
        Map<NormalNoise, Scales> scales = new IdentityHashMap<>();
        DensityFunction.Visitor collector = new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction input) {
                switch (input) {
                    case DensityFunctions.Noise noise -> add(scales, noise.noise(), noise.xzScale());
                    case DensityFunctions.ShiftedNoise shifted -> add(scales, shifted.noise(), shifted.xzScale());
                    default -> {
                    }
                }

                return input;
            }
        };
        for (DensityFunction root : roots) {
            root.mapAll(collector);
        }

        return scales;
    }

    private static void add(Map<NormalNoise, Scales> scales, DensityFunction.NoiseHolder holder, double xzScale) {
        NormalNoise noise = holder.noise();
        if (noise != null) {
            scales.computeIfAbsent(noise, unused -> new Scales(noise, holder.noiseData().value(), new TreeSet<>()))
                    .ascending().add(xzScale);
        }
    }

    private static double[] frequencies(NormalNoise.NoiseParameters parameters) {
        List<Double> frequencies = new ArrayList<>();
        for (int i = 0; i < parameters.amplitudes().size(); i++) {
            if (parameters.amplitudes().getDouble(i) != 0.0) {
                double frequency = Math.pow(2.0, parameters.firstOctave() + i);
                frequencies.add(frequency);
                frequencies.add(frequency * NoiseConstants.SECOND_LAYER_DETUNE);
            }
        }

        return frequencies.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private static boolean collides(int lap, SortedSet<Double> scales, double[] frequencies) {
        for (double frequency : frequencies) {
            Set<Long> periods = new HashSet<>();
            for (double scale : scales) {
                periods.add(cells(lap, scale, frequency));
            }

            if (periods.size() < scales.size()) {
                return true;
            }
        }

        return false;
    }

    private static Map<Double, Double> rungs(int lap, SortedSet<Double> ascending, double lowestFrequency) {
        Map<Double, Double> rungs = new HashMap<>();
        long previous = 0L;
        for (double scale : ascending) {
            long cells = Math.max(cells(lap, scale, lowestFrequency), previous + 1L);
            rungs.put(scale, cells / (lap * lowestFrequency));
            previous = cells;
        }

        return Map.copyOf(rungs);
    }

    private static long cells(int lap, double scale, double frequency) {
        return Math.max(FLOOR_CELLS, Math.round(lap * scale * frequency));
    }

    private static double lowestFrequency(double[] frequencies) {
        double lowest = Double.POSITIVE_INFINITY;
        for (double frequency : frequencies) {
            lowest = Math.min(lowest, frequency);
        }

        return lowest;
    }

    private record Scales(NormalNoise noise, NormalNoise.NoiseParameters parameters, SortedSet<Double> ascending) {
    }
}

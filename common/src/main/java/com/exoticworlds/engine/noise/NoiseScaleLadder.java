package com.exoticworlds.engine.noise;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.exoticworlds.core.DensityFunctionNodes;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.generator.NoiseFunction;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class NoiseScaleLadder {
    public static final NoiseScaleLadder NONE = new NoiseScaleLadder(Map.of());

    private static final long FLOOR_CELLS = LapFloor.TWO_CELLS.period;

    private final Map<Holder<NormalNoise>, Map<Double, Double>> separated;

    private NoiseScaleLadder(Map<Holder<NormalNoise>, Map<Double, Double>> separated) {
        this.separated = separated;
    }

    public static NoiseScaleLadder of(WorldFold fold, List<DensityFunction> roots,
            DensityFunction.CompileContext context) {
        int lap = ClimateScaleCompression.lapBlocks(fold);
        if (lap == ClimateScaleCompression.UNBOUNDED_LAP) {
            return NONE;
        }

        Map<Holder<NormalNoise>, Map<Double, Double>> separated = new HashMap<>();
        for (Map.Entry<Holder<NormalNoise>, SortedSet<Double>> entry : scalesOf(roots).entrySet()) {
            SortedSet<Double> ascending = entry.getValue();
            if (ascending.size() < 2) {
                continue;
            }

            NoiseStack.Layer[] layers = FoldedSamplers.stackOf(context.createNoiseSampler(entry.getKey())).layers;
            if (collides(lap, ascending, layers)) {
                separated.put(entry.getKey(), rungs(lap, ascending, lowestFrequency(layers)));
            }
        }

        return separated.isEmpty() ? NONE : new NoiseScaleLadder(Map.copyOf(separated));
    }

    public double separated(Holder<NormalNoise> noise, double xzScale) {
        Map<Double, Double> rungs = this.separated.get(noise);
        if (rungs == null) {
            return xzScale;
        }

        Double rung = rungs.get(xzScale);
        return rung == null ? xzScale : rung;
    }

    private static Map<Holder<NormalNoise>, SortedSet<Double>> scalesOf(List<DensityFunction> roots) {
        Map<Holder<NormalNoise>, SortedSet<Double>> scales = new HashMap<>();
        for (DensityFunction root : roots) {
            DensityFunctionNodes.forEach(root, node -> {
                if (node instanceof NoiseFunction noise) {
                    scales.computeIfAbsent(noise.noise(), _ -> new TreeSet<>()).add(noise.xzScale());
                }
            });
        }

        return scales;
    }

    private static boolean collides(int lap, SortedSet<Double> scales, NoiseStack.Layer[] layers) {
        for (NoiseStack.Layer layer : layers) {
            Set<Long> periods = new HashSet<>();
            for (double scale : scales) {
                periods.add(cells(lap, scale, layer.frequency()));
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

    private static double lowestFrequency(NoiseStack.Layer[] layers) {
        double lowest = Double.POSITIVE_INFINITY;
        for (NoiseStack.Layer layer : layers) {
            lowest = Math.min(lowest, layer.frequency());
        }

        return lowest;
    }
}

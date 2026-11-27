package com.exoticworlds.engine.noise;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.gen.DensityFunctionNodes;
import com.exoticworlds.engine.noise.cuboid.CuboidScaleCells;
import com.exoticworlds.engine.noise.periodic.LapFloor;

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

    private interface Span {
        int rungBlocks();

        long rungCells(double layerScale);

        Object latticeCells(double layerScale);
    }

    private record LapSpan(int rungBlocks) implements Span {
        @Override
        public long rungCells(double layerScale) {
            return Math.max(FLOOR_CELLS, Math.round(this.rungBlocks * layerScale));
        }

        @Override
        public Object latticeCells(double layerScale) {
            return rungCells(layerScale);
        }
    }

    private record CuboidSpan(CuboidAtlas atlas, int rungBlocks) implements Span {
        @Override
        public long rungCells(double layerScale) {
            return CuboidScaleCells.alongEdge(this.rungBlocks, layerScale);
        }

        @Override
        public Object latticeCells(double layerScale) {
            return CuboidScaleCells.perBoxAxis(this.atlas, layerScale);
        }
    }

    public static NoiseScaleLadder of(WorldFold fold, List<DensityFunction> roots,
            DensityFunction.CompileContext context) {
        Span span = spanOf(fold);
        return span == null ? NONE : of(span, roots, context);
    }

    private static @Nullable Span spanOf(WorldFold fold) {
        CuboidAtlas atlas = fold.getCuboidAtlas();
        if (atlas != null) {
            return new CuboidSpan(atlas, CuboidScaleCells.shortestEdgeBlocks(atlas));
        }

        int lap = ClimateScaleCompression.lapBlocks(fold);
        return lap == ClimateScaleCompression.UNBOUNDED_LAP ? null : new LapSpan(lap);
    }

    private static NoiseScaleLadder of(Span span, List<DensityFunction> roots,
            DensityFunction.CompileContext context) {
        Map<Holder<NormalNoise>, Map<Double, Double>> separated = new HashMap<>();
        for (Map.Entry<Holder<NormalNoise>, SortedSet<Double>> entry : scalesOf(roots).entrySet()) {
            SortedSet<Double> ascending = entry.getValue();
            if (ascending.size() < 2) {
                continue;
            }

            NoiseStack.Layer[] layers = FoldedSamplers.getStack(context.createNoiseSampler(entry.getKey())).layers;
            if (isColliding(span, ascending, layers)) {
                separated.put(entry.getKey(), rungs(span, ascending, lowestFrequency(layers)));
            }
        }

        return separated.isEmpty() ? NONE : new NoiseScaleLadder(Map.copyOf(separated));
    }

    public double getSeparated(Holder<NormalNoise> noise, double xzScale) {
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

    private static boolean isColliding(Span span, SortedSet<Double> scales, NoiseStack.Layer[] layers) {
        for (NoiseStack.Layer layer : layers) {
            Set<Object> lattices = new HashSet<>();
            for (double scale : scales) {
                lattices.add(span.latticeCells(scale * layer.frequency()));
            }

            if (lattices.size() < scales.size()) {
                return true;
            }
        }

        return false;
    }

    private static Map<Double, Double> rungs(Span span, SortedSet<Double> ascending, double lowestFrequency) {
        Map<Double, Double> rungs = new HashMap<>();
        long previous = 0L;
        for (double scale : ascending) {
            long cells = Math.max(span.rungCells(scale * lowestFrequency), previous + 1L);
            rungs.put(scale, cells / (span.rungBlocks() * lowestFrequency));
            previous = cells;
        }

        return Map.copyOf(rungs);
    }

    private static double lowestFrequency(NoiseStack.Layer[] layers) {
        double lowest = Double.POSITIVE_INFINITY;
        for (NoiseStack.Layer layer : layers) {
            lowest = Math.min(lowest, layer.frequency());
        }

        return lowest;
    }
}

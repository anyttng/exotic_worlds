package com.exoticworlds.engine.noise;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.levelgen.synth.GradientNoise;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SmearedPerlinNoise;

public final class PeriodicOctaveSampler {
    private static final String NOT_PERLIN_LEAF = "Periodic octaves sample Perlin leaves only, got ";

    static PeriodicOctaves compile(WorldFold fold, NoiseFrame frame, double scale,
            double @Nullable [] layerFactors, NoiseStack stack) {
        CuboidAtlas atlas = fold.cuboidAtlas();
        if (atlas != null && frame.isDefault()) {
            return PeriodicOctaves.cuboid(cuboidOctaves(atlas, scale, layerFactors, stack));
        }

        LapFloor floor = LapFloor.of(fold);
        NoiseStack.Layer[] layers = stack.layers;
        PeriodicLattice[] lattices = new PeriodicLattice[layers.length];
        double[] frequencies = new double[layers.length];
        float[] amplitudes = new float[layers.length];
        for (int i = 0; i < layers.length; i++) {
            NoiseStack.Layer layer = layers[i];
            double frequency = layer.frequency();
            lattices[i] = lattice(fold, frame, layerScale(scale, layerFactors, i, frequency), floor,
                    layer.noise());
            frequencies[i] = frequency;
            amplitudes[i] = layer.amplitude();
        }

        return new PeriodicOctaves(frame.axes(), lattices, frequencies, amplitudes);
    }

    private static CuboidOctaves cuboidOctaves(CuboidAtlas atlas, double scale, double @Nullable [] layerFactors,
            NoiseStack stack) {
        NoiseStack.Layer[] layers = stack.layers;
        CuboidLattice[] lattices = new CuboidLattice[layers.length];
        double[] frequencies = new double[layers.length];
        float[] amplitudes = new float[layers.length];
        for (int i = 0; i < layers.length; i++) {
            NoiseStack.Layer layer = layers[i];
            double frequency = layer.frequency();
            lattices[i] = cuboidLattice(atlas, layerScale(scale, layerFactors, i, frequency), layer.noise());
            frequencies[i] = frequency;
            amplitudes[i] = layer.amplitude();
        }

        return new CuboidOctaves(atlas, lattices, frequencies, amplitudes);
    }

    @SuppressWarnings("deprecation")
    private static CuboidLattice cuboidLattice(CuboidAtlas atlas, double layerScale, Noise leaf) {
        if (leaf.getClass() == PerlinNoise.class) {
            PerlinNoise perlin = (PerlinNoise) leaf;
            return CuboidLattice.of(perlin.perms, perlin.offsetY, PeriodicNoiseSampler.NO_FUDGE, atlas, layerScale);
        }

        if (leaf.getClass() == SmearedPerlinNoise.class) {
            SmearedPerlinNoise smeared = (SmearedPerlinNoise) leaf;
            return CuboidLattice.of(smeared.perms, smeared.offsetY, smeared.fudgeYScale, atlas, layerScale);
        }

        throw new IllegalArgumentException(NOT_PERLIN_LEAF + leaf.getClass().getName());
    }

    static float octave(SlotAxes axes, PeriodicLattice lattice, double frequency, double x, double y, double z) {
        return PeriodicNoiseSampler.sample(lattice, slotInput(axes.x(), x, frequency),
                slotInput(axes.y(), y, frequency), slotInput(axes.z(), z, frequency), y * frequency);
    }

    private static double layerScale(double scale, double @Nullable [] layerFactors, int layer, double frequency) {
        return layerFactors == null ? scale * frequency : scale * layerFactors[layer] * frequency;
    }

    @SuppressWarnings("deprecation")
    private static PeriodicLattice lattice(WorldFold fold, NoiseFrame frame, double layerScale, LapFloor floor,
            Noise leaf) {
        if (leaf.getClass() == PerlinNoise.class) {
            PerlinNoise perlin = (PerlinNoise) leaf;
            return PeriodicNoiseSampler.lattice(perlin.perms, perlin.offsetX, perlin.offsetY, perlin.offsetZ,
                    PeriodicNoiseSampler.NO_FUDGE, fold, frame, layerScale, floor);
        }

        if (leaf.getClass() == SmearedPerlinNoise.class) {
            SmearedPerlinNoise smeared = (SmearedPerlinNoise) leaf;
            return PeriodicNoiseSampler.lattice(smeared.perms, smeared.offsetX, smeared.offsetY, smeared.offsetZ,
                    smeared.fudgeYScale, fold, frame, layerScale, floor);
        }

        throw new IllegalArgumentException(NOT_PERLIN_LEAF + leaf.getClass().getName());
    }

    private static double slotInput(SlotAxis axis, double coord, double frequency) {
        return axis.carriesWorldAxis() ? coord : GradientNoise.wrap(coord * frequency);
    }

    private PeriodicOctaveSampler() {
    }
}

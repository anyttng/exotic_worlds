package com.toroidalworld.engine.noise;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;

import net.minecraft.world.level.levelgen.synth.GradientNoise;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SmearedPerlinNoise;

public final class PeriodicOctaveSampler {
    public static float sample(WorldFold transformer, NoiseFrame frame, double scale, NoiseStack stack,
            double x, double y, double z) {
        return sample(transformer, frame, scale, null, stack, x, y, z);
    }

    public static float sample(WorldFold transformer, NoiseFrame frame, double scale, double @Nullable [] layerFactors,
            NoiseStack stack, double x, double y, double z) {
        SlotAxes axes = frame.axes();
        LapFloor floor = LapFloor.of(transformer);
        NoiseStack.Layer[] layers = stack.layers;
        float value = 0.0F;
        for (int i = 0; i < layers.length; i++) {
            NoiseStack.Layer layer = layers[i];
            double frequency = layer.frequency();
            PeriodicLattice lattice = lattice(transformer, frame, layerScale(scale, layerFactors, i, frequency),
                    floor, layer.noise());
            value += layer.amplitude() * octave(axes, lattice, frequency, x, y, z);
        }

        return value;
    }

    static PeriodicOctaves compile(WorldFold transformer, NoiseFrame frame, double scale,
            double @Nullable [] layerFactors, NoiseStack stack) {
        LapFloor floor = LapFloor.of(transformer);
        NoiseStack.Layer[] layers = stack.layers;
        PeriodicLattice[] lattices = new PeriodicLattice[layers.length];
        double[] frequencies = new double[layers.length];
        float[] amplitudes = new float[layers.length];
        for (int i = 0; i < layers.length; i++) {
            NoiseStack.Layer layer = layers[i];
            double frequency = layer.frequency();
            lattices[i] = lattice(transformer, frame, layerScale(scale, layerFactors, i, frequency), floor,
                    layer.noise());
            frequencies[i] = frequency;
            amplitudes[i] = layer.amplitude();
        }

        return new PeriodicOctaves(frame.axes(), lattices, frequencies, amplitudes);
    }

    static float octave(SlotAxes axes, PeriodicLattice lattice, double frequency, double x, double y, double z) {
        return PeriodicNoiseSampler.sample(lattice, slotInput(axes.x(), x, frequency),
                slotInput(axes.y(), y, frequency), slotInput(axes.z(), z, frequency), y * frequency);
    }

    private static double layerScale(double scale, double @Nullable [] layerFactors, int layer, double frequency) {
        return layerFactors == null ? scale * frequency : scale * layerFactors[layer] * frequency;
    }

    @SuppressWarnings("deprecation")
    private static PeriodicLattice lattice(WorldFold transformer, NoiseFrame frame, double layerScale, LapFloor floor,
            Noise leaf) {
        if (leaf.getClass() == PerlinNoise.class) {
            PerlinNoise perlin = (PerlinNoise) leaf;
            return PeriodicNoiseSampler.lattice(perlin.perms, perlin.offsetX, perlin.offsetY, perlin.offsetZ,
                    PeriodicNoiseSampler.NO_FUDGE, transformer, frame, layerScale, floor);
        }

        if (leaf.getClass() == SmearedPerlinNoise.class) {
            SmearedPerlinNoise smeared = (SmearedPerlinNoise) leaf;
            return PeriodicNoiseSampler.lattice(smeared.perms, smeared.offsetX, smeared.offsetY, smeared.offsetZ,
                    smeared.fudgeYScale, transformer, frame, layerScale, floor);
        }

        throw new IllegalArgumentException("Periodic octaves sample Perlin leaves only, got "
                + leaf.getClass().getName());
    }

    private static double slotInput(SlotAxis axis, double coord, double frequency) {
        return axis.carriesWorldAxis() ? coord : GradientNoise.wrap(coord * frequency);
    }

    private PeriodicOctaveSampler() {
    }
}

package com.exoticworlds.compat.ultraterraforged;

import com.exoticworlds.compat.terraforged.RtfLap;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.function.Interpolation;

public final class RtfOctaves {
    private static final float RIDGE_FIRST_AMPLITUDE = 2.0F;

    private static final float RIDGE_OFFSET = 1.0F;

    @FunctionalInterface
    public interface LatticeSample {
        float at(float x, float z, int seed, Interpolation interpolation);
    }

    public static float sum(RtfLap.Frame frame, LatticeSample sample, Interpolation interpolation, float x, float z,
            int firstSeed, int octaves, double frequency, float lacunarity, float gain, float firstAmplitude) {
        float sum = 0.0F;
        float amplitude = firstAmplitude;
        double octaveFrequency = frequency;
        for (int octave = 0; octave < octaves; octave++) {
            try (RtfLap.Frame.Scope lattice = frame.octave(octaveFrequency)) {
                sum += sample.at(frame.latticeU(x, z), frame.latticeV(z), firstSeed + octave,
                        interpolation) * amplitude;
            }

            octaveFrequency *= lacunarity;
            amplitude *= gain;
        }

        return sum;
    }

    public static float ridge(RtfLap.Frame frame, LatticeSample sample, Interpolation interpolation, float x, float z,
            int firstSeed, int octaves, double frequency, float lacunarity, float gain, float[] spectralWeights) {
        float amplitude = RIDGE_FIRST_AMPLITUDE;
        float value = 0.0F;
        float weight = 1.0F;
        double octaveFrequency = frequency;
        for (int octave = 0; octave < octaves; octave++) {
            float signal;
            try (RtfLap.Frame.Scope lattice = frame.octave(octaveFrequency)) {
                signal = sample.at(frame.latticeU(x, z), frame.latticeV(z), firstSeed + octave, interpolation);
            }

            signal = RIDGE_OFFSET - Math.abs(signal);
            signal *= signal;
            signal *= weight;
            weight = NoiseUtil.clamp(signal * amplitude, 0.0F, 1.0F);
            value += signal * spectralWeights[octave];
            octaveFrequency *= lacunarity;
            amplitude *= gain;
        }

        return value;
    }

    private RtfOctaves() {
    }
}

package com.exoticworlds.engine.noise;

import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.noise.BlendedNoiseFixture.Octave;
import static com.exoticworlds.engine.noise.BlendedNoiseFixture.mix;
import static com.exoticworlds.engine.noise.BlendedNoiseFixture.sample;

import java.util.Arrays;

import net.minecraft.world.level.levelgen.synth.PerlinNoise;

final class LayeredNoise {
    static final int WORLD_BLOCKS = 512;

    static final WorldFold WORLD = WorldFolds.of(
            ShapeFixtures.torus(ShapeFixtures.loopedBounds(-16, 16, -16, 16)));

    private static final double DETUNE = NoiseConstants.SECOND_LAYER_DETUNE;

    enum WalkMode { CURRENT, CORRECTED }

    @SuppressWarnings("ArrayRecordComponent")
    record NoiseParams(String name, int firstOctave, double[] amplitudes) {
        int spanOctaves() {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (int i = 0; i < amplitudes.length; i++) {
                if (amplitudes[i] != 0.0) {
                    min = Math.min(min, i);
                    max = Math.max(max, i);
                }
            }
            return max - min;
        }

        double valueFactor() {
            double expectedDeviation = 0.1 * (1.0 + 1.0 / (spanOctaves() + 1));
            return 0.16666666666666666 / expectedDeviation;
        }
    }

    record Stats(double mean, double std, double min, double max, double p05, double p95, double rough) {
        static Stats of(double[][] field) {
            int n = field.length * field[0].length;
            double sum = 0.0;
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            double[] flat = new double[n];
            int k = 0;
            for (double[] row : field) {
                for (double value : row) {
                    sum += value;
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                    flat[k++] = value;
                }
            }
            double mean = sum / n;

            double varSum = 0.0;
            for (double value : flat) {
                varSum += (value - mean) * (value - mean);
            }

            Arrays.sort(flat);
            double p05 = flat[(int) (n * 0.05)];
            double p95 = flat[(int) (n * 0.95)];

            double roughSum = 0.0;
            int roughCount = 0;
            for (int i = 0; i + 1 < field.length; i++) {
                for (int j = 0; j < field[i].length; j++) {
                    double delta = field[i + 1][j] - field[i][j];
                    roughSum += delta * delta;
                    roughCount++;
                }
            }

            return new Stats(mean, Math.sqrt(varSum / n), min, max, p05, p95, Math.sqrt(roughSum / roughCount));
        }

        String line(String label) {
            return String.format("  %-9s mean=%+.4f std=%.4f min=%+.4f max=%+.4f p05=%+.4f p95=%+.4f roughQ=%.5f",
                    label, mean, std, min, max, p05, p95, rough);
        }

        static double average(double[] values) {
            double sum = 0.0;
            for (double value : values) {
                sum += value;
            }
            return sum / values.length;
        }

        static double variance(double[] values) {
            double mean = 0.0;
            for (double value : values) {
                mean += value;
            }
            mean /= values.length;
            double varSum = 0.0;
            for (double value : values) {
                varSum += (value - mean) * (value - mean);
            }
            return varSum / values.length;
        }
    }

    @SuppressWarnings("ArrayRecordComponent")
    record CorrectedGrids(double[] vanilla, double[] wrapped, double[] corrected) {
    }

    private final NoiseParams params;
    private final Octave[][] layers;
    private final double valueFactor;

    LayeredNoise(long worldSeed, int salt, NoiseParams params) {
        this.params = params;
        this.valueFactor = params.valueFactor();
        this.layers = new Octave[2][params.amplitudes.length];
        for (int layer = 0; layer < 2; layer++) {
            for (int i = 0; i < params.amplitudes.length; i++) {
                if (params.amplitudes[i] != 0.0) {
                    long seed = mix(worldSeed + 0x9E3779B97F4A7C15L * (salt * 64L + layer * 32L + i + 1L));
                    this.layers[layer][i] = Octave.of(seed);
                }
            }
        }
    }

    // x/z already carry the caller's horizontal scale (and warp), exactly like vanilla NormalNoise.getValue input.
    double vanillaValue(double x, double z) {
        return (vanillaLayer(0, x, z) + vanillaLayer(1, x * DETUNE, z * DETUNE)) * this.valueFactor;
    }

    private double vanillaLayer(int layer, double x, double z) {
        double inputFactor = Math.pow(2.0, this.params.firstOctave);
        double valueWeight = lowestFreqValueFactor();
        double sum = 0.0;
        for (int i = 0; i < this.params.amplitudes.length; i++) {
            Octave octave = this.layers[layer][i];
            if (octave != null) {
                double octaveValue = octave.vanilla().noise(PerlinNoise.wrap(x * inputFactor), 0.0,
                        PerlinNoise.wrap(z * inputFactor), 0.0, 0.0);
                sum += this.params.amplitudes[i] * valueWeight * octaveValue;
            }

            inputFactor *= 2.0;
            valueWeight /= 2.0;
        }
        return sum;
    }

    // x/z are raw block coordinates; baseScale is what the caller would park in the generation context.
    double wrappedValue(double x, double z, double baseScale) {
        return wrappedValue(x, z, baseScale, WalkMode.CURRENT);
    }

    double correctedValue(double x, double z, double baseScale) {
        return wrappedValue(x, z, baseScale, WalkMode.CORRECTED);
    }

    double wrappedValue(double x, double z, double baseScale, WalkMode mode) {
        return (wrappedLayer(0, x, z, baseScale, mode) + wrappedLayer(1, x, z, baseScale * DETUNE, mode))
                * this.valueFactor;
    }

    private double wrappedLayer(int layer, double x, double z, double baseScale, WalkMode mode) {
        double inputFactor = Math.pow(2.0, this.params.firstOctave);
        double valueWeight = lowestFreqValueFactor();
        double sum = 0.0;
        for (int i = 0; i < this.params.amplitudes.length; i++) {
            Octave octave = this.layers[layer][i];
            if (octave != null) {
                sum += this.params.amplitudes[i] * valueWeight
                        * sample(octave.permutations(), octave.xo(), octave.yo(),
                                octave.zo(), WORLD, baseScale * inputFactor,
                                x, 0.0, z, 0.0, 0.0, mode == WalkMode.CORRECTED ? 0.0 : -1.0);
            }

            inputFactor *= 2.0;
            valueWeight /= 2.0;
        }
        return sum;
    }

    private double lowestFreqValueFactor() {
        int n = this.params.amplitudes.length;
        return Math.pow(2.0, n - 1) / (Math.pow(2.0, n) - 1.0);
    }
}

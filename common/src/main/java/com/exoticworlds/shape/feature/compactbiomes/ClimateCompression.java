package com.exoticworlds.shape.feature.compactbiomes;

import java.util.Collections;
import java.util.function.DoubleSupplier;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.ClimateScaleCompression;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class ClimateCompression {
    private static final double UNMODIFIED_AMPLITUDE = 1.0;

    private static final double LN_2 = Math.log(2.0);

    public static double[] layerFactors(WorldFold fold, boolean isClimateField, Holder<NormalNoise> noise,
            NoiseStack stack, double baseScale, double verticalShare) {
        NoiseStack.Layer[] layers = stack.layers;
        double[] factors = new double[layers.length];
        for (int i = 0; i < layers.length; i++) {
            factors[i] = factor(fold, isClimateField, noise, baseScale * detuneOf(layers[i].frequency()),
                    verticalShare);
        }

        return factors;
    }

    public static double factor(WorldFold fold, boolean isClimateField, Holder<NormalNoise> noise, double baseScale,
            double verticalShare) {
        NormalNoise.Parameters parameters = noise.value().parameters;
        return factor(fold, isClimateField, octaveAmplitudes(parameters), Math.pow(2.0, parameters.baseOctave()),
                baseScale, verticalShare);
    }

    public static double factor(WorldFold fold, boolean isClimateField, DoubleList amplitudes,
            double lowestFreqInputFactor, double baseScale, double verticalShare) {
        return factor(fold, isClimateField, verticalShare,
                () -> ClimateScaleCompression.fitted(fold, amplitudes, lowestFreqInputFactor, baseScale));
    }

    public static double factor(WorldFold fold, boolean isClimateField, double verticalShare, DoubleSupplier fitted) {
        ClimateScale scale = fold.getOptions().get(CompactBiomes.FEATURE.getOption());
        if (scale.mode() == ClimateScale.Mode.OFF
                || !ClimateScaleCompression.isCompressible(fold, verticalShare)) {
            return ClimateScaleCompression.NO_COMPRESSION;
        }

        if (scale.isFixed() && !isClimateField) {
            return ClimateScaleCompression.NO_COMPRESSION;
        }

        if (scale.mode() == ClimateScale.Mode.CUSTOM) {
            return scale.factor();
        }

        double fit = fitted.getAsDouble();
        return scale.mode() == ClimateScale.Mode.STRONG
                ? Math.max(fit, ClimateScale.STRONG_FACTOR)
                : fit;
    }

    public static DoubleList octaveAmplitudes(NormalNoise.Parameters parameters) {
        return parameters.amplitudeModifiers().isEmpty()
                ? new DoubleArrayList(Collections.nCopies(parameters.octaveCount(), UNMODIFIED_AMPLITUDE))
                : parameters.amplitudeModifiers();
    }

    private static double detuneOf(double frequency) {
        return frequency / Math.pow(2.0, (double) Math.round(Math.log(frequency) / LN_2));
    }

    private ClimateCompression() {
    }
}

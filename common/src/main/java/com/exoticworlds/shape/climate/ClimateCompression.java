package com.exoticworlds.shape.climate;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ClimateScaleCompression;
import com.exoticworlds.shape.noise.DensityNoises;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.generator.NoiseFunction;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class ClimateCompression {
    private static final double UNMODIFIED_AMPLITUDE = 1.0;

    private static final double LN_2 = Math.log(2.0);

    public static double warpDivisor(Holder<NormalNoise> noise, WorldFold fold, double xzScale,
            double verticalShare) {
        return xzScale * factor(fold, noise, xzScale, verticalShare);
    }

    public static double[] layerFactors(WorldFold fold, Holder<NormalNoise> noise, NoiseStack stack,
            double baseScale, double verticalShare) {
        NoiseStack.Layer[] layers = stack.layers;
        double[] factors = new double[layers.length];
        for (int i = 0; i < layers.length; i++) {
            factors[i] = factor(fold, noise, baseScale * detuneOf(layers[i].frequency()), verticalShare);
        }

        return factors;
    }

    public static double factor(WorldFold fold, Holder<NormalNoise> noise, double baseScale, double verticalShare) {
        NormalNoise.Parameters parameters = noise.value().parameters;
        boolean climateField = noise.unwrapKey().filter(ClimateFields::isClimate).isPresent();
        return factor(fold, climateField, octaveAmplitudes(parameters), Math.pow(2.0, parameters.baseOctave()),
                baseScale, verticalShare);
    }

    public static double factor(WorldFold fold, boolean climateField, DoubleList amplitudes,
            double lowestFreqInputFactor, double baseScale, double verticalShare) {
        ClimateScale scale = fold.generationOptions().get(CompactBiomes.OPTION);
        if (scale.mode() == ClimateScale.Mode.OFF
                || !ClimateScaleCompression.compressible(fold, verticalShare)) {
            return ClimateScaleCompression.NO_COMPRESSION;
        }

        if (scale.isFixed() && !climateField) {
            return ClimateScaleCompression.NO_COMPRESSION;
        }

        if (scale.mode() == ClimateScale.Mode.CUSTOM) {
            return scale.factor();
        }

        double fitted = ClimateScaleCompression.fitted(fold, amplitudes, lowestFreqInputFactor, baseScale);
        return scale.mode() == ClimateScale.Mode.STRONG
                ? Math.max(fitted, ClimateScale.STRONG_FACTOR)
                : fitted;
    }

    public static @Nullable NoiseFunction climateNoiseOf(DensityFunction function) {
        List<NoiseFunction> climate = DensityNoises.matching(function, ClimateFields::isClimate);
        return climate.isEmpty() ? null : climate.getFirst();
    }

    public static DoubleList octaveAmplitudes(NormalNoise.Parameters parameters) {
        return parameters.amplitudeModifiers().isEmpty()
                ? new DoubleArrayList(Collections.nCopies(parameters.octaveCount(), UNMODIFIED_AMPLITUDE))
                : parameters.amplitudeModifiers();
    }

    private static double detuneOf(double frequency) {
        return frequency / Math.pow(2.0, Math.round(Math.log(frequency) / LN_2));
    }

    private ClimateCompression() {
    }
}

package com.exoticworlds.shape.climate;

import java.util.List;
import java.util.function.DoubleSupplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.ClimateCompressionCache;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ClimateScaleCompression;
import com.exoticworlds.shape.noise.DensityNoises;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class ClimateCompression {

    public record Resolved(WorldFold fold, double baseScale, double verticalShare, double factor) {
        boolean covers(WorldFold fold, double baseScale, double verticalShare) {
            return this.fold == fold && this.baseScale == baseScale && this.verticalShare == verticalShare;
        }
    }

    public static double resolve(ClimateCompressionCache cache, WorldFold fold, boolean climateField,
            DoubleList amplitudes, double lowestFreqInputFactor, double baseScale, double verticalShare) {
        Resolved resolved = cache.toroidal$climateCompression();
        if (resolved == null || !resolved.covers(fold, baseScale, verticalShare)) {
            resolved = new Resolved(fold, baseScale, verticalShare,
                    factor(fold, climateField, amplitudes, lowestFreqInputFactor, baseScale, verticalShare));
            cache.toroidal$climateCompression(resolved);
        }

        return resolved.factor();
    }

    public static double warpDivisor(DensityFunction.NoiseHolder noise, WorldFold fold, double xzScale,
            double verticalShare) {
        return xzScale * factor(fold, noise, xzScale, verticalShare);
    }

    public static double factor(WorldFold fold, boolean climateField, DoubleList amplitudes,
            double lowestFreqInputFactor, double baseScale, double verticalShare) {
        return factor(fold, climateField, verticalShare,
                () -> ClimateScaleCompression.fitted(fold, amplitudes, lowestFreqInputFactor, baseScale));
    }

    public static double factor(WorldFold fold, boolean climateField, double verticalShare, DoubleSupplier fitted) {
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

        double fit = fitted.getAsDouble();
        return scale.mode() == ClimateScale.Mode.STRONG
                ? Math.max(fit, ClimateScale.STRONG_FACTOR)
                : fit;
    }

    public static double factor(WorldFold fold, DensityFunction.NoiseHolder noise, double baseScale,
            double verticalShare) {
        NormalNoise.NoiseParameters parameters = noise.noiseData().value();
        boolean climateField = noise.noiseData().unwrapKey().filter(ClimateFields::isClimate).isPresent();

        return factor(fold, climateField, parameters.amplitudes(), Math.pow(2.0, parameters.firstOctave()),
                baseScale, verticalShare);
    }

    public static DensityNoises.@Nullable ScaledNoise climateNoiseOf(DensityFunction function) {
        List<DensityNoises.ScaledNoise> climate = DensityNoises.scaledMatching(function, ClimateFields::isClimate);
        return climate.isEmpty() ? null : climate.getFirst();
    }

    private ClimateCompression() {
    }
}

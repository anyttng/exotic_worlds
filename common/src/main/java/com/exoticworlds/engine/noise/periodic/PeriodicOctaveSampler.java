package com.exoticworlds.engine.noise.periodic;

import com.exoticworlds.accessors.IndexableNoise;
import com.exoticworlds.accessors.WrappedImprovedNoise;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

public final class PeriodicOctaveSampler {
    public static double sample(
            Context generation,
            double baseScale,
            DoubleList octaveFactors,
            ImprovedNoise[] noiseLevels,
            DoubleList amplitudes,
            double lowestFreqInputFactor,
            double lowestFreqValueFactor,
            double x,
            double y,
            double z,
            double yScale,
            double yFudge) {
        boolean yCarriesWorldAxis = generation.getSlotAxes().y().hasWorldAxis();
        double value = 0.0;
        double factor = lowestFreqInputFactor;
        double valueFactor = lowestFreqValueFactor;

        try (Context.ScaleScope scope = generation.openScale()) {
            for (int i = 0; i < noiseLevels.length; i++) {
                ImprovedNoise noise = noiseLevels[i];
                if (noise != null) {
                    scope.rescale(baseScale * octaveFactors.getDouble(i) * factor);
                    double slotY = yCarriesWorldAxis ? y : PerlinNoise.wrap(y * factor);
                    double noiseValue = ((WrappedImprovedNoise) (Object) noise).exotic_worlds$sampleWrapped(
                            generation, x, slotY, z, yScale * factor, yFudge * factor);
                    value += amplitudes.getDouble(i) * noiseValue * valueFactor;
                }

                factor *= 2.0;
                valueFactor /= 2.0;
            }
        }

        return value;
    }

    public static boolean indexable(Context generation, double baseScale, DoubleList octaveFactors,
            ImprovedNoise[] noiseLevels, double lowestFreqInputFactor) {
        int widest = -1;
        double widestScale = 0.0;
        for (int i = 0; i < noiseLevels.length; i++) {
            double octaveScale = octaveFactors.getDouble(i) * Math.scalb(lowestFreqInputFactor, i);
            if (noiseLevels[i] != null && octaveScale >= widestScale) {
                widest = i;
                widestScale = octaveScale;
            }
        }

        if (widest < 0) {
            return true;
        }

        try (Context.ScaleScope scope = generation.openScale()) {
            scope.rescale(baseScale * widestScale);
            return ((IndexableNoise) (Object) noiseLevels[widest]).exotic_worlds$indexable(generation);
        }
    }

    private PeriodicOctaveSampler() {
    }
}

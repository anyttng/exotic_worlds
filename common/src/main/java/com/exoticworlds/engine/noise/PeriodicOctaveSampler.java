package com.exoticworlds.engine.noise;

import com.exoticworlds.accessors.IndexableNoise;
import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

public final class PeriodicOctaveSampler {
    @SuppressWarnings("deprecation")
    public static double sample(
            Context generation,
            double baseScale,
            ImprovedNoise[] noiseLevels,
            DoubleList amplitudes,
            double lowestFreqInputFactor,
            double lowestFreqValueFactor,
            double x,
            double y,
            double z,
            double yScale,
            double yFudge) {
        boolean yCarriesWorldAxis = generation.slotAxes().y().carriesWorldAxis();
        double value = 0.0;
        double factor = lowestFreqInputFactor;
        double valueFactor = lowestFreqValueFactor;

        try (Context.ScaleScope scope = generation.openScale()) {
            for (int i = 0; i < noiseLevels.length; i++) {
                ImprovedNoise noise = noiseLevels[i];
                if (noise != null) {
                    scope.rescale(baseScale * factor);
                    double slotY = yCarriesWorldAxis ? y : PerlinNoise.wrap(y * factor);
                    double noiseValue = noise.noise(x, slotY, z, yScale * factor, yFudge * factor);
                    value += amplitudes.getDouble(i) * noiseValue * valueFactor;
                }

                factor *= 2.0;
                valueFactor /= 2.0;
            }
        }

        return value;
    }

    // The period grows with the scale, so the finest octave is the first to outgrow the cell index.
    public static boolean indexable(Context generation, double baseScale, ImprovedNoise[] noiseLevels,
            double lowestFreqInputFactor) {
        int finest = noiseLevels.length - 1;
        while (finest >= 0 && noiseLevels[finest] == null) {
            finest--;
        }

        if (finest < 0) {
            return true;
        }

        try (Context.ScaleScope scope = generation.openScale()) {
            scope.rescale(baseScale * Math.scalb(lowestFreqInputFactor, finest));
            return ((IndexableNoise) (Object) noiseLevels[finest]).toroidal$indexable(generation);
        }
    }

    private PeriodicOctaveSampler() {
    }
}

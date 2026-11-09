package com.exoticworlds.shape.noise;

import com.exoticworlds.core.NoiseShaping;
import com.exoticworlds.core.NoiseShapings;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.shape.climate.ClimateCompression;
import com.exoticworlds.shape.torus.CoastFields;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class FieldNoiseShaping implements NoiseShaping {
    public static void register() {
        NoiseShapings.register(new FieldNoiseShaping());
    }

    @Override
    public double[] layerFactors(WorldFold fold, Holder<NormalNoise> noise, NoiseStack stack, double baseScale,
            double verticalShare) {
        return ClimateCompression.layerFactors(fold, noise, stack, baseScale, verticalShare);
    }

    @Override
    public double warpDivisor(WorldFold fold, Holder<NormalNoise> noise, double xzScale, double verticalShare) {
        return ClimateCompression.warpDivisor(noise, fold, xzScale, verticalShare);
    }

    @Override
    public boolean coast(Holder<NormalNoise> noise) {
        return noise.unwrapKey().filter(CoastFields::isCoast).isPresent();
    }

    private FieldNoiseShaping() {
    }
}

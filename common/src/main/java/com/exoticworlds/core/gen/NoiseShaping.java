package com.exoticworlds.core.gen;

import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public interface NoiseShaping {
    double[] layerFactors(WorldFold fold, Holder<NormalNoise> noise, NoiseStack stack, double baseScale,
            double verticalShare);

    double warpDivisor(WorldFold fold, Holder<NormalNoise> noise, double xzScale, double verticalShare);

    boolean isCoast(Holder<NormalNoise> noise);
}

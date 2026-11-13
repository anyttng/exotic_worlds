package com.exoticworlds.compat.worldgen;

import com.exoticworlds.accessors.ScaledShiftedNoise;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.exoticworlds.engine.noise.NoiseScaleLadder;
import com.exoticworlds.shape.climate.ClimateCompression;

import net.minecraft.world.level.levelgen.DensityFunction;

public record ShiftedNoiseFold(DensityFunction.NoiseHolder noise, double horizontalScale, double vanillaScale,
        double verticalShare, double shiftScale) {
    private static final double NO_Y = 0.0;

    private static final double UNSCALED_SHIFT = 1.0;

    public static ShiftedNoiseFold of(ScaledShiftedNoise source) {
        DensityFunction.NoiseHolder noise = source.exotic_worlds$noise();
        double scale = source.exotic_worlds$scale();
        double horizontalScale = NoiseScaleLadder.installedScale(noise, scale);
        return new ShiftedNoiseFold(noise, horizontalScale, scale,
                GenerationFoldContext.verticalShare(horizontalScale, NO_Y),
                source.exotic_worlds$shiftsScaled() ? scale : UNSCALED_SHIFT);
    }

    public double warpDivisor(WorldFold fold) {
        return ClimateCompression.warpDivisor(this.noise, fold, this.horizontalScale, this.verticalShare);
    }

    public double sample(Context generation, int blockX, int blockZ, double shiftX, double shiftZ,
            double warpDivisor) {
        return ContextScaledNoise.sampleWarped(generation, this.noise, blockX, NO_Y, blockZ,
                shiftX * this.shiftScale, shiftZ * this.shiftScale, warpDivisor, this.horizontalScale,
                this.vanillaScale, this.verticalShare);
    }
}

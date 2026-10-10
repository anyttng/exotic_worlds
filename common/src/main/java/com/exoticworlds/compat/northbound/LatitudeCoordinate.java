package com.exoticworlds.compat.northbound;

import com.exoticworlds.core.WrapDomain;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

record LatitudeCoordinate(WrapDomain z, int bands, double pole, double equator)
        implements DensityFunction.SimpleFunction {

    @Override
    public double compute(DensityFunction.FunctionContext context) {
        long lap = this.z.domainLength;
        long phase = Math.floorMod((long) (context.blockZ() - this.z.lowerBound) * this.bands, lap);
        double tent = 1.0 - Math.abs(2.0 * phase / lap - 1.0);
        return this.pole + (this.equator - this.pole) * tent;
    }

    @Override
    public double minValue() {
        return Math.min(this.pole, this.equator);
    }

    @Override
    public double maxValue() {
        return Math.max(this.pole, this.equator);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        throw new UnsupportedOperationException("Calling .codec() on LatitudeCoordinate");
    }
}

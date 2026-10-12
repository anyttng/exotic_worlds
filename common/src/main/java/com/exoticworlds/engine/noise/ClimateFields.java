package com.exoticworlds.engine.noise;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;

public final class ClimateFields {
    public static Climate.TargetPoint at(Climate.Sampler sampler, int quartX, int quartY, int quartZ) {
        DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(
                QuartPos.toBlock(quartX), QuartPos.toBlock(quartY), QuartPos.toBlock(quartZ));
        return Climate.target(
                (float) sampler.temperature().compute(context),
                (float) sampler.humidity().compute(context),
                (float) sampler.continentalness().compute(context),
                (float) sampler.erosion().compute(context),
                (float) sampler.depth().compute(context),
                (float) sampler.weirdness().compute(context));
    }

    private ClimateFields() {
    }
}

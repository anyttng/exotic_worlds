package com.exoticworlds.engine.noise;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Climate;

public final class ClimateTargets {
    public static Climate.TargetPoint at(Climate.Sampler sampler, int quartX, int quartY, int quartZ) {
        int blockX = QuartPos.toBlock(quartX);
        int blockY = QuartPos.toBlock(quartY);
        int blockZ = QuartPos.toBlock(quartZ);
        return Climate.target(
                sampler.temperature().sampleValue(blockX, blockY, blockZ),
                sampler.humidity().sampleValue(blockX, blockY, blockZ),
                sampler.continentalness().sampleValue(blockX, blockY, blockZ),
                sampler.erosion().sampleValue(blockX, blockY, blockZ),
                sampler.depth().sampleValue(blockX, blockY, blockZ),
                sampler.weirdness().sampleValue(blockX, blockY, blockZ));
    }

    private ClimateTargets() {
    }
}

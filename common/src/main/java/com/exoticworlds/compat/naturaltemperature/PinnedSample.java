package com.exoticworlds.compat.naturaltemperature;

import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

public final class PinnedSample implements DensitySampler {
    private final float value;

    private PinnedSample(float value) {
        this.value = value;
    }

    public static DensitySampler.Bound at(DensitySampler.Bound field, int blockX, int blockY, int blockZ) {
        return new DensitySampler.Bound(new PinnedSample(field.sampleValue(blockX, blockY, blockZ)), field.context());
    }

    @Override
    public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
        return this.value;
    }

    @Override
    public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
        DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
    }
}

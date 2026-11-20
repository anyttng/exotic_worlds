package com.exoticworlds.compat.naturaltemperature;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

public final class PinnedSample implements DensitySampler {
    private final float value;

    private final ClimateLap lap;

    private final int blockZ;

    private PinnedSample(float value, ClimateLap lap, int blockZ) {
        this.value = value;
        this.lap = lap;
        this.blockZ = blockZ;
    }

    public static DensitySampler.Bound at(DensitySampler.Bound field, ClimateLap lap, int blockX, int blockY,
            int blockZ) {
        return new DensitySampler.Bound(new PinnedSample(field.sampleValue(blockX, blockY, blockZ), lap, blockZ),
                field.context());
    }

    public static @Nullable PinnedSample of(DensitySampler.Bound field) {
        return field.sampler() instanceof PinnedSample pinned ? pinned : null;
    }

    public ClimateLap lap() {
        return this.lap;
    }

    public int blockZ() {
        return this.blockZ;
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

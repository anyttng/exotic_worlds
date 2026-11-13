package com.exoticworlds.compat.lithostitched;

import com.mojang.serialization.MapCodec;

import dev.worldgen.lithostitched.api.worldgen.densityfunction.fastnoise.FastNoiseConfig;

public final class LapFastNoiseConfig extends FastNoiseConfig {
    private final LapFastNoise noise;

    public LapFastNoiseConfig(FastNoiseConfig source, LapFastNoise noise) {
        super(source.frequency(), source.salt());
        this.noise = noise;
    }

    @Override
    public double sample(double x, double y, double z) {
        return this.noise.get(x, y, z);
    }

    @Override
    public MapCodec<? extends FastNoiseConfig> getCodec() {
        throw new UnsupportedOperationException("[ew/lithostitched-compat] a lap fast noise config is never encoded");
    }
}

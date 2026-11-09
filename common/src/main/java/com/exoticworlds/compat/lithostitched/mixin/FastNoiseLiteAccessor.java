package com.exoticworlds.compat.lithostitched.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.worldgen.lithostitched.api.worldgen.densityfunction.fastnoise.FNL;

@Mixin(FNL.class)
public interface FastNoiseLiteAccessor {
    @Accessor("PrimeX")
    static int exotic_worlds$primeX() {
        throw new AssertionError();
    }

    @Accessor("PrimeY")
    static int exotic_worlds$primeY() {
        throw new AssertionError();
    }

    @Accessor("PrimeZ")
    static int exotic_worlds$primeZ() {
        throw new AssertionError();
    }

    @Accessor("mSeed")
    int exotic_worlds$seed();

    @Accessor("mFrequency")
    float exotic_worlds$frequency();

    @Accessor("mNoiseType")
    FNL.NoiseType exotic_worlds$noiseType();

    @Accessor("mRotationType3D")
    FNL.RotationType3D exotic_worlds$rotationType3d();

    @Accessor("mFractalType")
    FNL.FractalType exotic_worlds$fractalType();

    @Accessor("mOctaves")
    int exotic_worlds$octaves();

    @Accessor("mLacunarity")
    float exotic_worlds$lacunarity();

    @Accessor("mGain")
    float exotic_worlds$gain();

    @Accessor("mWeightedStrength")
    float exotic_worlds$weightedStrength();

    @Accessor("mPingPongStrength")
    float exotic_worlds$pingPongStrength();

    @Accessor("mFractalBounding")
    float exotic_worlds$fractalBounding();

    @Accessor("mCellularDistanceFunction")
    FNL.CellularDistanceFunction exotic_worlds$cellularDistanceFunction();

    @Accessor("mCellularReturnType")
    FNL.CellularReturnType exotic_worlds$cellularReturnType();

    @Accessor("mCellularJitterModifier")
    float exotic_worlds$cellularJitterModifier();
}

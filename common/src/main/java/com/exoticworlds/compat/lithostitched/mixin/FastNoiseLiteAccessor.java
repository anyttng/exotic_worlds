package com.exoticworlds.compat.lithostitched.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.worldgen.lithostitched.api.worldgen.densityfunction.fastnoise.FNL;

@Mixin(FNL.class)
public interface FastNoiseLiteAccessor {
    @Accessor("PrimeX")
    static int exotic_worlds$getPrimeX() {
        throw new AssertionError();
    }

    @Accessor("PrimeY")
    static int exotic_worlds$getPrimeY() {
        throw new AssertionError();
    }

    @Accessor("PrimeZ")
    static int exotic_worlds$getPrimeZ() {
        throw new AssertionError();
    }

    @Accessor("mSeed")
    int exotic_worlds$getSeed();

    @Accessor("mFrequency")
    float exotic_worlds$getFrequency();

    @Accessor("mNoiseType")
    FNL.NoiseType exotic_worlds$getNoiseType();

    @Accessor("mRotationType3D")
    FNL.RotationType3D exotic_worlds$getRotationType3d();

    @Accessor("mFractalType")
    FNL.FractalType exotic_worlds$getFractalType();

    @Accessor("mOctaves")
    int exotic_worlds$getOctaves();

    @Accessor("mLacunarity")
    float exotic_worlds$getLacunarity();

    @Accessor("mGain")
    float exotic_worlds$getGain();

    @Accessor("mWeightedStrength")
    float exotic_worlds$getWeightedStrength();

    @Accessor("mPingPongStrength")
    float exotic_worlds$getPingPongStrength();

    @Accessor("mCellularDistanceFunction")
    FNL.CellularDistanceFunction exotic_worlds$getCellularDistanceFunction();

    @Accessor("mCellularReturnType")
    FNL.CellularReturnType exotic_worlds$getCellularReturnType();

    @Accessor("mCellularJitterModifier")
    float exotic_worlds$getCellularJitterModifier();
}

package com.exoticworlds.compat.lithostitched.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.lithostitched.FastNoiseLap;
import com.exoticworlds.compat.lithostitched.FastNoiseLapHolder;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import dev.worldgen.lithostitched.api.worldgen.densityfunction.fastnoise.FNL;

@Mixin(FNL.class)
public class FastNoiseLiteMixin implements FastNoiseLapHolder {
    private static final String OWNER = "Ldev/worldgen/lithostitched/api/worldgen/densityfunction/fastnoise/FNL;";

    private static final String GRAD_COORD = OWNER + "GradCoord(IIIIFFF)F";

    private static final String HASH = OWNER + "Hash(IIII)I";

    private static final String LACUNARITY = OWNER + "mLacunarity:F";

    @Unique
    private @Nullable FastNoiseLap exotic_worlds$lap;

    @Override
    public @Nullable FastNoiseLap exotic_worlds$lap() {
        return this.exotic_worlds$lap;
    }

    @Override
    public void exotic_worlds$setLap(FastNoiseLap lap) {
        this.exotic_worlds$lap = lap;
    }

    @WrapOperation(method = {"SinglePerlin(IDDD)F", "SingleOpenSimplex2S(IDDD)F"},
            at = @At(value = "INVOKE", target = GRAD_COORD))
    private float exotic_worlds$closedGradient(int seed, int xPrimed, int yPrimed, int zPrimed, float xd, float yd,
            float zd, Operation<Float> original, @Local(argsOnly = true, ordinal = 0) int octaveSeed) {
        FastNoiseLap lap = this.exotic_worlds$lap;
        if (lap == null) {
            return original.call(seed, xPrimed, yPrimed, zPrimed, xd, yd, zd);
        }

        return original.call(seed, lap.reduceX(octaveSeed, xPrimed, yPrimed, zPrimed),
                lap.reduceY(octaveSeed, xPrimed, yPrimed, zPrimed), lap.reduceZ(octaveSeed, xPrimed, yPrimed, zPrimed),
                xd, yd, zd);
    }

    @WrapOperation(method = "SingleCellular(IDDD)F", at = @At(value = "INVOKE", target = HASH))
    private int exotic_worlds$closedCell(int seed, int xPrimed, int yPrimed, int zPrimed, Operation<Integer> original,
            @Local(argsOnly = true, ordinal = 0) int octaveSeed) {
        FastNoiseLap lap = this.exotic_worlds$lap;
        if (lap == null) {
            return original.call(seed, xPrimed, yPrimed, zPrimed);
        }

        return original.call(seed, lap.reduceX(octaveSeed, xPrimed, yPrimed, zPrimed),
                lap.reduceY(octaveSeed, xPrimed, yPrimed, zPrimed), lap.reduceZ(octaveSeed, xPrimed, yPrimed, zPrimed));
    }

    @ModifyExpressionValue(method = {"GenFractalFBm(DDD)F", "GenFractalRidged(DDD)F", "GenFractalPingPong(DDD)F"},
            at = @At(value = "FIELD", target = LACUNARITY))
    private float exotic_worlds$closedOctaveStep(float lacunarity, @Local(name = "i") int octave) {
        FastNoiseLap lap = this.exotic_worlds$lap;
        return lap == null ? lacunarity : lap.step(octave);
    }
}

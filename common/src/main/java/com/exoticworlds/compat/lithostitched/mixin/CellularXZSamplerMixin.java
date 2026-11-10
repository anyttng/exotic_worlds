package com.exoticworlds.compat.lithostitched.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.lithostitched.CellLap;
import com.exoticworlds.compat.lithostitched.CellLapHolder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.worldgen.lithostitched.impl.worldgen.densityfunction.CellularDensityFunction;
import net.minecraft.world.level.levelgen.WorldgenRandom;

@Mixin(CellularDensityFunction.XZSampler.class)
public class CellularXZSamplerMixin implements CellLapHolder {
    @Unique
    private @Nullable CellLap exotic_worlds$cellLap;

    @Override
    public @Nullable CellLap exotic_worlds$cellLap() {
        return this.exotic_worlds$cellLap;
    }

    @Override
    public void exotic_worlds$setCellLap(CellLap lap) {
        this.exotic_worlds$cellLap = lap;
    }

    @WrapOperation(method = InjectionTargets.DENSITY_SAMPLER_SAMPLE_VALUE, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;setLargeFeatureWithSalt(JIII)V"))
    private void exotic_worlds$closedCellSeed(WorldgenRandom random, long seed, int cellX, int cellZ, int salt,
            Operation<Void> original) {
        CellLap lap = this.exotic_worlds$cellLap;
        if (lap == null) {
            original.call(random, seed, cellX, cellZ, salt);
            return;
        }

        original.call(random, seed, lap.foldX(cellX, cellZ), lap.foldZ(cellZ), salt);
    }
}

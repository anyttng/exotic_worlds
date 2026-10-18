package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.AquiferCellsHolder;
import com.exoticworlds.engine.noise.AquiferCells;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;

@Mixin(targets = "net.minecraft.world.level.levelgen.Aquifer$NoiseBasedAquifer")
public class AquiferFluidLevelSeamMixin implements AquiferCellsHolder {
    @Unique
    private @Nullable AquiferCells exotic_worlds$aquiferCells;

    @WrapOperation(
            method = "computeRandomizedFluidSurfaceLevel",
            at = @At(value = "INVOKE", target = InjectionTargets.DENSITY_SAMPLER_BOUND_SAMPLE_VALUE))
    private float exotic_worlds$fluidLevelFromTilingCell(DensitySampler.Bound noise, int cellX, int cellY, int cellZ,
            Operation<Float> original,
            @Local(argsOnly = true, ordinal = 0) int blockX,
            @Local(argsOnly = true, ordinal = 2) int blockZ) {
        AquiferCells cells = this.exotic_worlds$aquiferCells;
        return cells == null ? original.call(noise, cellX, cellY, cellZ) : cells.level(blockX, cellY, blockZ);
    }

    @Override
    public @Nullable AquiferCells exotic_worlds$aquiferCells() {
        return this.exotic_worlds$aquiferCells;
    }

    @Override
    public void exotic_worlds$aquiferCells(AquiferCells cells) {
        this.exotic_worlds$aquiferCells = cells;
    }
}

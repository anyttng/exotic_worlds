package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.AquiferCellsHolder;
import com.exoticworlds.engine.noise.AquiferCells;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;

@Mixin(targets = "net.minecraft.world.level.levelgen.Aquifer$NoiseBasedAquifer")
public class AquiferFluidLevelSeamMixin implements AquiferCellsHolder {
    @Unique
    private @Nullable AquiferCells toroidal$aquiferCells;

    @WrapOperation(
            method = "computeRandomizedFluidSurfaceLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/densityfunction/DensitySampler$Bound;sampleValue(III)F"))
    private float toroidal$fluidLevelFromTilingCell(DensitySampler.Bound noise, int cellX, int cellY, int cellZ,
            Operation<Float> original,
            @Local(argsOnly = true, ordinal = 0) int blockX,
            @Local(argsOnly = true, ordinal = 2) int blockZ) {
        AquiferCells cells = this.toroidal$aquiferCells;
        return cells == null ? original.call(noise, cellX, cellY, cellZ) : cells.level(blockX, cellY, blockZ);
    }

    @Override
    public @Nullable AquiferCells toroidal$aquiferCells() {
        return this.toroidal$aquiferCells;
    }

    @Override
    public void toroidal$aquiferCells(AquiferCells cells) {
        this.toroidal$aquiferCells = cells;
    }
}

package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.AquiferCellsHolder;
import com.exoticworlds.engine.noise.AquiferCells;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;

// C2ME @Overwrites computeFluidType at priority 1100; applying below that loses this wrap under C2ME.
@Mixin(targets = "net.minecraft.world.level.levelgen.Aquifer$NoiseBasedAquifer", priority = 1200)
public class AquiferFluidTypeSeamMixin {
    @WrapOperation(
            method = "computeFluidType",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/densityfunction/DensitySampler$Bound;sampleValue(III)F"))
    private float toroidal$fluidTypeFromTilingCell(DensitySampler.Bound noise, int cellX, int cellY, int cellZ,
            Operation<Float> original,
            @Local(argsOnly = true, ordinal = 0) int blockX,
            @Local(argsOnly = true, ordinal = 2) int blockZ) {
        AquiferCells cells = ((AquiferCellsHolder) this).toroidal$aquiferCells();
        return cells == null ? original.call(noise, cellX, cellY, cellZ) : cells.type(blockX, cellY, blockZ);
    }
}

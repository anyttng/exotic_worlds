package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.reterraforged.ReTerraForgedInjectionTargets;
import com.exoticworlds.compat.reterraforged.RtfEntry;

import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(targets = "raccoonman.reterraforged.world.worldgen.densityfunction.CellSampler$CacheChunk")
public abstract class CellSamplerCacheChunkMixin {
    @WrapOperation(method = {ReTerraForgedInjectionTargets.COMPUTE, ReTerraForgedInjectionTargets.COMPUTE_INTERMEDIARY},
            at = @At(value = "INVOKE", target = InjectionTargets.FUNCTION_CONTEXT_BLOCK_X))
    private int toroidal$foldX(DensityFunction.FunctionContext context, Operation<Integer> original) {
        return RtfEntry.foldX(RtfEntry.generationFold(), original.call(context), context.blockZ());
    }

    @WrapOperation(method = {ReTerraForgedInjectionTargets.COMPUTE, ReTerraForgedInjectionTargets.COMPUTE_INTERMEDIARY},
            at = @At(value = "INVOKE", target = InjectionTargets.FUNCTION_CONTEXT_BLOCK_Z))
    private int toroidal$foldZ(DensityFunction.FunctionContext context, Operation<Integer> original) {
        return RtfEntry.foldZ(RtfEntry.generationFold(), original.call(context));
    }
}

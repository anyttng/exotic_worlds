package com.exoticworlds.compat.lithostitched.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.lithostitched.CellLap;
import com.exoticworlds.compat.lithostitched.CellLapHolder;
import com.exoticworlds.engine.noise.FoldedCompileContext;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import dev.worldgen.lithostitched.impl.worldgen.densityfunction.CellularDensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;

@Mixin(CellularDensityFunction.class)
public class CellularDensityFunctionCompileMixin {
    private static final String GRID_DIMENSIONS = "Ldev/worldgen/lithostitched/impl/worldgen/densityfunction/"
            + "CellularDensityFunction;gridDimensions:Ldev/worldgen/lithostitched/impl/worldgen/densityfunction/"
            + "CellularDensityFunction$GridDimensions;";

    @ModifyExpressionValue(method = InjectionTargets.DENSITY_FUNCTION_COMPILE_SAMPLER,
            at = @At(value = "FIELD", target = GRID_DIMENSIONS))
    private CellularDensityFunction.GridDimensions exotic_worlds$closedGrid(
            CellularDensityFunction.GridDimensions dimensions,
            @Local(argsOnly = true) DensityFunction.CompileContext context) {
        FoldedCompileContext folded = FoldedCompileContext.of(context);
        if (folded == null) {
            return dimensions;
        }

        return new CellularDensityFunction.GridDimensions(
                CellLap.of(folded.fold(), dimensions.horizontal()).width(), dimensions.vertical());
    }

    @ModifyReturnValue(method = InjectionTargets.DENSITY_FUNCTION_COMPILE_SAMPLER, at = @At("RETURN"))
    private DensitySampler exotic_worlds$markClosedGrid(DensitySampler sampler,
            @Local(argsOnly = true) DensityFunction.CompileContext context) {
        FoldedCompileContext folded = FoldedCompileContext.of(context);
        if (folded != null && sampler instanceof CellLapHolder holder) {
            holder.exotic_worlds$setCellLap(CellLap.of(folded.fold(),
                    ((CellularDensityFunction) (Object) this).gridDimensions().horizontal()));
        }

        return sampler;
    }
}

package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.noise.FoldedCompileContext;
import com.exoticworlds.engine.noise.FoldedSamplers;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.generator.DistanceToPointFunction;

@Mixin(DistanceToPointFunction.class)
public class DistanceToPointFunctionCompileMixin {
    @WrapMethod(method = InjectionTargets.DENSITY_FUNCTION_COMPILE_SAMPLER)
    private DensitySampler toroidal$compileFolded(DensityFunction.CompileContext context,
            Operation<DensitySampler> original) {
        if (!(context instanceof FoldedCompileContext folded)) {
            return original.call(context);
        }

        DistanceToPointFunction self = (DistanceToPointFunction) (Object) this;
        return FoldedSamplers.distanceToPoint(folded, self.point(), self.metric());
    }
}

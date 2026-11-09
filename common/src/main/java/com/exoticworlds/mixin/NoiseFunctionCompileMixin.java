package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.noise.FoldedCompileContext;
import com.exoticworlds.engine.noise.FoldedSamplers;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.generator.NoiseFunction;

@Mixin(NoiseFunction.class)
public class NoiseFunctionCompileMixin {
    @SuppressWarnings("deprecation")
    @WrapMethod(method = InjectionTargets.DENSITY_FUNCTION_COMPILE_SAMPLER)
    private DensitySampler exotic_worlds$compileFolded(DensityFunction.CompileContext context,
            Operation<DensitySampler> original) {
        FoldedCompileContext folded = FoldedCompileContext.of(context);
        if (folded == null) {
            return original.call(context);
        }

        NoiseFunction self = (NoiseFunction) (Object) this;
        return FoldedSamplers.compileNoise(folded, self.noise(), self.xzScale(), self.yScale(), self.shiftX(),
                self.shiftY(), self.shiftZ());
    }
}

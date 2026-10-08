package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.noise.FoldedCompileContext;
import com.exoticworlds.engine.noise.FoldedSamplers;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;

@Mixin(BlendedNoise.class)
public class BlendedNoiseCompileMixin {
    @WrapMethod(method = InjectionTargets.DENSITY_FUNCTION_COMPILE_SAMPLER)
    private DensitySampler toroidal$compileFolded(DensityFunction.CompileContext context,
            Operation<DensitySampler> original) {
        return context instanceof FoldedCompileContext folded
                ? FoldedSamplers.blended(folded, (BlendedNoise) (Object) this)
                : original.call(context);
    }
}

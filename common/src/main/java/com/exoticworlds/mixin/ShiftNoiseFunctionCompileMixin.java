package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.noise.FoldedCompileContext;
import com.exoticworlds.engine.noise.FoldedSamplers;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.generator.ShiftNoiseFunction;

@Mixin({ShiftNoiseFunction.Shift.class, ShiftNoiseFunction.ShiftA.class, ShiftNoiseFunction.ShiftB.class})
public class ShiftNoiseFunctionCompileMixin {
    @WrapMethod(method = InjectionTargets.DENSITY_FUNCTION_COMPILE_SAMPLER)
    private DensitySampler toroidal$compileFolded(DensityFunction.CompileContext context,
            Operation<DensitySampler> original) {
        if (!(context instanceof FoldedCompileContext folded)) {
            return original.call(context);
        }

        return switch ((Object) this) {
            case ShiftNoiseFunction.Shift shift -> FoldedSamplers.shift(folded, shift.offsetNoise());
            case ShiftNoiseFunction.ShiftA shiftA -> FoldedSamplers.shiftA(folded, shiftA.offsetNoise());
            case ShiftNoiseFunction.ShiftB shiftB -> FoldedSamplers.shiftB(folded, shiftB.offsetNoise());
            default -> original.call(context);
        };
    }
}

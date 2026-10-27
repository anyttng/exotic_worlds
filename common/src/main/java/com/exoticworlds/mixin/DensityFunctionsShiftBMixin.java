package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.DensityFunctionSlotAxes;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$ShiftB")
public class DensityFunctionsShiftBMixin {
    @Shadow
    @Final
    private DensityFunction.NoiseHolder offsetNoise;

    @WrapMethod(method = "compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D")
    private double exotic_worlds$periodicCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        Context generation = GenerationFoldContext.wrappedContext();
        if (generation == null) {
            return original.call(context);
        }

        try (Context.BindingScope _ = generation.bind(generation.fold(), DensityFunctionSlotAxes.SHIFT_B,
                NoiseConstants.SHIFT_SCALE, GenerationFoldContext.UNDECLARED_VERTICAL_SHARE)) {
            return ContextScaledNoise.wrappedValue(generation, this.offsetNoise, context.blockZ(), context.blockX(),
                    0.0) * NoiseConstants.SHIFT_AMPLITUDE;
        }
    }
}

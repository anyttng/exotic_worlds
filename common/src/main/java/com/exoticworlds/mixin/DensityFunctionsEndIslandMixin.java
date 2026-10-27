package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.exoticworlds.engine.noise.PeriodicEndIslands;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

// C2ME @Overwrites compute at priority 1100; applying below that loses this wrap under C2ME.
@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$EndIslandDensityFunction", priority = 1200)
public class DensityFunctionsEndIslandMixin {
    @Shadow
    @Final
    private SimplexNoise islandNoise;

    @WrapMethod(method = "compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D")
    private double exotic_worlds$loopedCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        Context generation = GenerationFoldContext.wrappedContext();
        if (generation == null) {
            return original.call(context);
        }

        return PeriodicEndIslands.density(PeriodicEndIslands.heightValue(this.islandNoise, generation.fold(),
                context.blockX(), context.blockZ()));
    }
}

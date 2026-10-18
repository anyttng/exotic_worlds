package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.engine.noise.DensityRewrites;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

@Mixin(DensityFunctions.HolderHolder.class)
public class DensityFunctionsHolderHolderMixin {
    @Shadow
    @Final
    private Holder<DensityFunction> function;

    @WrapMethod(method = "mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)"
            + "Lnet/minecraft/world/level/levelgen/DensityFunction;")
    private DensityFunction exotic_worlds$rewriteKeyed(DensityFunction.Visitor visitor,
            Operation<DensityFunction> original) {
        DensityFunction rewritten = DensityRewrites.rewriteKeyed(visitor, this.function);
        return rewritten != null ? rewritten : original.call(visitor);
    }
}

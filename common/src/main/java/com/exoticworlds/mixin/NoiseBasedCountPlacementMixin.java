package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.FoldedBiomeInfo;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.NoiseBasedCountPlacement;

@Mixin(NoiseBasedCountPlacement.class)
public class NoiseBasedCountPlacementMixin {
    @Shadow
    @Final
    private double noiseFactor;

    @Shadow
    @Final
    private double noiseOffset;

    @Shadow
    @Final
    private int noiseToCountRatio;

    @WrapMethod(method = "count(Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)I")
    private int toroidal$foldedCount(RandomSource random, BlockPos origin, Operation<Integer> original) {
        Context generation = GenerationTransformerContext.context();
        WorldFold transformer = generation.wrappedTransformer();
        if (transformer == null) {
            return original.call(random, origin);
        }

        double flowerNoise = FoldedBiomeInfo.sample(generation, transformer, this.noiseFactor, origin.getX(),
                origin.getZ());
        return (int) Math.ceil((flowerNoise + this.noiseOffset) * this.noiseToCountRatio);
    }
}

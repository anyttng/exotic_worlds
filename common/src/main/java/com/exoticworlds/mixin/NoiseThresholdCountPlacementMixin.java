package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.FoldedBiomeInfo;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.NoiseThresholdCountPlacement;

@Mixin(NoiseThresholdCountPlacement.class)
public class NoiseThresholdCountPlacementMixin {
    @Shadow
    @Final
    private double noiseLevel;

    @Shadow
    @Final
    private int belowNoise;

    @Shadow
    @Final
    private int aboveNoise;

    @WrapMethod(method = "count(Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)I")
    private int toroidal$foldedCount(RandomSource random, BlockPos origin, Operation<Integer> original) {
        Context generation = GenerationTransformerContext.context();
        WorldFold transformer = generation.wrappedTransformer();
        if (transformer == null) {
            return original.call(random, origin);
        }

        double flowerNoise = FoldedBiomeInfo.sample(generation, transformer,
                NoiseConstants.BIOME_INFO_THRESHOLD_DIVISOR, origin.getX(), origin.getZ());
        return flowerNoise < this.noiseLevel ? this.belowNoise : this.aboveNoise;
    }
}

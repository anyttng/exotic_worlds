package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.FoldedPlacementCounts;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
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
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return original.call(random, origin);
        }

        return FoldedPlacementCounts.noiseThreshold(transformer, origin.getX(), origin.getZ(),
                this.noiseLevel, this.belowNoise, this.aboveNoise);
    }
}

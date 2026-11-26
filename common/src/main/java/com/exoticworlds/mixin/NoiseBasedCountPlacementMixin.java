package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.FoldedPlacementCounts;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.periodic.SimplexSlot;
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

    @Unique
    private final SimplexSlot exotic_worlds$slot = new SimplexSlot();

    @WrapMethod(method = "count(Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)I")
    private int exotic_worlds$foldedCount(RandomSource random, BlockPos origin, Operation<Integer> original) {
        WorldFold fold = GenerationFoldContext.getContext().getWrappedFold();
        if (fold == null) {
            return original.call(random, origin);
        }

        return FoldedPlacementCounts.noiseBased(this.exotic_worlds$slot, fold, origin.getX(), origin.getZ(),
                this.noiseFactor, this.noiseOffset, this.noiseToCountRatio);
    }
}

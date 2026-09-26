package com.toroidalworld.mixin;

import java.util.Optional;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;

@Mixin(MoveThroughVillageGoal.class)
public class MoveThroughVillageGoalMixin {
    @Shadow
    @Final
    protected PathfinderMob mob;

    @WrapOperation(
            method = {"canContinueToUse", "stop"},
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private boolean toroidal$poiArrivalThroughSeam(BlockPos poiPos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(this.mob, poiPos, bodyPosition, distance);
    }

    @WrapOperation(
            method = {"lambda$canUse$2", "method_19053"},
            at = @At(value = "INVOKE",
                    target = "Ljava/util/Optional;map(Ljava/util/function/Function;)Ljava/util/Optional;"))
    private Optional<Double> toroidal$poiScoreThroughSeam(Optional<BlockPos> poiPos,
            Function<BlockPos, Double> score, Operation<Optional<Double>> original,
            @Local(argsOnly = true, ordinal = 0) BlockPos bodyPos) {
        Function<BlockPos, Double> scoreThroughSeam = poi -> -SeamRange.sqr(this.mob, poi, bodyPos);
        return original.call(poiPos, scoreThroughSeam);
    }
}

package com.exoticworlds.mixin;

import java.util.Optional;
import java.util.function.BiPredicate;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeePollinateGoal")
public class BeePollinateGoalMixin {
    @Shadow(aliases = "this$0")
    @Final
    private Bee bee;

    @ModifyArg(
            method = "findNearbyFlower",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/blockscan/OrderedBlockMatcher;findFirst"
                            + "(Ljava/util/function/BiPredicate;)Ljava/util/Optional;"),
            index = 0)
    private BiPredicate<BlockPos, BlockState> exotic_worlds$flowerCandidatesThroughSeam(
            BiPredicate<BlockPos, BlockState> byRawPos) {
        WorldFold transformer = ((TransformerSource) this.bee).exotic_worlds$wrappedTransformer();
        if (transformer == null) {
            return byRawPos;
        }

        return (pos, state) -> byRawPos.test(transformer.fold(pos), state);
    }

    @ModifyReturnValue(method = "findNearbyFlower", at = @At("RETURN"))
    private Optional<BlockPos> exotic_worlds$foundFlowerCanonical(Optional<BlockPos> found) {
        return found.map(WorldLoopAttachments.transformerOf(this.bee.level())::fold);
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_AT_BOTTOM_CENTER_OF))
    private Vec3 exotic_worlds$flowerThroughSeam(Vec3 flowerPos) {
        return SeamSteering.nearestCopy(this.bee, flowerPos);
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO, ordinal = 1))
    private double exotic_worlds$hoverArrivalThroughSeam(Vec3 beePos, Vec3 hoverPos, Operation<Double> original) {
        return original.call(beePos, SeamSteering.nearestCopy(this.bee, hoverPos));
    }
}

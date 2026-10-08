package com.exoticworlds.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.StrollThroughVillageGoal;
import net.minecraft.world.phys.Vec3;

@Mixin(StrollThroughVillageGoal.class)
public class StrollThroughVillageGoalMixin {
    @Shadow
    @Final
    private PathfinderMob mob;

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private boolean toroidal$strollGateThroughSeam(BlockPos wantedPos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(this.mob, wantedPos, bodyPosition, distance);
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_AT_BOTTOM_CENTER_OF))
    private Vec3 toroidal$strollTargetThroughSeam(Vec3 strollTarget) {
        return SeamSteering.nearestCopy(this.mob, strollTarget);
    }

    @ModifyExpressionValue(
            method = "canContinueToUse",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/ai/goal/StrollThroughVillageGoal;"
                            + "wantedPos:Lnet/minecraft/core/BlockPos;",
                    opcode = Opcodes.GETFIELD, ordinal = 1))
    private BlockPos toroidal$strollIdentityThroughSeam(BlockPos wantedPos) {
        return SeamSteering.nearestCopy(this.mob, wantedPos);
    }
}

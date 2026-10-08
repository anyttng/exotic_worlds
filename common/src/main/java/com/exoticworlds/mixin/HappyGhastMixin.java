package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.phys.Vec3;

@Mixin(HappyGhast.class)
public class HappyGhastMixin {
    @ModifyExpressionValue(
            method = "scanPlayerAboveGhast",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_POSITION))
    private Vec3 toroidal$riderPositionThroughSeam(Vec3 position) {
        return SeamSteering.nearestCopy((HappyGhast) (Object) this, position);
    }

    @WrapOperation(
            method = "checkRestriction",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_CLOSER_THAN))
    private boolean toroidal$homeRestrictionThroughSeam(BlockPos home, Vec3i pos, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerThan((HappyGhast) (Object) this, home, pos, distance);
    }
}

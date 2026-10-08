package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonHoldingPatternPhase;
import net.minecraft.world.phys.Vec3;

@Mixin(DragonHoldingPatternPhase.class)
public class DragonHoldingPatternPhaseMixin {
    @WrapOperation(
            method = "findNewTarget",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_DIST_TO_CENTER_SQR))
    private double toroidal$eggDistanceThroughSeam(BlockPos eggPos, Position playerPosition,
            Operation<Double> original) {
        return SeamRange.sqr(((DragonPhaseAccessor) this).toroidal$dragon(), Vec3.atCenterOf(eggPos), playerPosition);
    }

    @WrapOperation(
            method = "doServerTick",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR_XYZ))
    private double toroidal$targetWindowThroughSeam(Vec3 target, double x, double y, double z,
            Operation<Double> original) {
        return SeamRange.sqr(((DragonPhaseAccessor) this).toroidal$dragon(), target, x, y, z);
    }
}

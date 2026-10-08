package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(BeehiveBlockEntity.class)
public class BeehiveBlockEntityMixin {
    @WrapOperation(
            method = "emptyAllLivingFromHive",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR))
    private double toroidal$releaseGateThroughSeam(Vec3 harvesterPosition, Vec3 releasedPosition,
            Operation<Double> original, @Local(argsOnly = true) Player harvester) {
        return SeamRange.sqr(harvester, harvesterPosition, releasedPosition);
    }
}

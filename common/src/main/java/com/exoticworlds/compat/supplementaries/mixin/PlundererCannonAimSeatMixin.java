package com.exoticworlds.compat.supplementaries.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.entities.goals.PlundererAICommon")
public class PlundererCannonAimSeatMixin {
    @WrapOperation(method = "aimCannonAndShoot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;position()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 exotic_worlds$targetNearestCannon(LivingEntity target, Operation<Vec3> original,
            @Local(ordinal = 0) Vec3 cannon) {
        return WorldLoopAttachments.foldOf(target.level()).nearestCopy(cannon, original.call(target));
    }
}

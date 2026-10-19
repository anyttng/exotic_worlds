package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.phys.AABB;

@Mixin(ThrownSplashPotion.class)
public class ThrownSplashPotionMixin {
    @WrapOperation(
            method = "onHitAsPotion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;distanceToSqr(Lnet/minecraft/world/phys/AABB;)D"))
    private double exotic_worlds$splashReachThroughSeam(AABB burst, AABB victim, Operation<Double> original) {
        return original.call(burst, FoldedBoxQuery.toward((ThrownSplashPotion) (Object) this, burst.getCenter(), victim));
    }
}

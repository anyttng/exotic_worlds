package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;

@Mixin(WitherBoss.class)
public class WitherApproachMixin {
    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X))
    private double toroidal$targetX(Entity target, Operation<Double> original) {
        return SeamAim.nearestCoord((Entity) (Object) this, target, Direction.Axis.X, original.call(target));
    }

    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z))
    private double toroidal$targetZ(Entity target, Operation<Double> original) {
        return SeamAim.nearestCoord((Entity) (Object) this, target, Direction.Axis.Z, original.call(target));
    }
}

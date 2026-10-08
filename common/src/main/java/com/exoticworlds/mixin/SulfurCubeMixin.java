package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.exoticworlds.engine.seam.SeamSteering;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.phys.Vec3;

@Mixin(SulfurCube.class)
public class SulfurCubeMixin {
    @Unique
    private static final String KNOCKBACK = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V";

    @Unique
    private static final String PLAYER_PUSH = "playerPush(Lnet/minecraft/world/entity/player/Player;)V";

    @WrapMethod(method = KNOCKBACK)
    private void toroidal$knockbackThroughSeam(double power, double xd, double zd, DamageSource source, float damage,
            boolean comesFromEffect, Operation<Void> original) {
        Vec3 direction = SeamAim.foldDelta((Entity) (Object) this, xd, zd);
        original.call(power, direction.x, direction.z, source, damage, comesFromEffect);
    }

    @ModifyExpressionValue(
            method = KNOCKBACK,
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_EYE_POSITION))
    private Vec3 toroidal$attackerEyeThroughSeam(Vec3 attackerEye) {
        return SeamSteering.nearestCopy((Entity) (Object) this, attackerEye);
    }

    @ModifyExpressionValue(method = KNOCKBACK, at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_POSITION))
    private Vec3 toroidal$attackerFeetThroughSeam(Vec3 attackerFeet) {
        return SeamSteering.nearestCopy((Entity) (Object) this, attackerFeet);
    }

    @ModifyExpressionValue(method = PLAYER_PUSH, at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_POSITION))
    private Vec3 toroidal$pusherFeetThroughSeam(Vec3 pusherFeet) {
        return SeamSteering.nearestCopy((Entity) (Object) this, pusherFeet);
    }
}

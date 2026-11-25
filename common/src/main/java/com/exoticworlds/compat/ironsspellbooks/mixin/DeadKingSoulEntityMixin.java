package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.redspace.ironsspellbooks.entity.mobs.dead_king_boss.DeadKingSoulEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = DeadKingSoulEntity.class, remap = false)
public abstract class DeadKingSoulEntityMixin {
    private static final String GET_RESPAWN_POS =
            "Lio/redspace/ironsspellbooks/entity/mobs/dead_king_boss/DeadKingSoulEntity;"
                    + "getRespawnPos()Lnet/minecraft/world/phys/Vec3;";

    @ModifyExpressionValue(method = "tick()V", at = @At(value = "INVOKE", target = GET_RESPAWN_POS))
    private Vec3 exotic_worlds$stepTowardNearestSpawn(Vec3 respawn) {
        return SeamSteering.nearestCopy((Entity) (Object) this, respawn);
    }

    // The first read is the unset test against Vec3.ZERO, which a copy of the origin would fail.
    @ModifyExpressionValue(method = "isAtSpawn()Z", at = @At(value = "INVOKE", target = GET_RESPAWN_POS, ordinal = 1))
    private Vec3 exotic_worlds$measureNearestSpawn(Vec3 respawn) {
        return SeamSteering.nearestCopy((Entity) (Object) this, respawn);
    }
}

package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.exoticworlds.engine.seam.SeamAim;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.effect.guiding_bolt.GuidingBoltManager;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(value = GuidingBoltManager.class, remap = false)
public abstract class GuidingBoltManagerMixin {
    private static final String SERVER_TICK =
            "serverTick(Lnet/neoforged/neoforge/event/tick/LevelTickEvent$Post;)V";

    @WrapOperation(method = SERVER_TICK, at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X))
    private static double exotic_worlds$searchNearestX(Entity entity, Operation<Double> original,
            @Local Projectile projectile) {
        return SeamAim.nearestCoord(projectile, entity, Direction.Axis.X, original.call(entity));
    }

    @WrapOperation(method = SERVER_TICK, at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z))
    private static double exotic_worlds$searchNearestZ(Entity entity, Operation<Double> original,
            @Local Projectile projectile) {
        return SeamAim.nearestCoord(projectile, entity, Direction.Axis.Z, original.call(entity));
    }

    @WrapOperation(method = SERVER_TICK, at = @At(value = "INVOKE", target = InjectionTargets.AABB_CONTAINS))
    private static boolean exotic_worlds$captureBoxBesideStart(AABB box, Vec3 start, Operation<Boolean> original,
            @Local Projectile projectile) {
        return original.call(FoldedBoxQuery.toward(projectile, start, box), start);
    }

    @ModifyExpressionValue(method = "livingTick(Lnet/neoforged/neoforge/event/tick/EntityTickEvent$Pre;)V",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_GET_CENTER))
    private static Vec3 exotic_worlds$homeTowardNearestCopy(Vec3 center, @Local Projectile projectile) {
        return SeamSteering.nearestCopy(projectile, center);
    }
}

package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

@Mixin(value = AbstractSpellCastingMob.class, remap = false)
public abstract class AbstractSpellCastingMobMixin {
    private static final String FORCE_LOOK_AT_TARGET = "forceLookAtTarget(Lnet/minecraft/world/entity/LivingEntity;)V";

    @WrapOperation(method = FORCE_LOOK_AT_TARGET,
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X))
    private double exotic_worlds$lookAtNearestX(LivingEntity target, Operation<Double> original) {
        return exotic_worlds$lookAtNearest(target, Direction.Axis.X, original.call(target));
    }

    @WrapOperation(method = FORCE_LOOK_AT_TARGET,
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z))
    private double exotic_worlds$lookAtNearestZ(LivingEntity target, Operation<Double> original) {
        return exotic_worlds$lookAtNearest(target, Direction.Axis.Z, original.call(target));
    }

    @Unique
    private double exotic_worlds$lookAtNearest(LivingEntity target, Direction.Axis axis, double raw) {
        Entity mob = (Entity) (Object) this;
        return SeamAim.nearestCoord(mob, target, axis, raw);
    }
}

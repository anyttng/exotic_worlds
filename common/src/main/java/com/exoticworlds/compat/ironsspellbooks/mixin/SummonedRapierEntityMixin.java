package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.redspace.ironsspellbooks.entity.spells.summoned_weapons.SummonedRapierEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = SummonedRapierEntity.class, remap = false)
public abstract class SummonedRapierEntityMixin {
    @ModifyExpressionValue(method = "performSidestep(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_POSITION))
    private Vec3 exotic_worlds$damagerBesideRapier(Vec3 damager) {
        return SeamSteering.nearestCopy((Entity) (Object) this, damager);
    }
}

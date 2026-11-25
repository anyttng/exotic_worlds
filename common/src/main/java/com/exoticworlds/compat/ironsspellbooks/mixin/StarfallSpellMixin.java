package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.spells.ender.StarfallSpell;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = StarfallSpell.class, remap = false)
public abstract class StarfallSpellMixin {
    @ModifyExpressionValue(method = "onServerCastTick(Lnet/minecraft/world/level/Level;I"
            + "Lnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_POSITION))
    private Vec3 exotic_worlds$weightedBesideCenter(Vec3 position, @Local(argsOnly = true) LivingEntity caster,
            @Local(ordinal = 0) Vec3 center) {
        return SeamRange.toward(caster, center, position);
    }
}

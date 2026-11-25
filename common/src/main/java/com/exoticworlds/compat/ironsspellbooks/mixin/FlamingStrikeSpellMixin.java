package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.ironsspellbooks.IronsSpellbooksInjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.spells.fire.FlamingStrikeSpell;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = FlamingStrikeSpell.class, remap = false)
public abstract class FlamingStrikeSpellMixin {
    @ModifyExpressionValue(method = IronsSpellbooksInjectionTargets.SPELL_ON_CAST,
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_POSITION))
    private Vec3 exotic_worlds$struckBesideCaster(Vec3 position, @Local(argsOnly = true) LivingEntity caster) {
        return SeamSteering.nearestCopy(caster, position);
    }

    @ModifyExpressionValue(method = IronsSpellbooksInjectionTargets.SPELL_ON_CAST,
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_GET_CENTER, ordinal = 1))
    private Vec3 exotic_worlds$struckCenterBesideCaster(Vec3 center, @Local(argsOnly = true) LivingEntity caster) {
        return SeamSteering.nearestCopy(caster, center);
    }
}

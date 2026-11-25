package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.ironsspellbooks.IronsSpellbooksInjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.spells.ice.IceSpikesSpell;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = IceSpikesSpell.class, remap = false)
public abstract class IceSpikesSpellMixin {
    @ModifyExpressionValue(method = IronsSpellbooksInjectionTargets.SPELL_ON_CAST,
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_POSITION))
    private Vec3 exotic_worlds$targetBesideCaster(Vec3 target, @Local(argsOnly = true) LivingEntity caster) {
        return SeamSteering.nearestCopy(caster, target);
    }
}

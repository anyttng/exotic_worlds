package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.spells.eldritch.TelekinesisSpell;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = TelekinesisSpell.class, remap = false)
public abstract class TelekinesisSpellMixin {
    @ModifyExpressionValue(method = "handleTelekinesis(Lnet/minecraft/server/level/ServerLevel;"
            + "Lnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;F)V",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_POSITION, ordinal = 1))
    private Vec3 exotic_worlds$heldBesideCaster(Vec3 target, @Local(argsOnly = true) LivingEntity caster) {
        return SeamSteering.nearestCopy(caster, target);
    }
}

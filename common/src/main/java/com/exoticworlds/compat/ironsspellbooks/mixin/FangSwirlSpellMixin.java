package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.ironsspellbooks.IronsSpellbooksInjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.spells.evocation.FangSwirlSpell;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = FangSwirlSpell.class, remap = false)
public abstract class FangSwirlSpellMixin {
    @ModifyExpressionValue(method = IronsSpellbooksInjectionTargets.SPELL_ON_CAST,
            at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/api/util/Utils;moveToRelativeGroundLevel("
                    + "Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;I)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$destinationBesideCaster(Vec3 destination, @Local(argsOnly = true) LivingEntity caster) {
        return SeamSteering.nearestCopy(caster, destination);
    }
}

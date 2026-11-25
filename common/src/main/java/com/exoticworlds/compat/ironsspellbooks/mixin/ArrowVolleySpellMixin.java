package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.ironsspellbooks.IronsSpellbooksInjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.spells.evocation.ArrowVolleySpell;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = ArrowVolleySpell.class, remap = false)
public abstract class ArrowVolleySpellMixin {
    @ModifyExpressionValue(method = IronsSpellbooksInjectionTargets.SPELL_ON_CAST,
            at = @At(value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/capabilities/magic/TargetEntityCastData;getTargetPosition("
                            + "Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/phys/Vec3;"))
    private @Nullable Vec3 exotic_worlds$targetBesideCaster(@Nullable Vec3 target,
            @Local(argsOnly = true) LivingEntity caster) {
        return target == null ? null : SeamSteering.nearestCopy(caster, target);
    }
}

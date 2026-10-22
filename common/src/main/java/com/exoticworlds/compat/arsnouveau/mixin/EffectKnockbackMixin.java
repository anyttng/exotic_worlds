package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectKnockback;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = EffectKnockback.class, remap = false)
public abstract class EffectKnockbackMixin {
    @ModifyExpressionValue(
            method = "knockback(Lnet/minecraft/world/entity/Entity;Lcom/hollingsworth/arsnouveau/api/spell/wrapped_caster/IWrappedCaster;FZ)V",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT))
    private Vec3 exotic_worlds$extractDeltaThroughSeam(Vec3 delta, @Local(argsOnly = true) Entity target) {
        return SeamAim.foldDelta(target, delta);
    }
}

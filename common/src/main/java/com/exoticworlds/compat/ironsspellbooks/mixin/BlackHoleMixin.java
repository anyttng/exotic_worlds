package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.entity.spells.black_hole.BlackHole;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = BlackHole.class, remap = false)
public abstract class BlackHoleMixin {
    @ModifyExpressionValue(method = "handleGravity(Lnet/minecraft/world/phys/Vec3;FZ)V",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_POSITION))
    private Vec3 exotic_worlds$pulledBesideCenter(Vec3 position, @Local(argsOnly = true) Vec3 center) {
        return SeamRange.toward((Entity) (Object) this, center, position);
    }

    @ModifyExpressionValue(method = "handleClientEffects(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_POSITION))
    private Vec3 exotic_worlds$burstBesideCenter(Vec3 position, @Local(argsOnly = true) Vec3 center) {
        return SeamRange.toward((Entity) (Object) this, center, position);
    }
}

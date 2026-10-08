package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;

@Mixin(HoglinBase.class)
public interface HoglinBaseMixin {
    @ModifyArg(
            method = "throwTarget(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_INIT),
            index = 0)
    private static double toroidal$tossDirectionX(double deltaX,
            @Local(argsOnly = true, ordinal = 0) LivingEntity body) {
        return SeamAim.foldX(body, deltaX);
    }

    @ModifyArg(
            method = "throwTarget(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_INIT),
            index = 2)
    private static double toroidal$tossDirectionZ(double deltaZ,
            @Local(argsOnly = true, ordinal = 0) LivingEntity body) {
        return SeamAim.foldZ(body, deltaZ);
    }
}

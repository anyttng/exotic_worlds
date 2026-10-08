package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.ai.behavior.RamTarget;

@Mixin(RamTarget.class)
public class RamTargetMixin {
    @ModifyArg(
            method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/goat/Goat;J)V",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_INIT),
            index = 0)
    private double toroidal$ramDirectionX(double deltaX, @Local(argsOnly = true) Goat body) {
        return SeamAim.foldX(body, deltaX);
    }

    @ModifyArg(
            method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/goat/Goat;J)V",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_INIT),
            index = 2)
    private double toroidal$ramDirectionZ(double deltaZ, @Local(argsOnly = true) Goat body) {
        return SeamAim.foldZ(body, deltaZ);
    }
}

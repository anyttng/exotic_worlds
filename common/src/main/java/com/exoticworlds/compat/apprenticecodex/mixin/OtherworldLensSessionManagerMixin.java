package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "jp.aquafactory.apprenticecodex.spell.otherworldlens.OtherworldLensSessionManager", remap = false)
public abstract class OtherworldLensSessionManagerMixin {
    @WrapOperation(method = "validate", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR))
    private static double exotic_worlds$lensDistanceThroughSeam(Vec3 player, Vec3 lens, Operation<Double> original,
            @Local(argsOnly = true) ServerPlayer caster) {
        return original.call(player, SeamRange.toward(caster, player, lens));
    }
}

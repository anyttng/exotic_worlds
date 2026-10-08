package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignBlockEntity;

@Mixin(SignBlockEntity.class)
public class SignFacingMixin {
    @WrapOperation(
            method = "getSlotPlayerIsFacing",
            at = @At(value = "INVOKE", target = InjectionTargets.MTH_ATAN2))
    private double toroidal$facingAngleThroughSeam(double deltaZ, double deltaX, Operation<Double> original,
            @Local(argsOnly = true) Player player) {
        return original.call(SeamAim.foldZ(player, deltaZ), SeamAim.foldX(player, deltaX));
    }
}

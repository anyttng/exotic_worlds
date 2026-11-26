package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.accessors.LightStepFrame;
import com.exoticworlds.engine.fold.LightStep;

import net.minecraft.core.Direction;
import net.minecraft.world.level.lighting.SkyLightEngine;

@Mixin(SkyLightEngine.class)
public abstract class SkyLightEmptySectionStepMixin {
    @ModifyArg(
            method = {"propagateIncrease", "propagateDecrease"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/lighting/SkyLightEngine;"
                            + "propagateFromEmptySections(JLnet/minecraft/core/Direction;IZI)V"),
            index = 1,
            expect = 2)
    private Direction exotic_worlds$turnStepDirection(Direction propagationDirection) {
        return LightStep.inTargetFrame(
                ((LightStepFrame) (Object) this).exotic_worlds$getStepOrientation(), propagationDirection);
    }
}

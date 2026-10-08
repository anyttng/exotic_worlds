package com.exoticworlds.compat.scalablelux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.scalablelux.LightLockFolds;

@Mixin(targets = "ca.spottedleaf.starlight.common.light.vanillainterface.ThreadedLevelLightEngineVanillaInterface",
        remap = false)
public class ThreadedLevelLightEngineVanillaInterfaceMixin {
    @Inject(method = "close", at = @At("HEAD"))
    private void toroidal$releaseLockOwner(CallbackInfo ci) {
        LightLockFolds.release(this);
    }
}

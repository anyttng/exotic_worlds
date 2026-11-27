package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.eminus.LapTest;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(targets = "com.eminus.render.tree.TreeTraversal")
public class TreeTraversalWaitingMixin {
    @Inject(method = "waiting", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$waitOnlyInsideTheLap(CallbackInfo ci, @Local(argsOnly = true) long key) {
        if (!((LapTest) (Object) this).exotic_worlds$isMeeting(key, LapTest.NO_MARGIN)) {
            ci.cancel();
        }
    }
}

package com.exoticworlds.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.sable.client.SableClientFrame;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;

@Mixin(value = ClientSubLevel.class, remap = false)
public class ClientSubLevelMixin {
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void exotic_worlds$reseatHeldPoses(CallbackInfo ci) {
        SableClientFrame.reseatHeld((ClientSubLevel) (Object) this);
    }
}

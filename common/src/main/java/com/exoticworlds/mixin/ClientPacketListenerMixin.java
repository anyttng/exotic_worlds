package com.exoticworlds.mixin;

import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.client.engine.PublishedShapes;
import com.exoticworlds.client.engine.SyncedTagFold;
import com.exoticworlds.engine.net.ComponentPositions;

import net.minecraft.client.multiplayer.ClientPacketListener;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "clearLevel", at = @At("RETURN"))
    private void toroidal$forgetWhatTheServerPublished(CallbackInfo ci) {
        PublishedShapes.clear();
        SyncedTagFold.declare(Map.of());
        ComponentPositions.declare(Set.of());
    }
}

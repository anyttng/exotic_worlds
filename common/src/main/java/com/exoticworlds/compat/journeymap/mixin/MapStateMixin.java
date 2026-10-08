package com.exoticworlds.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.journeymap.JourneyMapFold;

import journeymap.client.properties.FullMapProperties;
import journeymap.client.properties.InGameMapProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

@Mixin(targets = "journeymap.client.model.map.MapState", remap = false)
public abstract class MapStateMixin {
    @WrapMethod(method = "setZoom(I)Z")
    private boolean toroidal$refuseBelowZoomFloor(int zoom, Operation<Boolean> original) {
        return zoom >= JourneyMapFold.zoomFloor() && original.call(zoom);
    }

    @Inject(method = "refresh", at = @At("HEAD"))
    private void toroidal$seatStoredZoomOnTheFloor(Minecraft minecraft, Player player, InGameMapProperties mapProperties,
            CallbackInfo ci) {
        if (!(mapProperties instanceof FullMapProperties)) {
            return;
        }

        int stored = mapProperties.zoomLevel.get();
        int seated = JourneyMapFold.seatFullscreenZoom(stored);
        if (seated != stored) {
            mapProperties.zoomLevel.set(seated);
        }
    }
}

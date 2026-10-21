package com.exoticworlds.compat.ultimatemapatlases.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.ultimatemapatlases.AtlasPointSeat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.phys.Vec3;
import net.nanaky.ultimate_map_atlases.integration.moonlight.ClientMarkersRenderer;
import net.nanaky.ultimate_map_atlases.utils.MapDataHolder;

@Mixin(ClientMarkersRenderer.class)
public class ClientMarkersRendererMixin {
    @WrapOperation(
            method = "drawSmallPins",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;subtract(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 exotic_worlds$pinNearPlayer(Vec3 player, double pinX, double y, double pinZ,
            Operation<Vec3> original, @Local MapDataHolder holder) {
        Vec3 pin = AtlasPointSeat.toward(holder.data.dimension, player, pinX, pinZ);
        return original.call(player, pin.x, y, pin.z);
    }
}

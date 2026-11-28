package com.exoticworlds.compat.mapatlases.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.map.AtlasPointSeat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.phys.Vec3;
import pepjebs.mapatlases.integration.moonlight.ClientMarkersRenderer;
import pepjebs.mapatlases.utils.Slice;

@Mixin(ClientMarkersRenderer.class)
public class ClientMarkersRendererMixin {
    @WrapOperation(method = "drawSmallPins", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT))
    private static Vec3 exotic_worlds$pinNearAnchor(Vec3 anchor, Vec3 pin, Operation<Vec3> original,
            @Local(argsOnly = true) Slice slice) {
        Vec3 seated = AtlasPointSeat.toward(slice.dimension(), anchor, pin.x, pin.z);
        return original.call(anchor, new Vec3(seated.x, pin.y, seated.z));
    }
}

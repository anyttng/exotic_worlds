package com.exoticworlds.compat.xaero.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.exoticworlds.compat.xaero.XaeroFold;

@Mixin(targets = "xaero.common.minimap.waypoints.Waypoint", remap = false)
public abstract class WaypointMixin {
    @Shadow
    private int x;
    @Shadow
    private int z;

    @ModifyReturnValue(method = "getX(D)I", at = @At("RETURN"))
    private int toroidal$foldX(int original, double dimDiv) {
        return (int) Math.round(XaeroFold.nearestToCamera(original, toroidal$scaled(this.z, dimDiv)).x);
    }

    @ModifyReturnValue(method = "getZ(D)I", at = @At("RETURN"))
    private int toroidal$foldZ(int original, double dimDiv) {
        return (int) Math.round(XaeroFold.nearestToCamera(toroidal$scaled(this.x, dimDiv), original).z);
    }

    // Restates getX(D)/getZ(D): calling the other getter instead recurses through its handler.
    @Unique
    private static int toroidal$scaled(int coord, double dimDiv) {
        return dimDiv == 1.0 ? coord : (int) Math.floor(coord / dimDiv);
    }
}

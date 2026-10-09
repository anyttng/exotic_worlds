package com.exoticworlds.compat.xaero.mixin.map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold;

import xaero.common.minimap.waypoints.Waypoint;

@Mixin(value = xaero.map.mods.gui.Waypoint.class, remap = false)
public abstract class WaypointWrapperMixin {
    @Shadow
    private Object original;
    @Shadow
    private double dimDiv;

    @ModifyReturnValue(method = "getX", at = @At("RETURN"))
    private int toroidal$foldX(int x) {
        return this.dimDiv == 1.0 ? XaeroWorldMapFold.foldBlock(x, ((Waypoint) this.original).getZ()).getX() : x;
    }

    @ModifyReturnValue(method = "getZ", at = @At("RETURN"))
    private int toroidal$foldZ(int z) {
        return this.dimDiv == 1.0 ? XaeroWorldMapFold.foldBlock(((Waypoint) this.original).getX(), z).getZ() : z;
    }
}

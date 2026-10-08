package com.exoticworlds.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import journeymap.client.model.map.MapState;
import journeymap.client.model.region.RegionCoord;
import journeymap.client.render.map.RegionTile;

@Mixin(targets = "journeymap.client.render.map.RegionTile", remap = false)
public interface RegionTileAccessor {
    @Invoker("<init>")
    static RegionTile toroidal$create(RegionCoord regionCoord, MapState state) {
        throw new AssertionError();
    }
}

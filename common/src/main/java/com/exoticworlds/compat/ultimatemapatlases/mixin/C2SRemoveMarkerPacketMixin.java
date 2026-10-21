package com.exoticworlds.compat.ultimatemapatlases.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.ultimatemapatlases.AtlasBannerSeat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.nanaky.ultimate_map_atlases.networking.C2SRemoveMarkerPacket;

@Mixin(C2SRemoveMarkerPacket.class)
public class C2SRemoveMarkerPacketMixin {
    @Shadow
    @Final
    private int worldX;

    @Shadow
    @Final
    private int worldZ;

    @ModifyExpressionValue(
            method = "handle",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/saveddata/maps/MapBanner;pos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$bannerNearPoint(BlockPos banner, @Local MapItemSavedData data) {
        return AtlasBannerSeat.toward(data, this.worldX, this.worldZ, banner);
    }
}

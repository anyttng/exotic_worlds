package com.exoticworlds.compat.mapatlases.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.mapatlases.BannerDecorationSeat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;

@Mixin(C2SRemoveMarkerPacket.class)
public class C2SRemoveMarkerPacketMixin {
    private static final String BANNER_POS = "Lnet/minecraft/world/level/saveddata/maps/MapBanner;pos()Lnet/minecraft/core/BlockPos;";

    @ModifyExpressionValue(
            method = "removeBannerMarker",
            at = {
                    @At(value = "INVOKE", target = BANNER_POS, ordinal = 0),
                    @At(value = "INVOKE", target = BANNER_POS, ordinal = 1)})
    private static BlockPos exotic_worlds$seatBannerBesideCenter(BlockPos banner,
            @Local(argsOnly = true) MapItemSavedData data) {
        return BannerDecorationSeat.towardMapCenter(data, banner);
    }
}

package com.exoticworlds.compat.moonlight.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.moonlight.MoonlightMapFold;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

@Mixin(MLMapMarker.class)
public class MarkerSeatMixin {
    @WrapOperation(
            method = "createDecorationFromMarker",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/mehvahdjukaar/moonlight/api/map/decoration/MLMapMarker;getPos()Lnet/minecraft/core/BlockPos;"))
    private @Nullable BlockPos toroidal$seatOnMap(MLMapMarker<?> marker, Operation<BlockPos> original,
            @Local(argsOnly = true) MapItemSavedData data) {
        BlockPos pos = original.call(marker);
        if (pos == null) {
            return null;
        }

        return NearestCopy.toward(MoonlightMapFold.of(null, data), new BlockPos(data.centerX, pos.getY(), data.centerZ),
                pos);
    }
}

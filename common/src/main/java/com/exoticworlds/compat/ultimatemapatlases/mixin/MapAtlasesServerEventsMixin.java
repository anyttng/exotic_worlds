package com.exoticworlds.compat.ultimatemapatlases.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.api.v1.ExoticWorldsApi;
import com.exoticworlds.compat.ultimatemapatlases.AtlasTileCopies;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;
import net.nanaky.ultimate_map_atlases.lifecycle.MapAtlasesServerEvents;
import net.nanaky.ultimate_map_atlases.map_collection.IMapCollection;
import net.nanaky.ultimate_map_atlases.utils.MapDataHolder;
import net.nanaky.ultimate_map_atlases.utils.Slice;

@Mixin(MapAtlasesServerEvents.class)
public class MapAtlasesServerEventsMixin {
    @WrapOperation(
            method = "onPlayerTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/nanaky/ultimate_map_atlases/map_collection/IMapCollection;select(IILnet/nanaky/ultimate_map_atlases/utils/Slice;)Lnet/nanaky/ultimate_map_atlases/utils/MapDataHolder;"))
    private static @Nullable MapDataHolder exotic_worlds$neighborAtAnyCopy(IMapCollection maps, int centerX,
            int centerZ, Slice slice, Operation<MapDataHolder> original, @Local(argsOnly = true) Player player) {
        for (AtlasTileCopies.TileCenter center : AtlasTileCopies.of(ExoticWorldsApi.shapeOf(player.level()).orElse(null),
                centerX, centerZ, AtlasTileCopies.MAP_PIXELS << maps.getScale())) {
            MapDataHolder found = original.call(maps, center.x(), center.z(), slice);
            if (found != null) {
                return found;
            }
        }

        return null;
    }
}

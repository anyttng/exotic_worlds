package com.exoticworlds.compat.mapatlases.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.mapatlases.MapAtlasesInjectionTargets;
import com.exoticworlds.compat.mapatlases.MapGridKeyCopies;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.lifecycle.MapAtlasesServerEvents;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.map_collection.MapsNeighborhood;
import pepjebs.mapatlases.utils.AtlasMap;

@Mixin(MapAtlasesServerEvents.class)
public class MapAtlasesServerEventsMixin {
    private static final String CREATE_MAP_AT = "Lpepjebs/mapatlases/item/MapAtlasItem;createMapAt(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;Lpepjebs/mapatlases/map_collection/MapGridKey;)Lpepjebs/mapatlases/utils/AtlasMap;";
    private static final String LIST_IS_EMPTY = "Ljava/util/List;isEmpty()Z";

    @WrapOperation(method = "onPlayerTick",
            at = @At(value = "INVOKE", target = MapAtlasesInjectionTargets.GET_MAP_AT_KEY, ordinal = 0))
    private static @Nullable AtlasMap exotic_worlds$neighborAtAnyCopy(MapCollection maps, MapGridKey key,
            Operation<AtlasMap> original, @Local(argsOnly = true) ServerPlayer player) {
        for (MapGridKey copy : MapGridKeyCopies.copies(player.level(), key)) {
            @Nullable AtlasMap map = original.call(maps, copy);
            if (map != null) {
                return map;
            }
        }

        return null;
    }

    @WrapOperation(method = "onPlayerTick",
            at = @At(value = "INVOKE", target = MapAtlasesInjectionTargets.GET_MAP_AT_KEY, ordinal = 1))
    private static @Nullable AtlasMap exotic_worlds$centerUnderPlayer(MapCollection maps, MapGridKey key,
            Operation<AtlasMap> original, @Local(argsOnly = true) ServerPlayer player) {
        for (MapGridKey copy : MapGridKeyCopies.copies(player.level(), key)) {
            @Nullable AtlasMap map = original.call(maps, copy);
            if (map != null) {
                return map;
            }
        }

        return MapGridKeyCopies.getShiftedUnder(player.level(), maps, key, player.getX(), player.getZ());
    }

    @WrapOperation(method = "onPlayerTick", at = @At(value = "INVOKE", target = CREATE_MAP_AT))
    private static @Nullable AtlasMap exotic_worlds$keepTheMovedCopies(ServerPlayer player, ItemStack atlas,
            MapGridKey key, Operation<AtlasMap> original, @Local MapCollection maps) {
        if (MapGridKeyCopies.isCoveredAcrossEdge(player.level(), maps, key)) {
            return null;
        }

        return original.call(player, atlas, key);
    }

    @Inject(method = "onPlayerTick", at = @At(value = "INVOKE", target = LIST_IS_EMPTY))
    private static void exotic_worlds$addMovedNeighbors(ServerPlayer player, CallbackInfo ci,
            @Local MapCollection maps, @Local MapsNeighborhood neighborhood, @Local List<AtlasMap> mapsInView) {
        MapGridKeyCopies.addShiftedNeighbors(player.level(), maps, neighborhood.center(), mapsInView);
    }
}

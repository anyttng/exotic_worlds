package com.exoticworlds.compat.ultimatemapatlases.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.ClientShapes;
import com.exoticworlds.compat.ultimatemapatlases.AtlasBlockMarkerKey;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.seam.MapSeamFold;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.nanaky.ultimate_map_atlases.integration.moonlight.ClientMarkers;

@Mixin(ClientMarkers.class)
public class ClientMarkersMixin {
    @ModifyVariable(method = "addBlockMarker", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static String exotic_worlds$foldedAddedKey(String key, @Local(argsOnly = true) int mapId) {
        return AtlasBlockMarkerKey.folded(exotic_worlds$shapeOfMap(mapId), key);
    }

    @ModifyVariable(method = "addBlockMarker", at = @At("HEAD"), argsOnly = true)
    private static BlockPos exotic_worlds$foldedHead(BlockPos head, @Local(argsOnly = true) int mapId) {
        return ClientShapes.fold(exotic_worlds$shapeOfMap(mapId), head);
    }

    @ModifyVariable(method = {"hasBlockMarker", "removeBlockMarker"}, at = @At("HEAD"), argsOnly = true)
    private static String exotic_worlds$foldedKey(String key, @Local(argsOnly = true) int mapId) {
        return AtlasBlockMarkerKey.folded(exotic_worlds$shapeOfMap(mapId), key);
    }

    @ModifyArg(
            method = "addMissingBlockMarkersToMap",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;isLoaded(Lnet/minecraft/core/BlockPos;)Z"))
    private static BlockPos exotic_worlds$loadedNearPlayer(BlockPos marker) {
        return exotic_worlds$nearPlayer(marker);
    }

    @ModifyArg(
            method = "addMissingBlockMarkersToMap",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockPos exotic_worlds$blockNearPlayer(BlockPos marker) {
        return exotic_worlds$nearPlayer(marker);
    }

    @Unique
    private static @Nullable ToroidalShape exotic_worlds$shapeOfMap(int mapId) {
        ClientLevel level = Minecraft.getInstance().level;
        MapItemSavedData data = level == null ? null : level.getMapData(new MapId(mapId));
        return data == null ? null : ClientShapes.of(data.dimension);
    }

    @Unique
    private static BlockPos exotic_worlds$nearPlayer(BlockPos marker) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? marker
                : NearestCopy.toward(MapSeamFold.transformerFor(null, player.level().dimension()),
                        player.blockPosition(), marker);
    }
}

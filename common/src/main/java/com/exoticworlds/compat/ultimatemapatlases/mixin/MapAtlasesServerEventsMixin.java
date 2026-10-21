package com.exoticworlds.compat.ultimatemapatlases.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.ultimatemapatlases.AtlasCopyLookup;
import com.exoticworlds.compat.ultimatemapatlases.UltimateMapAtlasesInjectionTargets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.nanaky.ultimate_map_atlases.lifecycle.MapAtlasesServerEvents;
import net.nanaky.ultimate_map_atlases.map_collection.IMapCollection;
import net.nanaky.ultimate_map_atlases.map_collection.MapKey;
import net.nanaky.ultimate_map_atlases.utils.MapDataHolder;
import net.nanaky.ultimate_map_atlases.utils.Slice;

@Mixin(MapAtlasesServerEvents.class)
public class MapAtlasesServerEventsMixin {
    private static final String SELECT_AT = "Lnet/nanaky/ultimate_map_atlases/map_collection/IMapCollection;select(IILnet/nanaky/ultimate_map_atlases/utils/Slice;)Lnet/nanaky/ultimate_map_atlases/utils/MapDataHolder;";

    @WrapOperation(method = "onPlayerTick", at = @At(value = "INVOKE", target = SELECT_AT))
    private static @Nullable MapDataHolder exotic_worlds$neighborAtAnyCopy(IMapCollection maps, int centerX,
            int centerZ, Slice slice, Operation<MapDataHolder> original, @Local(argsOnly = true) Player player) {
        return AtlasCopyLookup.atAnyCopy(player.level(), maps, centerX, centerZ, slice, original);
    }

    @WrapOperation(method = "onPlayerTick",
            at = @At(value = "INVOKE", target = UltimateMapAtlasesInjectionTargets.SELECT_KEY))
    private static @Nullable MapDataHolder exotic_worlds$activeAtAnyCopy(IMapCollection maps, MapKey key,
            Operation<MapDataHolder> original, @Local(argsOnly = true) Player player) {
        return AtlasCopyLookup.atAnyCopy(player.level(), maps, key, original);
    }

    @Inject(method = "onPlayerTick",
            at = @At(value = "INVOKE", target = UltimateMapAtlasesInjectionTargets.SELECT_KEY, ordinal = 0))
    private static void exotic_worlds$shiftedNeighbors(CallbackInfo ci, @Local(argsOnly = true) Player player,
            @Local IMapCollection maps, @Local MapKey activeKey, @Local List<MapDataHolder> nearbyExistentMaps) {
        AtlasCopyLookup.addShiftedNeighbors(player.level(), maps, activeKey, nearbyExistentMaps);
    }

    @WrapOperation(method = "sendSlicesAboveAndBelow", at = @At(value = "INVOKE", target = SELECT_AT))
    private static @Nullable MapDataHolder exotic_worlds$sliceAtAnyCopy(IMapCollection maps, int centerX,
            int centerZ, Slice slice, Operation<MapDataHolder> original,
            @Local(argsOnly = true) ServerPlayer player) {
        return AtlasCopyLookup.atAnyCopy(player.level(), maps, centerX, centerZ, slice, original);
    }

    @WrapOperation(method = "onPlayerJoin",
            at = @At(value = "INVOKE", target = UltimateMapAtlasesInjectionTargets.SELECT_KEY))
    private static @Nullable MapDataHolder exotic_worlds$joinAtAnyCopy(IMapCollection maps, MapKey key,
            Operation<MapDataHolder> original, @Local(argsOnly = true) ServerPlayer player) {
        return AtlasCopyLookup.atAnyCopy(player.level(), maps, key, original);
    }

    @Inject(method = "maybeCreateNewMapEntry", at = @At("HEAD"), cancellable = true)
    private static void exotic_worlds$keepTheHeldCopy(ServerPlayer player, ItemStack atlas, IMapCollection maps,
            Slice slice, int destX, int destZ, CallbackInfo ci) {
        if (AtlasCopyLookup.heldAcrossEdge(player.level(), maps, MapKey.at(maps.getScale(), destX, destZ, slice))) {
            ci.cancel();
        }
    }
}

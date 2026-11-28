package com.exoticworlds.compat.mapatlases.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.compat.map.ClientFolds;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import pepjebs.mapatlases.map_collection.MapGridKey;

@Mixin(MapGridKey.class)
public class MapGridKeyMixin {
    private static final String AT = "Lpepjebs/mapatlases/map_collection/MapGridKey;at(BLpepjebs/mapatlases/utils/Slice;DD)Lpepjebs/mapatlases/map_collection/MapGridKey;";

    @ModifyArg(method = "atEntityPosition", at = @At(value = "INVOKE", target = AT), index = 2)
    private static double exotic_worlds$foldedKeyX(double x, @Local(argsOnly = true) Player player) {
        return exotic_worlds$folded(player, x, player.getZ()).x;
    }

    @ModifyArg(method = "atEntityPosition", at = @At(value = "INVOKE", target = AT), index = 3)
    private static double exotic_worlds$foldedKeyZ(double z, @Local(argsOnly = true) Player player) {
        return exotic_worlds$folded(player, player.getX(), z).z;
    }

    @Unique
    private static Vec3 exotic_worlds$folded(Player player, double x, double z) {
        return ClientFolds.fold(ClientFolds.of(player.level()), x, z);
    }
}

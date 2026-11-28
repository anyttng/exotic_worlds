package com.exoticworlds.compat.mapatlases.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.compat.mapatlases.MapGridKeyCopies;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.map_collection.MapsNeighborhood;

@Mixin(MapsNeighborhood.class)
public class MapsNeighborhoodMixin {
    private static final String CONSTRUCTOR = "Lpepjebs/mapatlases/map_collection/MapsNeighborhood;<init>(Ljava/util/Set;Lpepjebs/mapatlases/map_collection/MapGridKey;)V";

    @ModifyArg(method = "around", at = @At(value = "INVOKE", target = CONSTRUCTOR), index = 0)
    private static Set<MapGridKey> exotic_worlds$foldAround(Set<MapGridKey> around,
            @Local(argsOnly = true) Player player) {
        return MapGridKeyCopies.foldKeys(player.level(), around);
    }

    @ModifyArg(method = "around", at = @At(value = "INVOKE", target = CONSTRUCTOR), index = 1)
    private static MapGridKey exotic_worlds$foldCenter(MapGridKey center, @Local(argsOnly = true) Player player) {
        return MapGridKeyCopies.foldKey(player.level(), center);
    }
}

package com.exoticworlds.compat.simpleatlas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.simpleatlas.AtlasTileFold;

import net.minecraft.core.Direction;

import rubbertoe.simple_atlas.client.screen.icon.AtlasIcon;
import rubbertoe.simple_atlas.network.AtlasTilePayload;

@Mixin(AtlasIcon.class)
public abstract class AtlasIconMixin {
    @ModifyExpressionValue(
            method = "resolveAnchor",
            at = @At(value = "INVOKE",
                    target = "Lrubbertoe/simple_atlas/client/screen/icon/AtlasIcon$WorldPoint;x()D"))
    private double toroidal$pointXNearTile(double x, @Local(name = "tile") AtlasTilePayload tile) {
        return toroidal$seat(tile, Direction.Axis.X, x);
    }

    @ModifyExpressionValue(
            method = "resolveAnchor",
            at = @At(value = "INVOKE",
                    target = "Lrubbertoe/simple_atlas/client/screen/icon/AtlasIcon$WorldPoint;z()D"))
    private double toroidal$pointZNearTile(double z, @Local(name = "tile") AtlasTilePayload tile) {
        return toroidal$seat(tile, Direction.Axis.Z, z);
    }

    @Unique
    private static double toroidal$seat(AtlasTilePayload tile, Direction.Axis axis, double coord) {
        return MapCopies.current() == MapCopies.SINGLE
                ? AtlasTileFold.foldOnTile(tile, axis, coord)
                : AtlasTileFold.nearestToTile(tile, axis, coord);
    }
}

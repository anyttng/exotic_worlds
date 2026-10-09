package com.exoticworlds.compat.simpleatlas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.simpleatlas.AtlasTileFold;

import net.minecraft.world.phys.Vec3;

import rubbertoe.simple_atlas.client.screen.icon.AtlasIcon;
import rubbertoe.simple_atlas.network.AtlasTilePayload;

@Mixin(AtlasIcon.class)
public abstract class AtlasIconMixin {
    @WrapOperation(
            method = "resolveAnchor",
            at = @At(value = "INVOKE",
                    target = "Lrubbertoe/simple_atlas/client/screen/icon/AtlasIcon$WorldPoint;x()D"))
    private double toroidal$pointXNearTile(@Coerce Object point, Operation<Double> original,
            @Local(name = "tile") AtlasTilePayload tile) {
        return toroidal$seat(tile, original.call(point), ((AtlasIconPointAccessor) point).toroidal$z()).x;
    }

    @WrapOperation(
            method = "resolveAnchor",
            at = @At(value = "INVOKE",
                    target = "Lrubbertoe/simple_atlas/client/screen/icon/AtlasIcon$WorldPoint;z()D"))
    private double toroidal$pointZNearTile(@Coerce Object point, Operation<Double> original,
            @Local(name = "tile") AtlasTilePayload tile) {
        return toroidal$seat(tile, ((AtlasIconPointAccessor) point).toroidal$x(), original.call(point)).z;
    }

    @Unique
    private static Vec3 toroidal$seat(AtlasTilePayload tile, double x, double z) {
        return MapCopies.current() == MapCopies.SINGLE
                ? AtlasTileFold.foldOnTile(tile, x, z)
                : AtlasTileFold.nearestToTile(tile, x, z);
    }
}

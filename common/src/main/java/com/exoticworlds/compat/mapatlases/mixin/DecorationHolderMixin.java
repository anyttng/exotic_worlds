package com.exoticworlds.compat.mapatlases.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.map.AtlasPointSeat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.phys.Vec3;
import pepjebs.mapatlases.utils.DecorationHolder;

@Mixin(DecorationHolder.class)
public abstract class DecorationHolderMixin {
    @ModifyExpressionValue(
            method = "decorationDistSq",
            at = @At(value = "INVOKE", target = "Lpepjebs/mapatlases/utils/DecorationHolder;getWorldX()D"))
    private double exotic_worlds$worldXNearPoint(double x, @Local(argsOnly = true, ordinal = 0) double px,
            @Local(argsOnly = true, ordinal = 1) double pz) {
        DecorationHolder holder = (DecorationHolder) (Object) this;
        return exotic_worlds$seated(holder, px, pz, x, holder.getWorldZ()).x;
    }

    @ModifyExpressionValue(
            method = "decorationDistSq",
            at = @At(value = "INVOKE", target = "Lpepjebs/mapatlases/utils/DecorationHolder;getWorldZ()D"))
    private double exotic_worlds$worldZNearPoint(double z, @Local(argsOnly = true, ordinal = 0) double px,
            @Local(argsOnly = true, ordinal = 1) double pz) {
        DecorationHolder holder = (DecorationHolder) (Object) this;
        return exotic_worlds$seated(holder, px, pz, holder.getWorldX(), z).z;
    }

    @Unique
    private static Vec3 exotic_worlds$seated(DecorationHolder holder, double px, double pz, double x, double z) {
        return AtlasPointSeat.toward(holder.data().data.dimension, new Vec3(px, 0.0, pz), x, z);
    }
}

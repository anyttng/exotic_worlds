package com.exoticworlds.compat.simpleclouds.mixin;

import org.joml.Matrix2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.simpleclouds.SimpleCloudsShapes;
import com.llamalad7.mixinextras.sugar.Local;

import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;

@Mixin(value = CloudRegion.class, remap = false)
public abstract class CloudRegionMixin {
    @Unique
    private static final String CIRCLE = "circle";
    @Unique
    private static final float CURRENT_TICK = 1.0F;

    @ModifyVariable(method = CIRCLE, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static float toroidal$seatQueryX(float x, @Local(argsOnly = true) CloudRegion region,
            @Local(argsOnly = true, ordinal = 1) float z) {
        ToroidalShape shape = SimpleCloudsShapes.current();
        if (shape == null) {
            return x;
        }

        Matrix2f transform = region.createTransform(CURRENT_TICK);
        return SimpleCloudsShapes.latticeOf(shape, transform.m00, transform.m01, transform.m10, transform.m11)
                .seatedX(region.getPosX(), region.getPosZ(), transform.m00, transform.m01, transform.m10,
                        transform.m11, x, z);
    }

    @ModifyVariable(method = CIRCLE, at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static float toroidal$seatQueryZ(float z, @Local(argsOnly = true) CloudRegion region,
            @Local(argsOnly = true, ordinal = 0) float x) {
        ToroidalShape shape = SimpleCloudsShapes.current();
        if (shape == null) {
            return z;
        }

        Matrix2f transform = region.createTransform(CURRENT_TICK);
        return SimpleCloudsShapes.latticeOf(shape, transform.m00, transform.m01, transform.m10, transform.m11)
                .seatedZ(region.getPosX(), region.getPosZ(), transform.m00, transform.m01, transform.m10,
                        transform.m11, x, z);
    }
}

package com.exoticworlds.compat.simpleclouds.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.exoticworlds.compat.simpleclouds.CloudRegionShaderSource;
import com.exoticworlds.compat.simpleclouds.SimpleCloudsShapes;
import com.exoticworlds.compat.simpleclouds.mixin.CloudManagerAccessor;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import dev.nonamecrackers2.simpleclouds.client.mesh.generator.MultiRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudGetter;

@Mixin(value = MultiRegionCloudMeshGenerator.class, remap = false)
public abstract class MultiRegionCloudMeshGeneratorMixin {
    @Shadow
    private CloudGetter cloudGetter;

    @ModifyReturnValue(method = "lambda$uploadCloudRegionData$4", at = @At("RETURN"))
    private float[] toroidal$appendLattice(float[] region) {
        return CloudRegionShaderSource.withLattice(region, SimpleCloudsShapes.of(
                this.cloudGetter instanceof CloudManagerAccessor manager ? manager.toroidal$level() : null));
    }

    @ModifyConstant(method = "uploadCloudRegionData",
            constant = @Constant(intValue = CloudRegionShaderSource.REGION_BYTES))
    private int toroidal$foldedRegionBytes(int bytes) {
        return CloudRegionShaderSource.rewritten() ? CloudRegionShaderSource.FOLDED_REGION_BYTES : bytes;
    }

    @ModifyConstant(method = {"initExtra", "onOffGen"},
            constant = @Constant(intValue = CloudRegionShaderSource.REGION_BUFFER_BYTES))
    private int toroidal$foldedRegionBuffer(int bytes) {
        return CloudRegionShaderSource.FOLDED_REGION_BUFFER_BYTES;
    }
}

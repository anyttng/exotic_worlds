package com.exoticworlds.compat.simpleclouds.mixin;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.compat.simpleclouds.CloudShape;
import com.exoticworlds.compat.simpleclouds.SimpleCloudsShapes;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(value = CloudManager.class, remap = false)
public abstract class CloudManagerMixin {
    @Shadow
    @Final
    protected Level level;

    @WrapMethod(method = "getPrecipitationAt")
    private Pair<?, ?> exotic_worlds$bindPrecipitationFold(BlockPos pos, Operation<Pair<?, ?>> original) {
        WorldFold fold = WorldLoopAttachments.noiseFoldOf(this.level);
        try (GenerationFoldContext.Context.FoldScope scope = GenerationFoldContext.context().withFold(fold)) {
            return original.call(pos);
        }
    }

    @WrapMethod(method = "getCloudTypeAtPosition")
    private Pair<CloudType, Float> exotic_worlds$bindCloudShape(float x, float z,
            Operation<Pair<CloudType, Float>> original) {
        CloudShape previous = SimpleCloudsShapes.enter(this.level);
        try {
            return original.call(x, z);
        } finally {
            SimpleCloudsShapes.exit(previous);
        }
    }
}

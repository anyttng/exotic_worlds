package com.exoticworlds.compat.simpleclouds.mixin;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.compat.simpleclouds.SimpleCloudsShapes;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
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
    private Pair<?, ?> toroidal$bindPrecipitationTransformer(BlockPos pos, Operation<Pair<?, ?>> original) {
        return GenerationTransformerContext.withTransformer(
                WorldLoopAttachments.noiseTransformerOf(this.level), () -> original.call(pos));
    }

    @WrapMethod(method = "getCloudTypeAtPosition")
    private Pair<CloudType, Float> toroidal$bindCloudShape(float x, float z,
            Operation<Pair<CloudType, Float>> original) {
        return SimpleCloudsShapes.bound(this.level, () -> original.call(x, z));
    }
}

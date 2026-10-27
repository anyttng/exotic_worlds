package com.exoticworlds.compat.seedviewer.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.BinderOrder;
import com.exoticworlds.compat.seedviewer.FoldedNoiseSampler;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.acenia.seedviewer.client.worldgen.NoiseMapType;
import net.acenia.seedviewer.client.worldgen.NoiseSampler;

@Mixin(NoiseSampler.PointSampler.class)
public class PointSamplerMixin {
    @Shadow
    @Final
    NoiseSampler this$0;

    @WrapMethod(method = "sample", order = BinderOrder.FOLD)
    private float exotic_worlds$sampleTheFoldedField(NoiseMapType type, int blockX, int blockY, int blockZ,
            Operation<Float> original) {
        FoldedNoiseSampler sampler = (FoldedNoiseSampler) (Object) this.this$0;
        WorldFold fold = sampler.exotic_worlds$fold();
        if (!fold.isWrapped()) {
            return original.call(type, blockX, blockY, blockZ);
        }

        return sampler.exotic_worlds$cache(type).get(blockX, blockY, blockZ, () -> GenerationFoldContext.withFold(
                fold, () -> original.call(type, blockX, blockY, blockZ)));
    }
}

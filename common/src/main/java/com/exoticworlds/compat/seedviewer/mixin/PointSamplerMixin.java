package com.exoticworlds.compat.seedviewer.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.BinderOrder;
import com.exoticworlds.compat.seedviewer.FoldedNoiseSampler;
import com.exoticworlds.compat.seedviewer.FoldedSampleCache;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
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
    private float toroidal$sampleTheFoldedField(NoiseMapType type, int blockX, int blockY, int blockZ,
            Operation<Float> original) {
        FoldedNoiseSampler sampler = (FoldedNoiseSampler) (Object) this.this$0;
        WorldFold fold = sampler.toroidal$fold();
        FoldedSampleCache<Float> cache = sampler.toroidal$cache(type.modeId());
        Supplier<Float> evaluate = () -> GenerationTransformerContext.withTransformer(fold,
                () -> original.call(type, blockX, blockY, blockZ));
        return cache == null ? evaluate.get() : cache.get(blockX, blockY, blockZ, evaluate);
    }
}

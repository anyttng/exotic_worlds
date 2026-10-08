package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.stateproviders.DualNoiseProvider;
import net.minecraft.world.level.levelgen.synth.Noise;

@Mixin(DualNoiseProvider.class)
public class DualNoiseProviderMixin {
    @Shadow
    @Final
    private Noise slowNoise;

    @Shadow
    @Final
    private float slowScale;

    @WrapMethod(method = "getSlowNoiseValue(Lnet/minecraft/core/BlockPos;)F")
    private float toroidal$rawCoordinateNoise(BlockPos pos, Operation<Float> original) {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return original.call(pos);
        }

        return ContextScaledNoise.sample(transformer, this.slowNoise, this.slowScale,
                pos.getX(), pos.getY() * this.slowScale, pos.getZ());
    }
}

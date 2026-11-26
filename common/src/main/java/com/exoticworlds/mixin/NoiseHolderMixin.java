package com.exoticworlds.mixin;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.NoiseOriginHolder;
import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.NoiseOrigin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(DensityFunction.NoiseHolder.class)
public class NoiseHolderMixin {
    private static final String CONSTRUCTOR =
            "<init>(Lnet/minecraft/core/Holder;Lnet/minecraft/world/level/levelgen/synth/NormalNoise;)V";

    @Inject(method = CONSTRUCTOR, at = @At("RETURN"))
    private void exotic_worlds$stampNoiseOrigin(Holder<NormalNoise.NoiseParameters> noiseData,
            @Nullable NormalNoise noise, CallbackInfo callback) {
        GenerationFoldContext.Context context = GenerationFoldContext.getContext();
        if (noise == null || context.getRouterBuildFold() == null) {
            return;
        }

        List<NoiseFeature> features = ShapeFeatures.atMoment(context.getRouterBuildFeatures(), NoiseFeature.class);
        ((NoiseOriginHolder) noise).exotic_worlds$setNoiseOrigin(NoiseOrigin.of(features,
                context.getRouterBuildDimension(), noiseData, Set.of(), context.getRouterBuildOffsets()));
    }
}

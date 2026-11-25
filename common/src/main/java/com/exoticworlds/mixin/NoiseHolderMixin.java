package com.exoticworlds.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.ClimateFieldMark;
import com.exoticworlds.accessors.NoiseFeatureHolder;
import com.exoticworlds.accessors.NoiseOffsetHolder;
import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.NoiseFeatures;
import com.exoticworlds.shape.feature.compactbiomes.ClimateFields;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(DensityFunction.NoiseHolder.class)
public class NoiseHolderMixin {
    private static final String CONSTRUCTOR =
            "<init>(Lnet/minecraft/core/Holder;Lnet/minecraft/world/level/levelgen/synth/NormalNoise;)V";

    @Inject(method = CONSTRUCTOR, at = @At("RETURN"))
    private void exotic_worlds$markClimateField(Holder<NormalNoise.NoiseParameters> noiseData, NormalNoise noise,
            CallbackInfo callback) {
        if (noise instanceof ClimateFieldMark mark
                && noiseData.unwrapKey().filter(ClimateFields::isClimate).isPresent()) {
            mark.exotic_worlds$markClimateField();
        }
    }

    @Inject(method = CONSTRUCTOR, at = @At("RETURN"))
    private void exotic_worlds$stampNoiseFeatures(Holder<NormalNoise.NoiseParameters> noiseData,
            @Nullable NormalNoise noise, CallbackInfo callback) {
        GenerationFoldContext.Context context = GenerationFoldContext.getContext();
        if (noise == null || context.getRouterBuildFold() == null) {
            return;
        }

        List<NoiseFeature> features = ShapeFeatures.atMoment(context.getRouterBuildFeatures(), NoiseFeature.class);
        ((NoiseFeatureHolder) noise).exotic_worlds$setNoiseFeatures(features);
        ((NoiseOffsetHolder) noise).exotic_worlds$setNoiseOffset(
                NoiseFeatures.offsetOf(features, context.getRouterBuildOffsets(), noiseData));
    }
}

package com.exoticworlds.engine.noise;

import java.util.function.DoubleSupplier;

import com.exoticworlds.core.feature.ClimateFieldMoment;
import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseMoment;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class ForeignFieldCompression {
    public static double followingNoise(WorldFold fold, Holder<NormalNoise> noise, double xzScale, double yScale) {
        FoldedCompileContext context = FoldedCompileContexts.get(fold);
        return context == null ? NoiseFeature.UNCHANGED : followingNoise(context, noise, xzScale, yScale);
    }

    static double followingNoise(FoldedCompileContext context, Holder<NormalNoise> noise, double xzScale,
            double yScale) {
        NoiseMoment moment = new NoiseMoment(context.fold(), context.dimension(), noise,
                FoldedSamplers.stack(context, noise), xzScale, GenerationFoldContext.verticalShare(xzScale, yScale));
        return NoiseFeatures.warpFactor(context.noiseFeatures(), moment);
    }

    public static double ofField(WorldFold fold, double verticalShare, DoubleSupplier fitted) {
        FoldFeatures owner = FoldFeatures.of(fold);
        return owner == null ? NoiseFeature.UNCHANGED
                : NoiseFeatures.climateFieldFactor(ShapeFeatures.atMoment(owner.features(), NoiseFeature.class),
                        new ClimateFieldMoment(fold, owner.dimension(), verticalShare, fitted));
    }

    private ForeignFieldCompression() {
    }
}

package com.exoticworlds.engine.noise;

import java.util.List;
import java.util.function.DoubleSupplier;

import com.exoticworlds.core.feature.ClimateFieldMoment;
import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseOffsets;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class ForeignFieldCompression {
    public static double followingNoise(WorldFold fold, Holder<NormalNoise.NoiseParameters> noise, double xzScale,
            double yScale) {
        FoldFeatures owner = FoldFeatures.of(fold);
        return owner == null ? NoiseFeature.UNCHANGED
                : followingNoise(fold, owner.dimension(), ShapeFeatures.atMoment(owner.features(), NoiseFeature.class),
                        noise, xzScale, yScale);
    }

    static double followingNoise(WorldFold fold, ResourceKey<LevelStem> dimension, List<NoiseFeature> features,
            Holder<NormalNoise.NoiseParameters> noise, double xzScale, double yScale) {
        return NoiseOrigin.of(features, dimension, noise, new NoiseOffsets())
                .warpFactorAt(fold, xzScale, GenerationFoldContext.verticalShare(xzScale, yScale));
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

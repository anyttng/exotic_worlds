package com.exoticworlds.engine.noise;

import java.util.function.DoubleSupplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.FoldedRandomState;
import com.exoticworlds.core.feature.ClimateFieldMoment;
import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseMoment;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.level.CurrentServer;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class ForeignFieldCompression {
    public static double followingNoise(WorldFold fold, Holder<NormalNoise> noise, double xzScale, double yScale) {
        FoldedCompileContext context = compileContextOf(fold);
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

    private static @Nullable FoldedCompileContext compileContextOf(WorldFold fold) {
        MinecraftServer server = CurrentServer.get();
        if (server == null) {
            return null;
        }

        for (ServerLevel level : server.getAllLevels()) {
            FoldedCompileContext context =
                    ((FoldedRandomState) (Object) level.getChunkSource().randomState()).exotic_worlds$getCompileContext();
            if (context != null && context.fold() == fold) {
                return context;
            }
        }

        return null;
    }

    private ForeignFieldCompression() {
    }
}

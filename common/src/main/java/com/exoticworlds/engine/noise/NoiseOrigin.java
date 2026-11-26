package com.exoticworlds.engine.noise;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseMoment;
import com.exoticworlds.core.feature.NoiseOffset;
import com.exoticworlds.core.feature.NoiseOffsets;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.gen.RouterField;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public record NoiseOrigin(List<NoiseFeature> features, ResourceKey<LevelStem> dimension,
        @Nullable Holder<NormalNoise.NoiseParameters> noise, Set<RouterField> routerFields, NoiseOffsets offsets,
        DoubleList layerFrequencies) {
    public static final NoiseOrigin NONE =
            new NoiseOrigin(List.of(), LevelStem.OVERWORLD, null, Set.of(), new NoiseOffsets(), DoubleList.of());

    public static NoiseOrigin of(List<NoiseFeature> features, ResourceKey<LevelStem> dimension,
            Holder<NormalNoise.NoiseParameters> noise, Set<RouterField> routerFields, NoiseOffsets offsets) {
        return new NoiseOrigin(features, dimension, noise, routerFields, offsets, layerFrequencies(noise.value()));
    }

    public NoiseOrigin withRouterFields(Set<RouterField> fields) {
        return new NoiseOrigin(this.features, this.dimension, this.noise, fields, this.offsets, this.layerFrequencies);
    }

    public double[] layerFactorsAt(WorldFold fold, double xzScale, double verticalShare) {
        Holder<NormalNoise.NoiseParameters> shaped = this.noise;
        if (shaped == null) {
            double[] unchanged = new double[this.layerFrequencies.size()];
            Arrays.fill(unchanged, NoiseFeature.UNCHANGED);
            return unchanged;
        }

        return NoiseFeatures.layerFactors(this.features, momentOf(shaped, fold, xzScale, verticalShare));
    }

    public double warpDivisorAt(WorldFold fold, double xzScale, double verticalShare) {
        Holder<NormalNoise.NoiseParameters> shaped = this.noise;
        return shaped == null ? xzScale
                : NoiseFeatures.warpDivisor(this.features, momentOf(shaped, fold, xzScale, verticalShare));
    }

    public double warpFactorAt(WorldFold fold, double xzScale, double verticalShare) {
        Holder<NormalNoise.NoiseParameters> shaped = this.noise;
        return shaped == null ? NoiseFeature.UNCHANGED
                : NoiseFeatures.warpFactor(this.features, momentOf(shaped, fold, xzScale, verticalShare));
    }

    public NoiseOffset offsetAt(WorldFold fold, double xzScale, double verticalShare) {
        Holder<NormalNoise.NoiseParameters> shaped = this.noise;
        return shaped == null ? NoiseOffset.NONE
                : NoiseFeatures.offsetOf(this.features, this.offsets, momentOf(shaped, fold, xzScale, verticalShare));
    }

    private NoiseMoment momentOf(Holder<NormalNoise.NoiseParameters> shaped, WorldFold fold, double xzScale,
            double verticalShare) {
        return new NoiseMoment(fold, this.dimension, shaped, this.routerFields, this.layerFrequencies, xzScale,
                verticalShare);
    }

    private static DoubleList layerFrequencies(NormalNoise.NoiseParameters parameters) {
        DoubleList frequencies = new DoubleArrayList();
        double frequency = Math.pow(2.0, parameters.firstOctave());
        for (double amplitude : parameters.amplitudes()) {
            if (amplitude != 0.0) {
                frequencies.add(frequency);
                frequencies.add(frequency * NoiseConstants.SECOND_LAYER_DETUNE);
            }

            frequency *= 2.0;
        }

        return DoubleList.of(frequencies.toDoubleArray());
    }
}

package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseMoment;
import com.exoticworlds.core.feature.NoiseOffset;
import com.exoticworlds.core.feature.NoiseOffsets;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.ShapeFixtures;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Holder;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class NoiseFeaturesTest {
    private static final WorldFold FOLD = WorldFolds.of(ShapeFixtures.torus(64));

    private static final Holder<NormalNoise.NoiseParameters> NOISE =
            Holder.direct(new NormalNoise.NoiseParameters(-7, 1.0, 1.0));

    private static final Holder<NormalNoise.NoiseParameters> OTHER_NOISE =
            Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0));

    private static final DoubleList LAYER_FREQUENCIES = DoubleList.of(1.0, NoiseConstants.SECOND_LAYER_DETUNE, 2.0,
            2.0 * NoiseConstants.SECOND_LAYER_DETUNE);

    private static final double XZ_SCALE = 0.25;

    private static final double SHARE = 0.0;

    @Test
    void theLayerFactorsOfTwoFeaturesMultiply() {
        double[] product =
                NoiseFeatures.layerFactors(List.of(new Scaling("a", 2.0), new Scaling("b", 3.0)), moment(NOISE));

        double[] expected = new double[LAYER_FREQUENCIES.size()];
        Arrays.fill(expected, 6.0);
        assertArrayEquals(expected, product);
    }

    @Test
    void theWarpDivisorIsTheScaleTimesTheProductOfTheWarpFactors() {
        assertEquals(XZ_SCALE * 2.0 * 3.0, NoiseFeatures.warpDivisor(
                List.of(new Scaling("a", 2.0), new Scaling("b", 3.0)), moment(NOISE)));
        assertEquals(XZ_SCALE, NoiseFeatures.warpDivisor(List.of(), moment(NOISE)));
    }

    @Test
    void theOffsetsOfTwoFeaturesOnOneNoiseAdd() {
        NoiseOffsets offsets = new NoiseOffsets();
        NoiseOffset offset = NoiseFeatures.offsetOf(List.of(new Offsetting("a"), new Offsetting("b")), offsets,
                moment(NOISE));

        offsets.set("a", 0.25);
        offsets.set("b", 0.5);

        assertEquals(0.75, offset.get());
    }

    @Test
    void aNoiseNoFeatureOffsetsStaysUnmoved() {
        NoiseOffsets offsets = new NoiseOffsets();
        offsets.set("a", 0.25);

        assertSame(NoiseOffset.NONE,
                NoiseFeatures.offsetOf(List.of(new Offsetting("a")), offsets, moment(OTHER_NOISE)));
    }

    @Test
    void anOriginListsTwoDetunedLayersForEveryAudibleOctave() {
        NoiseOrigin origin = NoiseOrigin.of(List.of(), LevelStem.OVERWORLD,
                Holder.direct(new NormalNoise.NoiseParameters(0, 1.0, 0.0, 1.0)), new NoiseOffsets());

        assertEquals(DoubleList.of(1.0, NoiseConstants.SECOND_LAYER_DETUNE, 4.0,
                4.0 * NoiseConstants.SECOND_LAYER_DETUNE), origin.layerFrequencies());
    }

    private static NoiseMoment moment(Holder<NormalNoise.NoiseParameters> noise) {
        return new NoiseMoment(FOLD, LevelStem.OVERWORLD, noise, LAYER_FREQUENCIES, XZ_SCALE, SHARE);
    }

    private record Scaling(String key, double factor) implements NoiseFeature {
        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public double[] layerFactors(NoiseMoment moment) {
            double[] factors = new double[moment.layerFrequencies().size()];
            Arrays.fill(factors, this.factor);
            return factors;
        }

        @Override
        public double warpFactor(NoiseMoment moment) {
            return this.factor;
        }
    }

    private record Offsetting(String key) implements NoiseFeature {
        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public boolean isOffsetting(NoiseMoment moment) {
            return moment.noise() == NOISE;
        }
    }
}

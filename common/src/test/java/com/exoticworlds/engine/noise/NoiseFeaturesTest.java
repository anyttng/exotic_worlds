package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseOffset;
import com.exoticworlds.core.feature.NoiseOffsets;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.ShapeFixtures;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class NoiseFeaturesTest {
    private static final WorldFold FOLD = WorldFolds.of(ShapeFixtures.torus(64));

    private static final NormalNoise PARAMETERS = NormalNoise.createParity(-7, 1.0, 1.0);

    private static final Holder<NormalNoise> NOISE = Holder.direct(PARAMETERS);

    private static final Holder<NormalNoise> OTHER_NOISE = Holder.direct(NormalNoise.createParity(-9, 1.0));

    private static final NoiseStack STACK = (NoiseStack) PARAMETERS.create(new LegacyRandomSource(7L));

    private static final double XZ_SCALE = 0.25;

    private static final double SHARE = 0.0;

    @Test
    void theLayerFactorsOfTwoFeaturesMultiply() {
        double[] product = NoiseFeatures.layerFactors(List.of(new Scaling("a", 2.0), new Scaling("b", 3.0)), FOLD,
                NOISE, STACK, XZ_SCALE, SHARE);

        double[] expected = new double[STACK.layers.length];
        Arrays.fill(expected, 6.0);
        assertArrayEquals(expected, product);
    }

    @Test
    void theWarpDivisorIsTheScaleTimesTheProductOfTheWarpFactors() {
        assertEquals(XZ_SCALE * 2.0 * 3.0, NoiseFeatures.warpDivisor(
                List.of(new Scaling("a", 2.0), new Scaling("b", 3.0)), FOLD, NOISE, XZ_SCALE, SHARE));
        assertEquals(XZ_SCALE, NoiseFeatures.warpDivisor(List.of(), FOLD, NOISE, XZ_SCALE, SHARE));
    }

    @Test
    void theOffsetsOfTwoFeaturesOnOneNoiseAdd() {
        NoiseOffsets offsets = new NoiseOffsets();
        NoiseOffset offset = NoiseFeatures.offsetOf(List.of(new Offsetting("a"), new Offsetting("b")), offsets, NOISE);

        offsets.set("a", 0.25);
        offsets.set("b", 0.5);

        assertEquals(0.75, offset.get());
    }

    @Test
    void aNoiseNoFeatureOffsetsStaysUnmoved() {
        NoiseOffsets offsets = new NoiseOffsets();
        offsets.set("a", 0.25);

        assertSame(NoiseOffset.NONE,
                NoiseFeatures.offsetOf(List.of(new Offsetting("a")), offsets, OTHER_NOISE));
    }

    private record Scaling(String key, double factor) implements NoiseFeature {
        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public double[] layerFactors(WorldFold fold, Holder<NormalNoise> noise, NoiseStack stack, double baseScale,
                double verticalShare) {
            double[] factors = new double[stack.layers.length];
            Arrays.fill(factors, this.factor);
            return factors;
        }

        @Override
        public double warpFactor(WorldFold fold, Holder<NormalNoise> noise, double xzScale, double verticalShare) {
            return this.factor;
        }
    }

    private record Offsetting(String key) implements NoiseFeature {
        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public boolean isOffsetting(Holder<NormalNoise> noise) {
            return noise == NOISE;
        }
    }
}

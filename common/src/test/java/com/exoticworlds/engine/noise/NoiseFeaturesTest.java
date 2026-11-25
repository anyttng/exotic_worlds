package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseOffset;
import com.exoticworlds.core.feature.NoiseOffsets;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.ShapeFixtures;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class NoiseFeaturesTest {
    private static final WorldFold FOLD = WorldFolds.of(ShapeFixtures.torus(64));

    private static final Holder<NormalNoise.NoiseParameters> NOISE =
            Holder.direct(new NormalNoise.NoiseParameters(-7, 1.0, 1.0));

    private static final Holder<NormalNoise.NoiseParameters> OTHER_NOISE =
            Holder.direct(new NormalNoise.NoiseParameters(-9, 1.0));

    private static final DensityFunction.NoiseHolder HOLDER = new DensityFunction.NoiseHolder(NOISE);

    private static final DoubleList AMPLITUDES = DoubleArrayList.of(1.0, 1.0);

    private static final double XZ_SCALE = 0.25;

    private static final double SHARE = 0.0;

    @Test
    void theScaleFactorsOfTwoFeaturesMultiply() {
        assertEquals(6.0, NoiseFeatures.scaleFactor(List.of(new Scaling("a", 2.0), new Scaling("b", 3.0)), FOLD,
                true, AMPLITUDES, 1.0, XZ_SCALE, SHARE));
    }

    @Test
    void theWarpDivisorIsTheScaleTimesTheProductOfTheWarpFactors() {
        assertEquals(XZ_SCALE * 2.0 * 3.0, NoiseFeatures.warpDivisor(
                List.of(new Scaling("a", 2.0), new Scaling("b", 3.0)), FOLD, HOLDER, XZ_SCALE, SHARE));
        assertEquals(XZ_SCALE, NoiseFeatures.warpDivisor(List.of(), FOLD, HOLDER, XZ_SCALE, SHARE));
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
        public double scaleFactor(WorldFold fold, boolean isClimateField, DoubleList amplitudes,
                double lowestFreqInputFactor, double baseScale, double verticalShare) {
            return this.factor;
        }

        @Override
        public double warpFactor(WorldFold fold, DensityFunction.NoiseHolder noise, double xzScale,
                double verticalShare) {
            return this.factor;
        }
    }

    private record Offsetting(String key) implements NoiseFeature {
        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public boolean isOffsetting(Holder<NormalNoise.NoiseParameters> noise) {
            return noise == NOISE;
        }
    }
}

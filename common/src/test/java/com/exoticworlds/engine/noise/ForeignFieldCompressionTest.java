package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.worldgen.LevelClimateCompression;
import com.exoticworlds.core.feature.ClimateFieldMoment;
import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseMoment;
import com.exoticworlds.core.feature.NoiseOffsets;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.shape.feature.compactbiomes.ClimateCompression;
import com.exoticworlds.shape.feature.compactbiomes.ClimateScale;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.generator.NoiseFunction;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class ForeignFieldCompressionTest {
    private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createWorldLookup();

    private static final DensityFunction TEMPERATURE = LOOKUP.lookupOrThrow(Registries.NOISE_SETTINGS)
            .getOrThrow(NoiseGeneratorSettings.OVERWORLD).value().noiseRouter().temperature();

    private static final NoiseFunction CLIMATE = Objects.requireNonNull(ClimateCompression.climateNoiseOf(TEMPERATURE));

    private static final long SEED = 7L;

    private static final int TINY_CHUNK_MIN = -16;

    private static final int TINY_CHUNK_MAX = 16;

    private static final int CUSTOM_FACTOR = 3;

    private static final double SECOND_FACTOR = 2.0;

    private static final double HORIZONTAL_SHARE = 0.0;

    private static final double FITTED = 2.5;

    private static final double NO_COMPRESSION = 1.0;

    private static final double FACTOR_TOLERANCE = 1.0E-9;

    private static final DensityFunction.CompileContext SEEDED = new DensityFunction.CompileContext() {
        @Override
        public Noise createNoiseSampler(Holder<NormalNoise> parameters) {
            return parameters.value().create(new LegacyRandomSource(SEED));
        }

        @Override
        public RandomSource createRandom(Identifier seed) {
            return new LegacyRandomSource(SEED);
        }

        @Override
        @Deprecated
        public RandomSource createEndIslandRandom() {
            return new LegacyRandomSource(SEED);
        }
    };

    @Test
    void aFieldFollowingTheTemperatureNoiseTakesTheFactorTheRouterNoiseTakes() {
        for (ClimateScale scale : List.of(ClimateScale.AUTO, ClimateScale.custom(CUSTOM_FACTOR))) {
            WorldFold fold = tiny(scale);
            double router = LevelClimateCompression.factor(fold, TEMPERATURE);
            String mode = scale.mode().getSerializedName();
            assertTrue(router > NO_COMPRESSION, "the temperature compresses in " + mode);
            assertEquals(router, followingTemperature(fold, List.of(CompactBiomes.FEATURE)), FACTOR_TOLERANCE, mode);
        }
    }

    @Test
    void aFieldFollowingANoiseNoFeatureCompressesStaysUnchanged() {
        assertEquals(NO_COMPRESSION, followingTemperature(tiny(ClimateScale.custom(CUSTOM_FACTOR)), List.of()));
    }

    @Test
    void aSecondNoiseFeatureMultipliesTheFactorOfAFieldFollowingANoise() {
        WorldFold fold = tiny(ClimateScale.custom(CUSTOM_FACTOR));
        assertEquals(CUSTOM_FACTOR * SECOND_FACTOR,
                followingTemperature(fold, List.of(CompactBiomes.FEATURE, new Scaling(SECOND_FACTOR))),
                FACTOR_TOLERANCE);
    }

    @Test
    void aFieldWithoutANoiseTakesTheFactorOfTheRouterBuildItsFoldIsBoundTo() {
        WorldFold fold = tiny(ClimateScale.AUTO);
        List<GenerationFeature> features = List.of(CompactBiomes.FEATURE, new Scaling(SECOND_FACTOR));
        double factor = GenerationFoldContext.withRouterBuild(fold, LevelStem.OVERWORLD, features,
                () -> ForeignFieldCompression.ofField(fold, HORIZONTAL_SHARE, () -> FITTED));
        assertEquals(FITTED * SECOND_FACTOR, factor, FACTOR_TOLERANCE);
    }

    @Test
    void aFieldWithoutANoiseOnAFoldNoBuildHoldsStaysUnchanged() {
        WorldFold fold = tiny(ClimateScale.AUTO);
        WorldFold other = tiny(ClimateScale.AUTO);
        double factor = GenerationFoldContext.withRouterBuild(other, LevelStem.OVERWORLD,
                List.of(CompactBiomes.FEATURE), () -> ForeignFieldCompression.ofField(fold, HORIZONTAL_SHARE,
                        () -> FITTED));
        assertEquals(NO_COMPRESSION, factor);
    }

    private static double followingTemperature(WorldFold fold, List<NoiseFeature> features) {
        FoldedCompileContext context = new FoldedCompileContext(SEEDED, fold, LevelStem.OVERWORLD, features,
                new NoiseOffsets(), NoiseScaleLadder.NONE);
        return ForeignFieldCompression.followingNoise(context, CLIMATE.noise(), CLIMATE.xzScale(), CLIMATE.yScale());
    }

    private static WorldFold tiny(ClimateScale scale) {
        return WorldFolds.of(
                ShapeFixtures.torus(
                        ShapeFixtures.loopedBounds(TINY_CHUNK_MIN, TINY_CHUNK_MAX, TINY_CHUNK_MIN, TINY_CHUNK_MAX)),
                ShapeOptions.DEFAULT.with(CompactBiomes.FEATURE.getOption(), scale));
    }

    private record Scaling(double factor) implements NoiseFeature {
        @Override
        public String getKey() {
            return "scaling";
        }

        @Override
        public double warpFactor(NoiseMoment moment) {
            return this.factor;
        }

        @Override
        public double climateFieldFactor(ClimateFieldMoment moment) {
            return this.factor;
        }
    }
}

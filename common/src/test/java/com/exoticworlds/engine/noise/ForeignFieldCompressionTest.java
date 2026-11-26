package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.feature.ClimateFieldMoment;
import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseMoment;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.gen.DensityNoises;
import com.exoticworlds.core.gen.RouterField;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.shape.feature.compactbiomes.ClimateCompression;
import com.exoticworlds.shape.feature.compactbiomes.ClimateScale;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;
import com.exoticworlds.core.gen.DensityNoises;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

class ForeignFieldCompressionTest {
    private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createLookup();

    private static final List<ResourceKey<NoiseGeneratorSettings>> CLIMATE_ROUTERS = List.of(
            NoiseGeneratorSettings.OVERWORLD, NoiseGeneratorSettings.LARGE_BIOMES, NoiseGeneratorSettings.NETHER);

    private static final int TINY_CHUNK_MIN = -16;

    private static final int TINY_CHUNK_MAX = 16;

    private static final int CUSTOM_FACTOR = 3;

    private static final List<ClimateScale> COMPRESSING_SCALES =
            List.of(ClimateScale.AUTO, ClimateScale.STRONG, ClimateScale.custom(CUSTOM_FACTOR));

    private static final double SECOND_FACTOR = 2.0;

    private static final double HORIZONTAL_SHARE = 0.0;

    private static final boolean IS_CLIMATE_FIELD = true;

    private static final double FITTED = 2.5;

    private static final double NO_COMPRESSION = 1.0;

    private static final double FACTOR_TOLERANCE = 1.0E-9;

    @Test
    void aFieldFollowingARouterTemperatureNoiseTakesTheFactorCompactBiomesGivesThatNoise() {
        for (ResourceKey<NoiseGeneratorSettings> settings : CLIMATE_ROUTERS) {
            DensityNoises.ScaledNoise climate = climateOf(settings);
            for (ClimateScale scale : COMPRESSING_SCALES) {
                WorldFold fold = tiny(scale);
                double expected = ClimateCompression.factor(fold, IS_CLIMATE_FIELD, climate.noise().noiseData(), climate.xzScale(),
                        GenerationFoldContext.verticalShare(climate.xzScale(), climate.yScale()));
                String leg = settings.identifier() + " " + scale.mode().getSerializedName();
                assertEquals(expected, following(fold, climate, List.of(CompactBiomes.FEATURE)), FACTOR_TOLERANCE,
                        leg);
            }
        }
    }

    @Test
    void aFieldFollowingANoiseTakesTheFeaturesOfTheRouterBuildItsFoldIsBoundTo() {
        WorldFold fold = tiny(ClimateScale.custom(CUSTOM_FACTOR));
        DensityNoises.ScaledNoise climate = climateOf(NoiseGeneratorSettings.OVERWORLD);
        double factor = GenerationFoldContext.withRouterBuild(fold, LevelStem.OVERWORLD,
                List.of(CompactBiomes.FEATURE), () -> ForeignFieldCompression.followingNoise(fold,
                        climate.noise().noiseData(), RouterField.TEMPERATURE, climate.xzScale(), climate.yScale()));
        assertEquals(CUSTOM_FACTOR, factor, FACTOR_TOLERANCE);
    }

    @Test
    void aFieldFollowingANoiseOnAFoldNoContextHoldsStaysUnchanged() {
        DensityNoises.ScaledNoise climate = climateOf(NoiseGeneratorSettings.OVERWORLD);
        assertEquals(NO_COMPRESSION, ForeignFieldCompression.followingNoise(tiny(ClimateScale.custom(CUSTOM_FACTOR)),
                climate.noise().noiseData(), RouterField.TEMPERATURE, climate.xzScale(), climate.yScale()));
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
        return following(fold, climateOf(NoiseGeneratorSettings.OVERWORLD), features);
    }

    private static double following(WorldFold fold, DensityNoises.ScaledNoise climate, List<NoiseFeature> features) {
        return ForeignFieldCompression.followingNoise(fold, LevelStem.OVERWORLD, features, climate.noise().noiseData(), RouterField.TEMPERATURE,
                climate.xzScale(), climate.yScale());
    }

    private static DensityNoises.ScaledNoise climateOf(ResourceKey<NoiseGeneratorSettings> settings) {
        DensityFunction temperature = LOOKUP.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(settings).value()
                .noiseRouter().temperature();
        return Objects.requireNonNull(DensityNoises.firstScaledNoiseUnder(temperature));
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

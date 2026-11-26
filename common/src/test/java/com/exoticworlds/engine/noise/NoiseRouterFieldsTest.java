package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.feature.NoiseMoment;
import com.exoticworlds.core.feature.NoiseOffsets;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.gen.DensityNoises;
import com.exoticworlds.core.gen.RouterField;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.shape.feature.compactbiomes.ClimateFields;
import com.exoticworlds.shape.feature.compactbiomes.ClimateScale;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;
import com.exoticworlds.shape.feature.guaranteedland.CoastFields;
import com.exoticworlds.shape.feature.guaranteedland.GuaranteedLand;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class NoiseRouterFieldsTest {
    private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createLookup();

    private static final List<ResourceKey<NoiseGeneratorSettings>> VANILLA_ROUTERS = List.of(
            NoiseGeneratorSettings.OVERWORLD, NoiseGeneratorSettings.LARGE_BIOMES, NoiseGeneratorSettings.AMPLIFIED,
            NoiseGeneratorSettings.NETHER);

    private static final Set<ResourceKey<NormalNoise.NoiseParameters>> KEYED_CLIMATE = Set.of(Noises.TEMPERATURE,
            Noises.TEMPERATURE_LARGE, Noises.TEMPERATURE_NETHER, Noises.VEGETATION, Noises.VEGETATION_LARGE,
            Noises.VEGETATION_NETHER);

    private static final Set<ResourceKey<NormalNoise.NoiseParameters>> KEYED_COAST =
            Set.of(Noises.CONTINENTALNESS, Noises.CONTINENTALNESS_LARGE);

    private static final Holder<NormalNoise.NoiseParameters> OWN_NOISE =
            Holder.direct(new NormalNoise.NoiseParameters(-10, 1.5, 0.0, 1.0));

    private static final double XZ_SCALE = 0.25;

    private static final double NO_Y_SCALE = 0.0;

    private static final double HORIZONTAL_SHARE = 0.0;

    private static final int CUSTOM_FACTOR = 3;

    @Test
    void onTheVanillaRoutersTheClimateNoisesAreTheOnesTheKeyListGave() {
        for (ResourceKey<NoiseGeneratorSettings> settings : VANILLA_ROUTERS) {
            Map<Holder<NormalNoise.NoiseParameters>, Set<RouterField>> byNoise =
                    NoiseRouterFields.of(routerOf(settings)).byNoise();
            assertEquals(keysWhere(byNoise, holder -> KEYED_CLIMATE.contains(keyOf(holder))),
                    keysWhere(byNoise, holder -> ClimateFields.isClimate(byNoise.get(holder))),
                    settings.identifier().toString());
        }
    }

    @Test
    void onTheVanillaRoutersTheCoastNoisesAreTheOnesTheKeyListGave() {
        for (ResourceKey<NoiseGeneratorSettings> settings : VANILLA_ROUTERS) {
            Map<Holder<NormalNoise.NoiseParameters>, Set<RouterField>> byNoise =
                    NoiseRouterFields.of(routerOf(settings)).byNoise();
            assertEquals(keysWhere(byNoise, holder -> KEYED_COAST.contains(keyOf(holder))),
                    keysWhere(byNoise, holder -> CoastFields.isCoast(byNoise.get(holder))),
                    settings.identifier().toString());
        }
    }

    @Test
    void theTemperatureFunctionHoldsItsNoiseAndNotTheWarpNoises() {
        DensityFunction temperature = routerOf(NoiseGeneratorSettings.OVERWORLD).temperature();
        assertEquals(List.of(Noises.TEMPERATURE), DensityNoises.noisesUnder(temperature).stream()
                .map(noise -> keyOf(noise.noiseData())).toList());
    }

    @Test
    void aNoiseOfItsOwnUnderTheTemperatureTakesTheCustomFactor() {
        NoiseRouter router = routerWith(RouterField.TEMPERATURE, DensityFunctions.noise(OWN_NOISE, XZ_SCALE,
                NO_Y_SCALE));
        assertEquals(CUSTOM_FACTOR, CompactBiomes.FEATURE.warpFactor(momentOf(router)));
    }

    @Test
    void aNoiseOfItsOwnUnderTheContinentsIsOffsetByGuaranteedLand() {
        NoiseRouter router = routerWith(RouterField.CONTINENTS, DensityFunctions.noise(OWN_NOISE, XZ_SCALE,
                NO_Y_SCALE));
        assertTrue(GuaranteedLand.FEATURE.isOffsetting(momentOf(router)));
    }

    @Test
    void aNoiseOfItsOwnUnderTheErosionIsNeitherClimateNorCoast() {
        NoiseRouter router = routerWith(RouterField.EROSION, DensityFunctions.noise(OWN_NOISE, XZ_SCALE,
                NO_Y_SCALE));
        Set<RouterField> fields = NoiseRouterFields.of(router).getFields(OWN_NOISE);
        assertFalse(ClimateFields.isClimate(fields));
        assertFalse(CoastFields.isCoast(fields));
    }

    private static NoiseMoment momentOf(NoiseRouter router) {
        WorldFold fold = WorldFolds.of(ShapeFixtures.torus(ShapeFixtures.loopedBounds(-16, 16, -16, 16)),
                ShapeOptions.DEFAULT.with(CompactBiomes.FEATURE.getOption(), ClimateScale.custom(CUSTOM_FACTOR)));
        return new NoiseMoment(fold, LevelStem.OVERWORLD, OWN_NOISE,
                NoiseRouterFields.of(router).getFields(OWN_NOISE),
                NoiseOrigin.of(List.of(), LevelStem.OVERWORLD, OWN_NOISE, Set.of(), new NoiseOffsets())
                        .layerFrequencies(),
                XZ_SCALE, HORIZONTAL_SHARE);
    }

    private static NoiseRouter routerWith(RouterField field, DensityFunction function) {
        DensityFunction zero = DensityFunctions.zero();
        return new NoiseRouter(zero, zero, zero, zero, field == RouterField.TEMPERATURE ? function : zero, zero,
                field == RouterField.CONTINENTS ? function : zero, field == RouterField.EROSION ? function : zero,
                zero, zero, zero, zero, zero, zero, zero);
    }

    private static NoiseRouter routerOf(ResourceKey<NoiseGeneratorSettings> settings) {
        return LOOKUP.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(settings).value().noiseRouter();
    }

    private static Set<ResourceKey<NormalNoise.NoiseParameters>> keysWhere(
            Map<Holder<NormalNoise.NoiseParameters>, Set<RouterField>> byNoise,
            Predicate<Holder<NormalNoise.NoiseParameters>> test) {
        return byNoise.keySet().stream().filter(test).map(NoiseRouterFieldsTest::keyOf).collect(Collectors.toSet());
    }

    private static ResourceKey<NormalNoise.NoiseParameters> keyOf(Holder<NormalNoise.NoiseParameters> noise) {
        return noise.unwrapKey().orElseThrow();
    }
}

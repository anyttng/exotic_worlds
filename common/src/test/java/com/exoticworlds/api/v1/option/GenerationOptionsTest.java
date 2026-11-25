package com.exoticworlds.api.v1.option;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.bridge.v1.option.OptionBridge;
import com.exoticworlds.shape.feature.compactbiomes.ClimateScale;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;
import com.exoticworlds.shape.feature.guaranteedland.GuaranteedLand;

class GenerationOptionsTest {
    private static final String NO_PREFIX = "";
    private static final String STAMP_PREFIX = ExoticWorlds.MODID + ":";

    private static final List<String> PREFIXES = List.of(NO_PREFIX, STAMP_PREFIX);

    private static final WorldOption<ClimateScale> COMPACT_BIOMES = OptionBridge.twin(CompactBiomes.FEATURE.getOption());
    private static final WorldOption<Boolean> GUARANTEED_LAND = OptionBridge.twin(GuaranteedLand.FEATURE.getOption());

    private static final GenerationOptions CHOSEN = GenerationOptions.DEFAULT
            .with(COMPACT_BIOMES, ClimateScale.OFF)
            .with(GUARANTEED_LAND, true);

    @Test
    void anOptionSetToItsDefaultEqualsTheDefaultOptions() {
        for (WorldOption<?> option : WorldOptions.all()) {
            GenerationOptions explicit = withDefault(GenerationOptions.DEFAULT, option);

            assertEquals(GenerationOptions.DEFAULT, explicit, option.key());
            assertEquals(GenerationOptions.DEFAULT.hashCode(), explicit.hashCode(), option.key());
        }
    }

    @Test
    void aChosenValueSetBackToItsDefaultLeavesNoTrace() {
        GenerationOptions reset = CHOSEN
                .with(COMPACT_BIOMES, COMPACT_BIOMES.defaultValue())
                .with(GUARANTEED_LAND, GUARANTEED_LAND.defaultValue());

        assertEquals(GenerationOptions.DEFAULT, reset);
    }

    @Test
    void everyRoundTripComesBackEqual() {
        GenerationOptions explicitDefaults = GenerationOptions.DEFAULT;
        for (WorldOption<?> option : WorldOptions.all()) {
            explicitDefaults = withDefault(explicitDefaults, option);
        }

        for (String prefix : PREFIXES) {
            Codec<GenerationOptions> codec = GenerationOptions.mapCodec(prefix).codec();

            for (GenerationOptions options : List.of(GenerationOptions.DEFAULT, explicitDefaults, CHOSEN)) {
                JsonElement written = codec.encodeStart(JsonOps.INSTANCE, options).getOrThrow();

                assertEquals(options, codec.parse(JsonOps.INSTANCE, written).getOrThrow(), written.toString());
            }
        }
    }

    private static <T> GenerationOptions withDefault(GenerationOptions options, WorldOption<T> option) {
        return options.with(option, option.defaultValue());
    }
}

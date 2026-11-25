package com.exoticworlds.core.option;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.bridge.v1.option.OptionBridge;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

class ShapeOptionsTest {
    private static final List<String> PREFIXES = List.of(CarriedShape.NO_KEY_PREFIX, ExoticWorlds.MODID + ":");

    private static final GenerationOptions CHOSEN_V1 = GenerationOptions.DEFAULT
            .with(OptionBridge.twin(CompactBiomes.FEATURE.getOption()), ClimateScale.OFF)
            .with(OptionBridge.twin(GuaranteedLand.FEATURE.getOption()), true);

    @BeforeAll
    static void registerOptions() {
        StartupRegistrations.options();
    }

    @Test
    void theInternalCodecWritesWhatTheV1CodecWrites() {
        ShapeOptions chosen = OptionBridge.fromV1(CHOSEN_V1);

        for (String prefix : PREFIXES) {
            for (GenerationOptions options : List.of(GenerationOptions.DEFAULT, CHOSEN_V1)) {
                JsonElement fromV1 = GenerationOptions.mapCodec(prefix).codec()
                        .encodeStart(JsonOps.INSTANCE, options).getOrThrow();
                JsonElement fromInternal = ShapeOptions.mapCodec(prefix).codec()
                        .encodeStart(JsonOps.INSTANCE, OptionBridge.fromV1(options)).getOrThrow();

                assertEquals(fromV1, fromInternal, prefix);
            }

            JsonElement written = GenerationOptions.mapCodec(prefix).codec()
                    .encodeStart(JsonOps.INSTANCE, CHOSEN_V1).getOrThrow();
            assertEquals(chosen, ShapeOptions.mapCodec(prefix).codec().parse(JsonOps.INSTANCE, written).getOrThrow());
        }
    }

    @Test
    void valuesComeBackEqualThroughV1() {
        assertEquals(CHOSEN_V1, OptionBridge.toV1(OptionBridge.fromV1(CHOSEN_V1)));
        assertEquals(GenerationOptions.DEFAULT, OptionBridge.toV1(ShapeOptions.DEFAULT));
    }
}

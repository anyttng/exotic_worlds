package com.exoticworlds.api.v2.option;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.WorldOptions;
import com.exoticworlds.bridge.v1.option.OptionBridge;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.OptionKey;
import com.exoticworlds.core.OptionKeys;
import com.exoticworlds.core.OptionValues;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

class ShapeOptionRegistrationTest {
    private static final int CHOSEN_HEIGHT = 12;

    private static final ShapeOption<Integer> HEIGHT =
            new ShapeOption<>("example_terrace_height", 300, Codec.INT, 7);

    private static final OptionKey<Integer> KEY =
            new OptionKey<>(HEIGHT.key(), HEIGHT.position(), HEIGHT.codec(), HEIGHT.defaultValue());

    @BeforeAll
    static void registerOption() {
        if (OptionKeys.all().stream().noneMatch(known -> known.key().equals(HEIGHT.key()))) {
            HEIGHT.register();
        }
    }

    @Test
    void aRegisteredOptionIsKnownInsideAndShownToV1() {
        assertTrue(OptionKeys.all().contains(KEY));
        assertTrue(WorldOptions.all().stream().anyMatch(option -> option.key().equals(HEIGHT.key())));
    }

    @Test
    void itsValueIsStoredAndReadBack() {
        OptionValues chosen = OptionValues.DEFAULT.with(KEY, CHOSEN_HEIGHT);
        Codec<OptionValues> codec = OptionValues.mapCodec(CarriedShape.NO_KEY_PREFIX).codec();

        JsonElement written = codec.encodeStart(JsonOps.INSTANCE, chosen).getOrThrow();

        assertEquals(CHOSEN_HEIGHT, written.getAsJsonObject().get(HEIGHT.key()).getAsInt());
        assertEquals(chosen, codec.parse(JsonOps.INSTANCE, written).getOrThrow());
    }

    @Test
    void itsValueCrossesToV1AndBack() {
        OptionValues chosen = OptionValues.DEFAULT.with(KEY, CHOSEN_HEIGHT);

        assertEquals(chosen, OptionBridge.fromV1(OptionBridge.toV1(chosen)));
    }
}

package com.exoticworlds.api.v2.option;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.WorldOptions;
import com.exoticworlds.bridge.v1.option.OptionBridge;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.core.option.ShapeOptions;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

class ShapeOptionV2RegistrationTest {
    private static final int CHOSEN_HEIGHT = 12;

    private static final ShapeOptionV2<Integer> HEIGHT =
            new ShapeOptionV2<>("example_terrace_height", 300, Codec.INT, 7);

    private static final ShapeOption<Integer> KEY =
            new ShapeOption<>(HEIGHT.key(), HEIGHT.position(), HEIGHT.codec(), HEIGHT.defaultValue());

    @BeforeAll
    static void registerOption() {
        if (ShapeOptionRegistry.getAll().stream().noneMatch(known -> known.key().equals(HEIGHT.key()))) {
            HEIGHT.register();
        }
    }

    @Test
    void aRegisteredOptionIsKnownInsideAndShownToV1() {
        assertTrue(ShapeOptionRegistry.getAll().contains(KEY));
        assertTrue(WorldOptions.all().stream().anyMatch(option -> option.key().equals(HEIGHT.key())));
    }

    @Test
    void itsValueIsStoredAndReadBack() {
        ShapeOptions chosen = ShapeOptions.DEFAULT.with(KEY, CHOSEN_HEIGHT);
        Codec<ShapeOptions> codec = ShapeOptions.mapCodec(CarriedShape.NO_KEY_PREFIX).codec();

        JsonElement written = codec.encodeStart(JsonOps.INSTANCE, chosen).getOrThrow();

        assertEquals(CHOSEN_HEIGHT, written.getAsJsonObject().get(HEIGHT.key()).getAsInt());
        assertEquals(chosen, codec.parse(JsonOps.INSTANCE, written).getOrThrow());
    }

    @Test
    void itsValueCrossesToV1AndBack() {
        ShapeOptions chosen = ShapeOptions.DEFAULT.with(KEY, CHOSEN_HEIGHT);

        assertEquals(chosen, OptionBridge.fromV1(OptionBridge.toV1(chosen)));
    }
}

package com.exoticworlds.api.v2.option;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.Codec;

class ShapeOptionsV2Test {
    private static final int DEFAULT_WIDTH = 64;
    private static final int CHOSEN_WIDTH = 128;

    private static final ShapeOptionV2<Integer> WIDTH =
            new ShapeOptionV2<>("example_band_width", 200, Codec.INT, DEFAULT_WIDTH);
    private static final ShapeOptionV2<Boolean> FLAG = new ShapeOptionV2<>("example_flag", 201, Codec.BOOL, false);

    @Test
    void anOptionNothingWasChosenForReadsItsDefault() {
        assertEquals(DEFAULT_WIDTH, ShapeOptionsV2.DEFAULT.get(WIDTH));
        assertEquals(false, ShapeOptionsV2.DEFAULT.get(FLAG));
    }

    @Test
    void aChosenValueIsReadBackAndLeavesTheOtherOptionsAlone() {
        ShapeOptionsV2 chosen = ShapeOptionsV2.DEFAULT.with(WIDTH, CHOSEN_WIDTH);

        assertEquals(CHOSEN_WIDTH, chosen.get(WIDTH));
        assertEquals(false, chosen.get(FLAG));
        assertEquals(DEFAULT_WIDTH, ShapeOptionsV2.DEFAULT.get(WIDTH));
    }

    @Test
    void aValueSetBackToTheDefaultEqualsNothingChosen() {
        ShapeOptionsV2 reset = ShapeOptionsV2.DEFAULT.with(WIDTH, CHOSEN_WIDTH).with(WIDTH, DEFAULT_WIDTH);

        assertEquals(ShapeOptionsV2.DEFAULT, reset);
        assertEquals(ShapeOptionsV2.DEFAULT.hashCode(), reset.hashCode());
    }

    @Test
    void optionsAreEqualByTheValuesChosen() {
        ShapeOptionsV2 first = ShapeOptionsV2.DEFAULT.with(WIDTH, CHOSEN_WIDTH).with(FLAG, true);
        ShapeOptionsV2 second = ShapeOptionsV2.DEFAULT.with(FLAG, true).with(WIDTH, CHOSEN_WIDTH);

        assertEquals(first, second);
        assertNotEquals(first, ShapeOptionsV2.DEFAULT.with(FLAG, true));
    }
}

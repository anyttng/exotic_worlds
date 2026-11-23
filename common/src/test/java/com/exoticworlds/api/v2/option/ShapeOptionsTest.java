package com.exoticworlds.api.v2.option;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.Codec;

class ShapeOptionsTest {
    private static final int DEFAULT_WIDTH = 64;
    private static final int CHOSEN_WIDTH = 128;

    private static final ShapeOption<Integer> WIDTH =
            new ShapeOption<>("example_band_width", 200, Codec.INT, DEFAULT_WIDTH);
    private static final ShapeOption<Boolean> FLAG = new ShapeOption<>("example_flag", 201, Codec.BOOL, false);

    @Test
    void anOptionNothingWasChosenForReadsItsDefault() {
        assertEquals(DEFAULT_WIDTH, ShapeOptions.DEFAULT.get(WIDTH));
        assertEquals(false, ShapeOptions.DEFAULT.get(FLAG));
    }

    @Test
    void aChosenValueIsReadBackAndLeavesTheOtherOptionsAlone() {
        ShapeOptions chosen = ShapeOptions.DEFAULT.with(WIDTH, CHOSEN_WIDTH);

        assertEquals(CHOSEN_WIDTH, chosen.get(WIDTH));
        assertEquals(false, chosen.get(FLAG));
        assertEquals(DEFAULT_WIDTH, ShapeOptions.DEFAULT.get(WIDTH));
    }

    @Test
    void aValueSetBackToTheDefaultEqualsNothingChosen() {
        ShapeOptions reset = ShapeOptions.DEFAULT.with(WIDTH, CHOSEN_WIDTH).with(WIDTH, DEFAULT_WIDTH);

        assertEquals(ShapeOptions.DEFAULT, reset);
        assertEquals(ShapeOptions.DEFAULT.hashCode(), reset.hashCode());
    }

    @Test
    void optionsAreEqualByTheValuesChosen() {
        ShapeOptions first = ShapeOptions.DEFAULT.with(WIDTH, CHOSEN_WIDTH).with(FLAG, true);
        ShapeOptions second = ShapeOptions.DEFAULT.with(FLAG, true).with(WIDTH, CHOSEN_WIDTH);

        assertEquals(first, second);
        assertNotEquals(first, ShapeOptions.DEFAULT.with(FLAG, true));
    }
}

package com.exoticworlds.api.v2.feature;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OptionPartV2Test {
    private static final int MIN = 8;

    private static final int MAX_BELOW_MIN = 2;

    @Test
    void aNumberPartRefusesBoundsOutOfOrder() {
        assertThrows(IllegalArgumentException.class, () -> OptionPartV2.number(MIN, MAX_BELOW_MIN));
    }
}

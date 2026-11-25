package com.exoticworlds.shape.option;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.world.level.dimension.LevelStem;

class DimensionScopeTest {

    @Test
    void allHoldsEveryDimension() {
        assertTrue(DimensionScope.ALL.contains(LevelStem.OVERWORLD));
        assertTrue(DimensionScope.ALL.contains(LevelStem.END));
    }

    @Test
    void aListHoldsItsOwnDimensionsAlone() {
        DimensionScope overworld = DimensionScope.of(List.of(LevelStem.OVERWORLD));

        assertTrue(overworld.contains(LevelStem.OVERWORLD));
        assertFalse(overworld.contains(LevelStem.NETHER));
    }

    @Test
    void anEmptyListIsRefusedRatherThanReadAsEvery() {
        assertThrows(IllegalArgumentException.class, () -> DimensionScope.of(List.of()));
    }
}

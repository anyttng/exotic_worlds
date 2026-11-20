package com.exoticworlds.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.exoticworlds.shape.torus.GuaranteedNetherComplexes;

class DeclaredOptionsTest {
    @Test
    void theModsOwnOptionsAreDeclaredInTheOrderOfTheirPositions() {
        StartupRegistrations.options();

        assertEquals(List.of(CompactBiomes.SHOWN, GuaranteedLand.SHOWN, GuaranteedNetherComplexes.SHOWN),
                DeclaredOptions.all());
    }
}

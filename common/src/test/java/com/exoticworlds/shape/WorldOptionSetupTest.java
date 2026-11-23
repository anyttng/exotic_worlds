package com.exoticworlds.shape;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.WorldOptions;
import com.exoticworlds.bridge.v1.option.OptionBridge;
import com.exoticworlds.core.ShapeOption;
import com.exoticworlds.core.ShapeOptionRegistry;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.exoticworlds.shape.torus.GuaranteedNetherComplexes;

class WorldOptionSetupTest {
    private static final List<ShapeOption<?>> OWN = List.of(SpawnPoint.OPTION, CompactBiomes.OPTION,
            GuaranteedLand.OPTION, GuaranteedNetherComplexes.OPTION);

    @BeforeAll
    static void registerOptions() {
        StartupRegistrations.options();
    }

    @Test
    void theModsOwnOptionsAreKnownInsideAndShownToV1() {
        for (ShapeOption<?> key : OWN) {
            assertTrue(ShapeOptionRegistry.all().contains(key), key.key());
            assertTrue(WorldOptions.all().contains(OptionBridge.twin(key)), key.key());
        }
    }
}

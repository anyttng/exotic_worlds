package com.exoticworlds.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.WorldOptions;
import com.exoticworlds.bridge.v1.option.OptionBridge;
import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;
import com.exoticworlds.shape.feature.guaranteedland.GuaranteedLand;
import com.exoticworlds.shape.feature.guaranteednethercomplexes.GuaranteedNetherComplexes;

import net.minecraft.core.Direction;

class ShapeSetupTest {
    private static final List<ShapeOption<?>> OWN = List.of(SpawnPoint.OPTION, CompactBiomes.FEATURE.getOption(),
            GuaranteedLand.FEATURE.getOption(), GuaranteedNetherComplexes.FEATURE.getOption());

    @BeforeAll
    static void registerOptions() {
        StartupRegistrations.options();
    }

    @Test
    void theModsOwnOptionsAreKnownInsideAndShownToV1() {
        for (ShapeOption<?> key : OWN) {
            assertTrue(ShapeOptionRegistry.getAll().contains(key), key.key());
            assertTrue(WorldOptions.all().contains(OptionBridge.twin(key)), key.key());
        }
    }

    @Test
    void aShapeStoredWithoutItsIdIsFoundByItsSpans() {
        assertEquals(Optional.of(DeclaredShapes.TORUS),
                new CarriedShape(null, ShapeFixtures.torus(48), ShapeOptions.DEFAULT).findShapeId());
        assertEquals(Optional.of(DeclaredShapes.CYLINDER),
                new CarriedShape(null, ShapeFixtures.cylinder(Direction.Axis.Z, 48), ShapeOptions.DEFAULT).findShapeId());
    }
}

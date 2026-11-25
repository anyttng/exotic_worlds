package com.exoticworlds.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.dimension.DeclaredShapes;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.LevelStem;

class ShapeDerivationsTest {
    private static final Identifier UNREGISTERED = Identifier.fromNamespaceAndPath("example_mod", "unregistered");

    private static final FlatShape OVERWORLD = ShapeFixtures.torus(64);

    @BeforeAll
    static void registerShapes() {
        StartupRegistrations.options();
    }

    @Test
    void aDerivedStemScalesTheSkewWithTheWidths() {
        FlatShape overworld = FlatShape.latticeTorus(WorldLoopBounds.ofWidths(256, 128), 16);

        assertEquals(FlatShape.latticeTorus(WorldLoopBounds.ofWidths(32, 16), 2),
                ShapeDerivations.derive(null, overworld, LevelStem.NETHER, 1.0, 8.0));
    }

    @Test
    void aSkewThatScalesToNoWholeChunkDerivesNothing() {
        FlatShape overworld = FlatShape.latticeTorus(WorldLoopBounds.ofWidths(256, 128), 12);

        assertNull(ShapeDerivations.derive(null, overworld, LevelStem.NETHER, 1.0, 8.0));
    }

    @Test
    void aWorldWithoutAStoredShapeIdKeepsTheOldEndOfTheOverworldsWidth() {
        assertEquals(OVERWORLD, ShapeDerivations.derive(null, OVERWORLD, LevelStem.END, 1.0, 1.0));
    }

    @Test
    void aTorusDerivesItsEndByTheLoopRule() {
        int endWidthChunks = WorldLoopSizes.endWidthChunksFor(64);

        assertEquals(FlatShape.unskewed(WorldLoopBounds.ofWidths(endWidthChunks, endWidthChunks)),
                ShapeDerivations.derive(DeclaredShapes.TORUS, OVERWORLD, LevelStem.END, 1.0, 1.0));
    }

    @Test
    void aCylinderDerivesItsEndOnItsOneLoopedAxis() {
        FlatShape cylinder = ShapeFixtures.cylinder(Direction.Axis.X, 64);

        assertEquals(FlatShape.unskewed(WorldLoopBounds.ofWidth(Direction.Axis.X, WorldLoopSizes.endWidthChunksFor(64))),
                ShapeDerivations.derive(DeclaredShapes.CYLINDER, cylinder, LevelStem.END, 1.0, 1.0));
    }

    @Test
    void aShapeWithNoRuleOfItsOwnTakesTheScaledCopy() {
        assertEquals(ShapeFixtures.torus(16),
                ShapeDerivations.derive(UNREGISTERED, ShapeFixtures.torus(128), LevelStem.NETHER, 1.0, 8.0));
        assertEquals(OVERWORLD, ShapeDerivations.derive(UNREGISTERED, OVERWORLD, LevelStem.END, 1.0, 1.0));
    }

    @Test
    void anOverworldThatLoopsNoAxisDerivesNothing() {
        assertNull(ShapeDerivations.derive(DeclaredShapes.TORUS, FlatShape.rectangle(), LevelStem.END, 1.0, 1.0));
    }
}

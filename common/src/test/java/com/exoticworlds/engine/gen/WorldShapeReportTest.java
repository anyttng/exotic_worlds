package com.exoticworlds.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.WorldLoopPresets;

import net.minecraft.core.Direction;

class WorldShapeReportTest {
    private static final int OVERWORLD_WIDTH_CHUNKS = 192;
    private static final int DIVIDING_NETHER_WIDTH_CHUNKS = 24;
    private static final int UNEVEN_OVERWORLD_WIDTH_CHUNKS = 32;
    private static final int NARROWER_NETHER_WIDTH_CHUNKS = 24;
    private static final int WIDER_NETHER_WIDTH_CHUNKS = 64;
    private static final int NARROW_END_WIDTH_CHUNKS = 128;

    @Test
    void aNetherWidthTheOverworldDividesByNamesItsScale() {
        assertEquals(", scale 1:8", WorldShapeReport.netherScaleText(
                square(OVERWORLD_WIDTH_CHUNKS), square(DIVIDING_NETHER_WIDTH_CHUNKS)));
    }

    @Test
    void aNetherWidthTheOverworldDoesNotDivideByNamesItsReducedRatio() {
        assertEquals(", scale 3:4", WorldShapeReport.netherScaleText(
                square(UNEVEN_OVERWORLD_WIDTH_CHUNKS), square(NARROWER_NETHER_WIDTH_CHUNKS)));
        assertEquals(", scale 2:1", WorldShapeReport.netherScaleText(
                square(UNEVEN_OVERWORLD_WIDTH_CHUNKS), square(WIDER_NETHER_WIDTH_CHUNKS)));
    }

    @Test
    void aCylinderNamesTheRatioOfItsLoopingAxis() {
        assertEquals(", scale 1:8", WorldShapeReport.netherScaleText(
                loopedZ(OVERWORLD_WIDTH_CHUNKS), loopedZ(DIVIDING_NETHER_WIDTH_CHUNKS)));
        assertEquals(", scale 3:4", WorldShapeReport.netherScaleText(
                loopedZ(UNEVEN_OVERWORLD_WIDTH_CHUNKS), loopedZ(NARROWER_NETHER_WIDTH_CHUNKS)));
        assertEquals(", scale 2:1", WorldShapeReport.netherScaleText(
                loopedZ(UNEVEN_OVERWORLD_WIDTH_CHUNKS), loopedZ(WIDER_NETHER_WIDTH_CHUNKS)));
    }

    @Test
    void aNetherThatLoopsOnNeitherOfTheOverworldsAxesSaysNothing() {
        assertEquals("", WorldShapeReport.netherScaleText(loopedZ(OVERWORLD_WIDTH_CHUNKS),
                WorldLoopBounds.ofWidth(Direction.Axis.X, DIVIDING_NETHER_WIDTH_CHUNKS)));
    }

    @Test
    void everyShippedPresetsNetherKeepsItsScale() {
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            int netherWidthChunks = preset.getWidthChunks() / preset.getNetherScale();

            for (String text : List.of(
                    WorldShapeReport.netherScaleText(square(preset.getWidthChunks()), square(netherWidthChunks)),
                    WorldShapeReport.netherScaleText(loopedZ(preset.getWidthChunks()), loopedZ(netherWidthChunks)))) {
                assertEquals(", scale 1:" + preset.getNetherScale(), text, preset.getId());
            }
        }
    }

    @Test
    void aNetherSkewedByTheOverworldsSkewOverTheScaleSaysNothing() {
        assertEquals(WorldShapeReport.Note.NONE, WorldShapeReport.netherSkewNote(
                FlatShape.latticeTorus(square(OVERWORLD_WIDTH_CHUNKS), 16),
                FlatShape.latticeTorus(square(DIVIDING_NETHER_WIDTH_CHUNKS), 2)));
        assertEquals(WorldShapeReport.Note.NONE, WorldShapeReport.netherSkewNote(
                ShapeFixtures.torus(square(OVERWORLD_WIDTH_CHUNKS)), ShapeFixtures.torus(
                        square(DIVIDING_NETHER_WIDTH_CHUNKS))));
    }

    @Test
    void aNetherSkewThatDoesNotMatchTheOverworldsIsNamedAsBroken() {
        WorldShapeReport.Note note = WorldShapeReport.netherSkewNote(
                FlatShape.latticeTorus(square(OVERWORLD_WIDTH_CHUNKS), 16),
                FlatShape.latticeTorus(square(DIVIDING_NETHER_WIDTH_CHUNKS), 3));

        assertTrue(note.isBroken(), note.text());
        assertTrue(note.text().contains("skewed by 16"), note.text());
        assertTrue(note.text().contains("skewed by 2 chunks, not 3"), note.text());
    }

    @Test
    void anOverworldSkewTheScaleDoesNotDivideIsNamedAsBroken() {
        WorldShapeReport.Note note = WorldShapeReport.netherSkewNote(
                FlatShape.latticeTorus(square(OVERWORLD_WIDTH_CHUNKS), 5),
                ShapeFixtures.torus(square(DIVIDING_NETHER_WIDTH_CHUNKS)));

        assertTrue(note.isBroken(), note.text());
        assertTrue(note.text().contains("skewed by 5/8 chunks, not 0"), note.text());
    }

    @Test
    void aNetherSkewScaledByTheWidthRatioOfUnevenWidthsSaysNothing() {
        assertEquals(WorldShapeReport.Note.NONE, WorldShapeReport.netherSkewNote(
                FlatShape.latticeTorus(square(UNEVEN_OVERWORLD_WIDTH_CHUNKS), 4),
                FlatShape.latticeTorus(square(NARROWER_NETHER_WIDTH_CHUNKS), 3)));
        assertEquals(WorldShapeReport.Note.NONE, WorldShapeReport.netherSkewNote(
                FlatShape.latticeTorus(square(UNEVEN_OVERWORLD_WIDTH_CHUNKS), 4),
                FlatShape.latticeTorus(square(WIDER_NETHER_WIDTH_CHUNKS), 8)));
    }

    @Test
    void aNetherSkewThatMissesTheWidthRatioOfUnevenWidthsIsNamedAsBroken() {
        WorldShapeReport.Note note = WorldShapeReport.netherSkewNote(
                FlatShape.latticeTorus(square(UNEVEN_OVERWORLD_WIDTH_CHUNKS), 4),
                FlatShape.latticeTorus(square(NARROWER_NETHER_WIDTH_CHUNKS), 2));

        assertTrue(note.isBroken(), note.text());
        assertTrue(note.text().contains("skewed by 3 chunks, not 2"), note.text());
    }

    @Test
    void anEndAtTheOuterIslandThresholdSaysNothing() {
        assertEquals(WorldShapeReport.Note.NONE,
                WorldShapeReport.endWidthNote(square(WorldLoopSizes.END_MIN_WIDTH_CHUNKS)));
    }

    @Test
    void anEndBelowTheOuterIslandThresholdIsNamedAsBroken() {
        WorldShapeReport.Note note = WorldShapeReport.endWidthNote(square(NARROW_END_WIDTH_CHUNKS));

        assertTrue(note.isBroken(), note.text());
        assertTrue(note.text().contains(WorldLoopSizes.describe(NARROW_END_WIDTH_CHUNKS)), note.text());
        assertTrue(note.text().contains(WorldLoopSizes.describe(WorldLoopSizes.END_MIN_WIDTH_CHUNKS)), note.text());
    }

    @Test
    void aDimensionWithEndTerrainIsHeldToTheThresholdAndOneWithoutIsNot() {
        assertTrue(WorldShapeReport.endWidth(true, square(NARROW_END_WIDTH_CHUNKS)).isBroken());
        assertEquals(WorldShapeReport.Note.NONE, WorldShapeReport.endWidth(false, square(NARROW_END_WIDTH_CHUNKS)));
    }

    @Test
    void aCylinderEndIsHeldToTheThresholdOnItsLoopingAxis() {
        WorldShapeReport.Note note = WorldShapeReport.endWidthNote(loopedZ(NARROW_END_WIDTH_CHUNKS));

        assertTrue(note.isBroken(), note.text());
        assertTrue(note.text().contains(Direction.Axis.Z.getName()), note.text());
        assertEquals(WorldShapeReport.Note.NONE,
                WorldShapeReport.endWidthNote(loopedZ(WorldLoopSizes.END_MIN_WIDTH_CHUNKS)));
    }

    @Test
    void everyShippedPresetsEndClearsTheThreshold() {
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            assertEquals(WorldShapeReport.Note.NONE,
                    WorldShapeReport.endWidthNote(square(preset.getEndWidthChunks())), preset.getId());
            assertEquals(WorldShapeReport.Note.NONE,
                    WorldShapeReport.endWidthNote(loopedZ(preset.getEndWidthChunks())), preset.getId());
        }
    }

    private static WorldLoopBounds square(int widthChunks) {
        return WorldLoopBounds.ofWidth(widthChunks);
    }

    private static WorldLoopBounds loopedZ(int widthChunks) {
        return WorldLoopBounds.ofWidth(Direction.Axis.Z, widthChunks);
    }
}

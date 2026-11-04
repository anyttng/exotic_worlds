package com.exoticworlds.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.shape.WorldLoopPresets;

import net.minecraft.core.Direction;

class WorldShapeReportTest {
    private static final int OVERWORLD_CHUNK_WIDTH = 192;
    private static final int DIVIDING_NETHER_CHUNK_WIDTH = 24;
    private static final int UNEVEN_OVERWORLD_CHUNK_WIDTH = 32;
    private static final int NARROWER_NETHER_CHUNK_WIDTH = 24;
    private static final int WIDER_NETHER_CHUNK_WIDTH = 64;
    private static final int NARROW_END_CHUNK_WIDTH = 128;

    @Test
    void aNetherWidthTheOverworldDividesByNamesItsScale() {
        assertEquals(", scale 1:8", WorldShapeReport.netherScaleText(
                square(OVERWORLD_CHUNK_WIDTH), square(DIVIDING_NETHER_CHUNK_WIDTH)));
    }

    @Test
    void aNetherWidthTheOverworldDoesNotDivideByNamesItsReducedRatio() {
        assertEquals(", scale 3:4", WorldShapeReport.netherScaleText(
                square(UNEVEN_OVERWORLD_CHUNK_WIDTH), square(NARROWER_NETHER_CHUNK_WIDTH)));
        assertEquals(", scale 2:1", WorldShapeReport.netherScaleText(
                square(UNEVEN_OVERWORLD_CHUNK_WIDTH), square(WIDER_NETHER_CHUNK_WIDTH)));
    }

    @Test
    void aCylinderNamesTheRatioOfItsLoopingAxis() {
        assertEquals(", scale 1:8", WorldShapeReport.netherScaleText(
                loopedZ(OVERWORLD_CHUNK_WIDTH), loopedZ(DIVIDING_NETHER_CHUNK_WIDTH)));
        assertEquals(", scale 3:4", WorldShapeReport.netherScaleText(
                loopedZ(UNEVEN_OVERWORLD_CHUNK_WIDTH), loopedZ(NARROWER_NETHER_CHUNK_WIDTH)));
        assertEquals(", scale 2:1", WorldShapeReport.netherScaleText(
                loopedZ(UNEVEN_OVERWORLD_CHUNK_WIDTH), loopedZ(WIDER_NETHER_CHUNK_WIDTH)));
    }

    @Test
    void aNetherThatLoopsOnNeitherOfTheOverworldsAxesSaysNothing() {
        assertEquals("", WorldShapeReport.netherScaleText(loopedZ(OVERWORLD_CHUNK_WIDTH),
                WorldLoopBounds.ofWidth(Direction.Axis.X, DIVIDING_NETHER_CHUNK_WIDTH)));
    }

    @Test
    void everyShippedPresetsNetherKeepsItsScale() {
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            int netherChunkWidth = preset.chunkWidth() / preset.netherScale();

            for (String text : List.of(
                    WorldShapeReport.netherScaleText(square(preset.chunkWidth()), square(netherChunkWidth)),
                    WorldShapeReport.netherScaleText(loopedZ(preset.chunkWidth()), loopedZ(netherChunkWidth)))) {
                assertEquals(", scale 1:" + preset.netherScale(), text, preset.id());
            }
        }
    }

    @Test
    void aNetherSkewedByTheOverworldsSkewOverTheScaleSaysNothing() {
        assertEquals(WorldShapeReport.Note.NONE, WorldShapeReport.netherSkewNote(
                FlatShape.latticeTorus(square(OVERWORLD_CHUNK_WIDTH), 16),
                FlatShape.latticeTorus(square(DIVIDING_NETHER_CHUNK_WIDTH), 2)));
        assertEquals(WorldShapeReport.Note.NONE, WorldShapeReport.netherSkewNote(
                ShapeFixtures.torus(square(OVERWORLD_CHUNK_WIDTH)), ShapeFixtures.torus(
                        square(DIVIDING_NETHER_CHUNK_WIDTH))));
    }

    @Test
    void aNetherSkewThatDoesNotMatchTheOverworldsIsNamedAsBroken() {
        WorldShapeReport.Note note = WorldShapeReport.netherSkewNote(
                FlatShape.latticeTorus(square(OVERWORLD_CHUNK_WIDTH), 16),
                FlatShape.latticeTorus(square(DIVIDING_NETHER_CHUNK_WIDTH), 3));

        assertTrue(note.broken(), note.text());
        assertTrue(note.text().contains("skewed by 16"), note.text());
        assertTrue(note.text().contains("skewed by 2 chunks, not 3"), note.text());
    }

    @Test
    void anOverworldSkewTheScaleDoesNotDivideIsNamedAsBroken() {
        WorldShapeReport.Note note = WorldShapeReport.netherSkewNote(
                FlatShape.latticeTorus(square(OVERWORLD_CHUNK_WIDTH), 5),
                ShapeFixtures.torus(square(DIVIDING_NETHER_CHUNK_WIDTH)));

        assertTrue(note.broken(), note.text());
        assertTrue(note.text().contains("skewed by 5/8 chunks, not 0"), note.text());
    }

    @Test
    void aNetherSkewScaledByTheWidthRatioOfUnevenWidthsSaysNothing() {
        assertEquals(WorldShapeReport.Note.NONE, WorldShapeReport.netherSkewNote(
                FlatShape.latticeTorus(square(UNEVEN_OVERWORLD_CHUNK_WIDTH), 4),
                FlatShape.latticeTorus(square(NARROWER_NETHER_CHUNK_WIDTH), 3)));
        assertEquals(WorldShapeReport.Note.NONE, WorldShapeReport.netherSkewNote(
                FlatShape.latticeTorus(square(UNEVEN_OVERWORLD_CHUNK_WIDTH), 4),
                FlatShape.latticeTorus(square(WIDER_NETHER_CHUNK_WIDTH), 8)));
    }

    @Test
    void aNetherSkewThatMissesTheWidthRatioOfUnevenWidthsIsNamedAsBroken() {
        WorldShapeReport.Note note = WorldShapeReport.netherSkewNote(
                FlatShape.latticeTorus(square(UNEVEN_OVERWORLD_CHUNK_WIDTH), 4),
                FlatShape.latticeTorus(square(NARROWER_NETHER_CHUNK_WIDTH), 2));

        assertTrue(note.broken(), note.text());
        assertTrue(note.text().contains("skewed by 3 chunks, not 2"), note.text());
    }

    @Test
    void anEndAtTheOuterIslandThresholdSaysNothing() {
        assertEquals(WorldShapeReport.Note.NONE,
                WorldShapeReport.endWidthNote(square(WorldLoopSizes.END_MIN_CHUNK_WIDTH)));
    }

    @Test
    void anEndBelowTheOuterIslandThresholdIsNamedAsBroken() {
        WorldShapeReport.Note note = WorldShapeReport.endWidthNote(square(NARROW_END_CHUNK_WIDTH));

        assertTrue(note.broken(), note.text());
        assertTrue(note.text().contains(WorldLoopSizes.describe(NARROW_END_CHUNK_WIDTH)), note.text());
        assertTrue(note.text().contains(WorldLoopSizes.describe(WorldLoopSizes.END_MIN_CHUNK_WIDTH)), note.text());
    }

    @Test
    void aCylinderEndIsHeldToTheThresholdOnItsLoopingAxis() {
        WorldShapeReport.Note note = WorldShapeReport.endWidthNote(loopedZ(NARROW_END_CHUNK_WIDTH));

        assertTrue(note.broken(), note.text());
        assertTrue(note.text().contains(Direction.Axis.Z.getName()), note.text());
        assertEquals(WorldShapeReport.Note.NONE,
                WorldShapeReport.endWidthNote(loopedZ(WorldLoopSizes.END_MIN_CHUNK_WIDTH)));
    }

    @Test
    void everyShippedPresetsEndClearsTheThreshold() {
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            assertEquals(WorldShapeReport.Note.NONE,
                    WorldShapeReport.endWidthNote(square(preset.endChunkWidth())), preset.id());
            assertEquals(WorldShapeReport.Note.NONE,
                    WorldShapeReport.endWidthNote(loopedZ(preset.endChunkWidth())), preset.id());
        }
    }

    private static WorldLoopBounds square(int chunkWidth) {
        return WorldLoopBounds.ofWidth(chunkWidth);
    }

    private static WorldLoopBounds loopedZ(int chunkWidth) {
        return WorldLoopBounds.ofWidth(Direction.Axis.Z, chunkWidth);
    }
}

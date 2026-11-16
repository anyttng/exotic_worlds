package com.exoticworlds.compat.distanthorizons;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.sql.dto.BeaconBeamDTO;

import net.minecraft.core.Direction;

class DhKeysRenderTreeTest {
    private static final byte LEAF = DhKeys.LEAF;
    private static final byte SECTION_128 = 7;
    private static final byte SECTION_256 = 8;
    private static final byte SECTION_512 = 9;
    private static final byte SECTION_2048 = 11;
    private static final byte TOP_LEVEL = 12;
    private static final byte FARTHEST_EXPECTED = 8;
    private static final int EDGE_REACH = 3;
    private static final int WIDTH_CHUNKS = 64;
    private static final int WIDTH_BLOCKS = WIDTH_CHUNKS * 16;
    private static final int LEAVES_PER_WORLD = WIDTH_BLOCKS / 64;
    private static final int WIDE_HALF_CHUNKS = 7500;

    private final DhLattice torus = torus(0, WIDTH_CHUNKS);
    private final DhLattice tiny = torus(-16, 16);
    private final DhLattice odd = torus(0, 101);
    private final DhLattice wide = torus(-WIDE_HALF_CHUNKS, WIDE_HALF_CHUNKS);

    private static DhLattice torus(int minChunk, int maxChunk) {
        AxisBounds.Looped looped = new AxisBounds.Looped(minChunk, maxChunk);
        return DhLattice.of(WorldFolds.of(ShapeFixtures.torus(new WorldLoopBounds(looped, looped))));
    }

    private static DhLattice cylinder(int minChunk, int maxChunk) {
        AxisBounds.Looped looped = new AxisBounds.Looped(minChunk, maxChunk);
        return DhLattice.of(
                WorldFolds.of(ShapeFixtures.torus(new WorldLoopBounds(looped, AxisBounds.Unbounded.INSTANCE))));
    }

    private static DhLattice skewed(int skewChunks) {
        return DhLattice.of(WorldFolds.of(
                FlatShape.latticeTorus(WorldLoopBounds.ofWidths(WIDTH_CHUNKS, WIDTH_CHUNKS), skewChunks)));
    }

    private static byte treeDetail(DhLattice lattice, int refBlockX, int refBlockZ, long pos) {
        return DhKeys.completeDetail(lattice, pos,
                DhKeys.straddlerDetail(lattice, refBlockX, refBlockZ, pos, FARTHEST_EXPECTED));
    }

    @Test
    void aCoarseSectionInsideTheFirstLapOfAWideWorldKeepsItsDistanceDetail() {
        assertEquals(5, DhKeys.completeDetail(this.wide, DhSectionPos.encode(SECTION_2048, 0, 0), (byte) 5),
                "the world width shares only 2^7 with the section grid and its min block 2^6");
    }

    @Test
    void aCoarseSectionPastTheSeamOfAWideWorldSplits() {
        assertEquals(SECTION_256 - 1 - LEAF,
                DhKeys.completeDetail(this.wide, DhSectionPos.encode(SECTION_256, 469, 0), (byte) 5),
                "blocks 120064..120319 fold by two laps and stay past the 120000 block edge");
    }

    @Test
    void aCompleteSectionUnderAnIncompleteParentStopsAtTheParentLevel() {
        assertEquals(SECTION_256 - LEAF,
                DhKeys.completeDetail(this.wide, DhSectionPos.encode(SECTION_128, 938, 0), (byte) 5),
                "blocks 120064..120191 fold by one lap onto -119936..-119809");
    }

    @Test
    void aSectionOnTheSeamOfAWideWorldSplitsToTheLeaf() {
        assertEquals(0, DhKeys.completeDetail(this.wide, DhSectionPos.encode(SECTION_128, 937, 0), (byte) 5));
    }

    @Test
    void aSectionWiderThanTheWorldSplitsAsTheWorldCapDid() {
        assertEquals(SECTION_2048 - 1 - LEAF,
                DhKeys.completeDetail(this.torus, DhSectionPos.encode(SECTION_2048, 0, 0), (byte) 7));
    }

    @Test
    void aChildOfASectionSplitForWhatCannotDrawIsNeverAskedForMoreThanOneLevelAboveItsOwn() {
        for (DhLattice lattice : List.of(this.wide, this.odd, torus(-50, 50), cylinder(0, 101), skewed(1),
                skewed(4))) {
            int min = lattice.shape().minBlock(Direction.Axis.X);
            int max = lattice.shape().maxBlock(Direction.Axis.X);
            for (int ref : new int[] {0, max - 10}) {
                for (byte level = LEAF + 1; level <= TOP_LEVEL; level++) {
                    for (int x : sectionsAround(level, min, max)) {
                        for (int z : sectionsAround(level, min, max)) {
                            long parent = DhSectionPos.encode(level, x, z);
                            if (treeDetail(lattice, ref, ref, parent) >= level - LEAF) {
                                continue;
                            }

                            for (int child = 0; child < 4; child++) {
                                long pos = DhSectionPos.getChildByIndex(parent, child);
                                assertTrue(treeDetail(lattice, ref, ref, pos) <= level - LEAF,
                                        DhSectionPos.toString(pos) + " ref " + ref);
                            }
                        }
                    }
                }
            }
        }
    }

    private static int[] sectionsAround(byte level, int minBlock, int maxBlock) {
        int width = DhFold.sectionWidthBlocks(level);
        int[] sections = new int[4 * EDGE_REACH];
        for (int i = 0; i < 2 * EDGE_REACH; i++) {
            sections[i] = Math.floorDiv(minBlock, width) - EDGE_REACH + i;
            sections[2 * EDGE_REACH + i] = Math.floorDiv(maxBlock, width) - EDGE_REACH + i;
        }

        return sections;
    }

    @Test
    void aSectionStraddlingTheNearestWindowSplitsDownToTheSnapLevel() {
        long straddler = DhSectionPos.encode(SECTION_512, 0, 0);
        assertEquals(0, DhKeys.straddlerDetail(this.tiny, 0, 0, straddler, (byte) 5),
                "a 512 block section covers the whole 512 block world, so its far half is a copy");
    }

    @Test
    void aChildOfAStraddlerStopsOneLevelBelowIt() {
        long child = DhSectionPos.encode(SECTION_256, 0, 0);
        assertEquals(SECTION_256 + 1 - LEAF, DhKeys.straddlerDetail(this.tiny, 0, 0, child, (byte) 5));
    }

    @Test
    void aSectionInsideTheNearestWindowKeepsItsDetail() {
        long inside = DhSectionPos.encode(LEAF, 2, 2);
        assertEquals(5, DhKeys.straddlerDetail(this.torus, 0, 0, inside, (byte) 5));
    }

    @Test
    void aSectionHangingOverTheWorldEdgeIsRefusedAndItsCompleteNeighborDrawn() {
        assertFalse(DhKeys.drawsSection(this.odd, 1600, 384, DhSectionPos.encode(SECTION_256, 6, 1)),
                "blocks 1536..1791 run past the 1616 block world");
        assertTrue(DhKeys.drawsSection(this.odd, 1600, 384, DhSectionPos.encode(SECTION_256, 5, 1)));
    }

    @Test
    void onlyTheNearestCopyOfACompleteSectionIsDrawn() {
        assertTrue(DhKeys.drawsSection(this.torus, 60, 0, DhSectionPos.encode(LEAF, 4, 0)));
        assertFalse(DhKeys.drawsSection(this.torus, 60, 0, DhSectionPos.encode(LEAF, 4 - LEAVES_PER_WORLD, 0)));
    }

    @Test
    void aBeamIsKeptOnTheNearestCopyAndDroppedALapAway() {
        BeaconBeamDTO near = beam(4 * 64 + 10, 10);
        BeaconBeamDTO far = beam(4 * 64 + 10 - WIDTH_BLOCKS, 10);
        List<BeaconBeamDTO> beams = new ArrayList<>(List.of(near, far));
        DhKeys.keepNearestBeams(this.torus, 60, 0, beams);
        assertEquals(List.of(near), beams);
    }

    @Test
    void anUnshapedLevelPassesEverythingThrough() {
        long pos = DhSectionPos.encode(SECTION_512, 3, -7);
        assertEquals(7, DhKeys.completeDetail(null, pos, (byte) 7));
        assertEquals(5, DhKeys.straddlerDetail(null, 0, 0, pos, (byte) 5));
        assertTrue(DhKeys.drawsSection(null, 0, 0, pos));
        assertTrue(DhKeys.isNearestCopy(null, 0, 0, pos));
        assertEquals(pos, DhKeys.nearestSection(null, 0, 0, pos));

        List<BeaconBeamDTO> beams = new ArrayList<>(List.of(beam(0, 0), beam(-WIDTH_BLOCKS, 0)));
        DhKeys.keepNearestBeams(null, 0, 0, beams);
        assertEquals(2, beams.size());
        assertDoesNotThrow(() -> DhKeys.keepNearestBeams(this.torus, 0, 0, null));
    }

    private static BeaconBeamDTO beam(int x, int z) {
        return new BeaconBeamDTO(new DhBlockPos(x, 64, z), Color.WHITE);
    }
}

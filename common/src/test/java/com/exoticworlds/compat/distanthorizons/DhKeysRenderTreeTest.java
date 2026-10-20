package com.exoticworlds.compat.distanthorizons;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.sql.dto.BeaconBeamDTO;

class DhKeysRenderTreeTest {
    private static final byte LEAF = DhKeys.LEAF;
    private static final byte SECTION_256 = 8;
    private static final byte SECTION_512 = 9;
    private static final int WIDTH_CHUNKS = 64;
    private static final int WIDTH_BLOCKS = WIDTH_CHUNKS * 16;
    private static final int LEAVES_PER_WORLD = WIDTH_BLOCKS / 64;

    private final DhLattice torus = torus(0, WIDTH_CHUNKS);
    private final DhLattice tiny = torus(-16, 16);
    private final DhLattice odd = torus(0, 101);

    private static DhLattice torus(int minChunk, int maxChunk) {
        AxisBounds.Looped looped = new AxisBounds.Looped(minChunk, maxChunk);
        return DhLattice.of(WorldFolds.of(ShapeFixtures.torus(new WorldLoopBounds(looped, looped))));
    }

    @Test
    void theExpectedDetailStopsAtTheCoarsestLevelThatFoldsExactly() {
        assertEquals(4, DhKeys.cappedDetail(this.torus, (byte) 7));
        assertEquals(2, DhKeys.cappedDetail(this.torus, (byte) 2));
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
        assertEquals(7, DhKeys.cappedDetail(null, (byte) 7));
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

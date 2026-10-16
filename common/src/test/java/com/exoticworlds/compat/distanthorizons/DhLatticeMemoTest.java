package com.exoticworlds.compat.distanthorizons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;

class DhLatticeMemoTest {
    private static final int SMALL_HALF_CHUNKS = 16;
    private static final int LARGE_HALF_CHUNKS = 32;

    private static WorldFold torus(int halfChunks) {
        AxisBounds.Looped looped = new AxisBounds.Looped(-halfChunks, halfChunks);
        return WorldFolds.of(FlatShape.torus(new WorldLoopBounds(looped, looped)));
    }

    @Test
    void theSameFoldHandsOutTheSameLattice() {
        DhLatticeMemo memo = new DhLatticeMemo();
        WorldFold fold = torus(SMALL_HALF_CHUNKS);

        assertSame(memo.latticeOf(fold), memo.latticeOf(fold));
    }

    @Test
    void aRepublishedFoldIsPickedUpOnTheNextRead() {
        DhLatticeMemo memo = new DhLatticeMemo();
        WorldFold small = torus(SMALL_HALF_CHUNKS);
        WorldFold large = torus(LARGE_HALF_CHUNKS);

        DhLattice first = memo.latticeOf(small);
        DhLattice second = memo.latticeOf(large);
        DhLattice third = memo.latticeOf(small);

        assertEquals(512, first.shape().widthBlocks(Direction.Axis.X));
        assertEquals(1024, second.shape().widthBlocks(Direction.Axis.X));
        assertEquals(512, third.shape().widthBlocks(Direction.Axis.X));
        assertNotSame(first, third);
    }

    @Test
    void noFoldGivesNoLattice() {
        assertNull(new DhLatticeMemo().latticeOf(null));
    }
}

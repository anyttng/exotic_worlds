package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import net.minecraft.world.level.ChunkPos;

class XaeroLeafFanTest {
    private static final int SIDE = 2;
    private static final long FIRST = ChunkPos.pack(-60, 0);
    private static final long SECOND = ChunkPos.pack(-60, 1);
    private static final long THIRD = ChunkPos.pack(-59, 0);

    @Test
    void theFirstColumnInRangeGrowsByTheFannedLeaves() {
        XaeroLeafFan fan = begun();
        assertEquals(SIDE + 3, fan.extendColumn(SIDE, 0, 0, 0, 0, 0, 0, 1, (x, z) -> new long[] {FIRST, SECOND, THIRD}));
        assertEquals(SIDE, fan.extendColumn(SIDE, 0, 0, 0, 0, 1, 0, 1, (x, z) -> new long[] {FIRST}),
                "the second column of the same region keeps Xaero's own bound");
    }

    @Test
    void theRowsPastTheRegionReadTheFannedLeavesInOrder() {
        XaeroLeafFan fan = begun();
        fan.extendColumn(SIDE, 0, 0, 0, 0, 0, 0, 1, (x, z) -> new long[] {FIRST, SECOND, THIRD});
        assertEquals(FIRST, fan.leaf(0, 2), "the first row past a two-row region");
        assertEquals(THIRD, fan.leaf(0, 4));
        assertEquals(XaeroLeafFan.NO_LEAF, fan.leaf(0, 5));
        assertEquals(XaeroLeafFan.NO_LEAF, fan.leaf(0, 1), "a row inside the region is Xaero's own");
        assertEquals(XaeroLeafFan.NO_LEAF, fan.leaf(1, 2), "only the chosen column fans");
    }

    @Test
    void aFannedRowIsAdmittedAndHidesXaerosOwnExistenceCheck() {
        XaeroLeafFan fan = begun();
        fan.extendColumn(SIDE, 0, 0, 0, 0, 0, 0, 1, (x, z) -> new long[] {FIRST});
        assertEquals(Integer.MAX_VALUE, fan.admitRow(1, 0, 2));
        assertEquals(1, fan.admitRow(1, 0, 1));
        assertFalse(fan.exists(true, 0, 2));
        assertTrue(fan.exists(true, 0, 1));
    }

    @Test
    void aColumnOutsideTheLeafRangeIsNeverChosen() {
        XaeroLeafFan fan = begun();
        AtomicInteger asked = new AtomicInteger();
        assertEquals(SIDE, fan.extendColumn(SIDE, 0, 0, 0, 0, 0, 1, 1, (x, z) -> {
            asked.incrementAndGet();
            return new long[] {FIRST};
        }));
        assertEquals(0, asked.get(), "column 0 lies left of the leaf range 1..1");
        assertEquals(SIDE + 1, fan.extendColumn(SIDE, 0, 0, 0, 0, 1, 1, 1, (x, z) -> {
            asked.incrementAndGet();
            return new long[] {FIRST};
        }));
        assertEquals(1, asked.get());
    }

    @Test
    void aLeafAnEarlierRegionTookIsNotDrawnTwice() {
        XaeroLeafFan fan = begun();
        fan.extendColumn(SIDE, 0, 0, 0, 0, 0, 0, 1, (x, z) -> new long[] {FIRST, SECOND});
        assertEquals(SIDE + 1, fan.extendColumn(SIDE, 0, 1, 0, 2, 0, 0, 1, (x, z) -> new long[] {SECOND, THIRD}),
                "the next region fans only the leaf the first one left");
        assertEquals(THIRD, fan.leaf(0, 4));
    }

    @Test
    void aNewFrameForgetsTheTakenLeaves() {
        XaeroLeafFan fan = begun();
        fan.extendColumn(SIDE, 0, 0, 0, 0, 0, 0, 1, (x, z) -> new long[] {FIRST});
        fan.begin();
        assertEquals(SIDE + 1, fan.extendColumn(SIDE, 0, 0, 0, 0, 0, 0, 1, (x, z) -> new long[] {FIRST}));
    }

    private static XaeroLeafFan begun() {
        XaeroLeafFan fan = new XaeroLeafFan();
        fan.begin();
        return fan;
    }
}

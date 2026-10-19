package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.world.level.ChunkPos;

class XaeroFannedSweepTest {
    @Test
    void aFannedRegionTheViewAlreadyDrewIsNotKeptAgain() {
        XaeroFannedSweep sweep = new XaeroFannedSweep();
        sweep.begin();
        sweep.seen(0, 0);
        sweep.fanned(new long[] {ChunkPos.pack(0, 0), ChunkPos.pack(-1, 0)});
        assertEquals(Set.of(ChunkPos.pack(-1, 0)), unseen(sweep));
    }

    @Test
    void aNewFrameStartsEmpty() {
        XaeroFannedSweep sweep = new XaeroFannedSweep();
        sweep.fanned(new long[] {ChunkPos.pack(-1, 0)});
        sweep.begin();
        assertEquals(Set.of(), unseen(sweep));
    }

    private static Set<Long> unseen(XaeroFannedSweep sweep) {
        Set<Long> kept = new HashSet<>();
        sweep.forEachUnseen(kept::add);
        return kept;
    }
}

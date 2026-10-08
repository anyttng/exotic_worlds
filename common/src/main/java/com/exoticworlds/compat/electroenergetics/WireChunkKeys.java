package com.exoticworlds.compat.electroenergetics;

import com.exoticworlds.core.WorldFold;

import it.unimi.dsi.fastutil.longs.LongIterator;

public final class WireChunkKeys implements LongIterator {
    private final LongIterator raw;
    private final WorldFold fold;

    private WireChunkKeys(LongIterator raw, WorldFold fold) {
        this.raw = raw;
        this.fold = fold;
    }

    public static LongIterator of(LongIterator raw, WorldFold fold) {
        return new WireChunkKeys(raw, fold);
    }

    @Override
    public boolean hasNext() {
        return this.raw.hasNext();
    }

    @Override
    public long nextLong() {
        return this.fold.foldChunkKey(this.raw.nextLong());
    }
}

package com.exoticworlds.engine.gen.structure;

import com.exoticworlds.core.fold.WrapDomain;

public final class SectorGridAxis {
    private final WrapDomain domain;
    private final int spacingChunks;
    private final int originChunk;
    private final boolean isClosed;
    private final int gridMin;
    private final int gridCount;
    private final int originGrid;
    private final int offsetCap;

    private SectorGridAxis(WrapDomain domain, int spacingChunks, int originChunk) {
        this.domain = domain;
        this.spacingChunks = spacingChunks;
        this.originChunk = originChunk;
        this.isClosed = domain.stepsToCoverTheWorld(spacingChunks) != Integer.MAX_VALUE;
        if (this.isClosed) {
            int lowestCell = Math.floorDiv(domain.lowerBound, spacingChunks);
            int highestCell = Math.floorDiv(domain.upperBound - 1, spacingChunks);
            this.gridMin = lowestCell;
            this.gridCount = highestCell - lowestCell + 1;
            this.originGrid = Math.floorDiv(originChunk, spacingChunks);
            this.offsetCap = this.gridCount / 2;
        } else {
            this.gridMin = 0;
            this.gridCount = 0;
            this.originGrid = 0;
            this.offsetCap = Integer.MAX_VALUE;
        }
    }

    public static SectorGridAxis of(WrapDomain chunkDomain, int spacingChunks, int originChunk) {
        return new SectorGridAxis(chunkDomain, spacingChunks, originChunk);
    }

    public int getOffsetCap() {
        return offsetCap;
    }

    public int probeChunk(int offset) {
        if (!isClosed) {
            return originChunk + spacingChunks * offset;
        }

        int cell = gridMin + Math.floorMod(originGrid + offset - gridMin, gridCount);
        return Math.max(cell * spacingChunks, domain.lowerBound);
    }

    public int lapsCrossed(int offset) {
        return isClosed ? Math.floorDiv(originGrid + offset - gridMin, gridCount) : 0;
    }

    public int probeChunkShifted(int offset, int shiftChunks) {
        int probe = probeChunk(offset);
        if (shiftChunks == 0 || !isClosed) {
            return probe;
        }

        return Math.max(Math.floorDiv(domain.wrap(probe + shiftChunks), spacingChunks) * spacingChunks, domain.lowerBound);
    }
}

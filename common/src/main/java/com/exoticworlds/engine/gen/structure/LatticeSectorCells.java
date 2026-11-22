package com.exoticworlds.engine.gen.structure;

import java.util.List;

import com.exoticworlds.core.TranslationLattice;

import net.minecraft.world.level.ChunkPos;

final class LatticeSectorCells implements SectorCells {
    private final int spacing;
    private final int skew;
    private final SectorGridAxis xCells;
    private final SectorGridAxis zCells;

    LatticeSectorCells(TranslationLattice lattice, int spacing, ChunkPos center) {
        this.spacing = spacing;
        this.skew = lattice.skew();
        this.xCells = SectorGridAxis.of(lattice.x(), spacing, center.x());
        this.zCells = SectorGridAxis.of(lattice.z(), spacing, center.z());
    }

    @Override
    public int offsetCap() {
        return Math.max(this.xCells.offsetCap(), this.zCells.offsetCap());
    }

    @Override
    public List<Read> reads(int x, int z) {
        if (Math.abs(x) > this.xCells.offsetCap() || Math.abs(z) > this.zCells.offsetCap()) {
            return List.of();
        }

        int probeX = this.xCells.probeChunkShifted(x, -this.zCells.lapsCrossed(z) * this.skew);
        int probeZ = this.zCells.probeChunk(z);
        return List.of(Read.whole(Math.floorDiv(probeX, this.spacing), Math.floorDiv(probeZ, this.spacing)));
    }
}

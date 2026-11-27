package com.exoticworlds.engine.gen.structure;

import java.util.List;

import com.exoticworlds.core.fold.TranslationLattice;

import net.minecraft.world.level.ChunkPos;

final class LatticeSectorCells implements SectorCells {
    private final int spacing;
    private final int skewChunks;
    private final SectorGridAxis xCells;
    private final SectorGridAxis zCells;

    LatticeSectorCells(TranslationLattice lattice, int spacing, ChunkPos center) {
        this.spacing = spacing;
        this.skewChunks = lattice.getSkew();
        this.xCells = SectorGridAxis.of(lattice.getX(), spacing, center.x());
        this.zCells = SectorGridAxis.of(lattice.getZ(), spacing, center.z());
    }

    @Override
    public int getOffsetCap() {
        return Math.max(this.xCells.getOffsetCap(), this.zCells.getOffsetCap());
    }

    @Override
    public List<Read> reads(int x, int z) {
        if (Math.abs(x) > this.xCells.getOffsetCap() || Math.abs(z) > this.zCells.getOffsetCap()) {
            return List.of();
        }

        int probeX = this.xCells.probeChunkShifted(x, -this.zCells.lapsCrossed(z) * this.skewChunks);
        int probeZ = this.zCells.probeChunk(z);
        return List.of(Read.whole(Math.floorDiv(probeX, this.spacing), Math.floorDiv(probeZ, this.spacing)));
    }
}

package com.exoticworlds.engine.gen.structure;

import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

final class SplitSectorCells implements SectorCells {
    private static final int FLAT_Y = 0;

    private final WorldFold fold;
    private final int spacing;
    private final int centerCellX;
    private final int centerCellZ;
    private final int offsetCap;

    SplitSectorCells(WorldFold fold, int spacing, ChunkPos center) {
        this.fold = fold;
        this.spacing = spacing;
        this.centerCellX = Math.floorDiv(center.x(), spacing);
        this.centerCellZ = Math.floorDiv(center.z(), spacing);
        this.offsetCap = fold.stepsToCoverTheWorld(spacing * SectionPos.SECTION_SIZE);
    }

    @Override
    public int getOffsetCap() {
        return this.offsetCap;
    }

    @Override
    public List<Read> reads(int x, int z) {
        if (Math.max(Math.abs(x), Math.abs(z)) > this.offsetCap) {
            return List.of();
        }

        int cellX = this.centerCellX + x;
        int cellZ = this.centerCellZ + z;
        BoundingBox cell = new BoundingBox(
                SectionPos.sectionToBlockCoord(cellX * this.spacing), FLAT_Y,
                SectionPos.sectionToBlockCoord(cellZ * this.spacing),
                SectionPos.sectionToBlockCoord((cellX + 1) * this.spacing) - 1, FLAT_Y,
                SectionPos.sectionToBlockCoord((cellZ + 1) * this.spacing) - 1);
        if (!this.fold.isCrossingBounds(cell)) {
            return List.of(Read.whole(cellX, cellZ));
        }

        List<Read> reads = new ArrayList<>();
        for (WorldFold.Folded<BoundingBox> piece : this.fold.split(cell)) {
            addReads(reads, piece.value());
        }

        return reads;
    }

    private void addReads(List<Read> reads, BoundingBox piece) {
        int minChunkX = SectionPos.blockToSectionCoord(piece.minX());
        int minChunkZ = SectionPos.blockToSectionCoord(piece.minZ());
        int maxChunkX = SectionPos.blockToSectionCoord(piece.maxX());
        int maxChunkZ = SectionPos.blockToSectionCoord(piece.maxZ());
        for (int cellX = Math.floorDiv(minChunkX, this.spacing); cellX <= Math.floorDiv(maxChunkX, this.spacing);
                cellX++) {
            for (int cellZ = Math.floorDiv(minChunkZ, this.spacing);
                    cellZ <= Math.floorDiv(maxChunkZ, this.spacing); cellZ++) {
                reads.add(new Read(cellX, cellZ, minChunkX, minChunkZ, maxChunkX, maxChunkZ));
            }
        }
    }
}

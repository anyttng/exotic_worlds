package com.exoticworlds.engine.gen.structure;

import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.gen.AddedStart;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

public final class SpreadRingScan {
    private final WorldFold fold;
    private final List<AddedStart> added;
    private final RandomSpreadStructurePlacement placement;
    private final long seed;
    private final SectorCells cells;

    private SpreadRingScan(WorldFold fold, List<AddedStart> added,
            RandomSpreadStructurePlacement placement, long seed, ChunkPos center) {
        this.fold = fold;
        this.added = added;
        this.placement = placement;
        this.seed = seed;
        this.cells = fold.getCuboidAtlas() != null
                ? new SplitSectorCells(fold, placement.spacing(), center)
                : new LatticeSectorCells(fold.getChunkLattice(), placement.spacing(), center);
    }

    public static SpreadRingScan of(WorldFold fold, AddedStructureStarts added,
            RandomSpreadStructurePlacement placement, long seed, ChunkPos center) {
        return new SpreadRingScan(fold, added.of(placement), placement, seed, center);
    }

    public <T> @Nullable T ring(int radius, Function<ChunkPos, @Nullable T> candidate,
            Function<AddedStart, @Nullable T> addedStart) {
        if (radius > this.cells.getOffsetCap()) {
            return null;
        }

        for (int x = -radius; x <= radius; x++) {
            boolean isXEdge = x == -radius || x == radius;
            for (int z = -radius; z <= radius; z++) {
                if (!isXEdge && z != -radius && z != radius) {
                    continue;
                }

                T found = cell(x, z, candidate, addedStart);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    // mc/1.21: Supplementaries' structure locator folds the cells of its own round rings through it.
    public <T> @Nullable T cell(int x, int z, Function<ChunkPos, @Nullable T> candidate,
            Function<AddedStart, @Nullable T> addedStart) {
        for (SectorCells.Read read : this.cells.reads(x, z)) {
            T found = read(read, candidate, addedStart);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private <T> @Nullable T read(SectorCells.Read read, Function<ChunkPos, @Nullable T> candidate,
            Function<AddedStart, @Nullable T> addedStart) {
        int spacingChunks = this.placement.spacing();
        ChunkPos picked = this.placement.getPotentialStructureChunk(this.seed, read.cellX() * spacingChunks,
                read.cellZ() * spacingChunks);
        if (read.isHolding(picked) && !this.fold.isOver(picked)) {
            T found = candidate.apply(picked);
            if (found != null) {
                return found;
            }
        }

        return addedInCell(read, addedStart);
    }

    private <T> @Nullable T addedInCell(SectorCells.Read read,
            Function<AddedStart, @Nullable T> addedStart) {
        int spacingChunks = this.placement.spacing();
        for (AddedStart start : this.added) {
            if (Math.floorDiv(start.chunk().x(), spacingChunks) != read.cellX()
                    || Math.floorDiv(start.chunk().z(), spacingChunks) != read.cellZ() || !read.isHolding(start.chunk())) {
                continue;
            }

            T found = addedStart.apply(start);
            if (found != null) {
                return found;
            }
        }

        return null;
    }
}

package com.exoticworlds.engine.gen.structure;

import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.gen.StructureStarts;
import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

public final class SpreadRingScan {
    private final WorldFold fold;
    private final List<StructureStarts.Added> added;
    private final RandomSpreadStructurePlacement placement;
    private final long seed;
    private final SectorCells cells;

    private SpreadRingScan(WorldFold fold, List<StructureStarts.Added> added,
            RandomSpreadStructurePlacement placement, long seed, ChunkPos center) {
        this.fold = fold;
        this.added = added;
        this.placement = placement;
        this.seed = seed;
        this.cells = fold.cuboidAtlas() != null
                ? new SplitSectorCells(fold, placement.spacing(), center)
                : new LatticeSectorCells(fold.chunkLattice(), placement.spacing(), center);
    }

    public static SpreadRingScan of(WorldFold fold, AddedStructureStarts added,
            RandomSpreadStructurePlacement placement, long seed, ChunkPos center) {
        return new SpreadRingScan(fold, added.of(placement), placement, seed, center);
    }

    public <T> @Nullable T ring(int radius, Function<ChunkPos, @Nullable T> candidate,
            Function<StructureStarts.Added, @Nullable T> addedStart) {
        if (radius > this.cells.offsetCap()) {
            return null;
        }

        for (int x = -radius; x <= radius; x++) {
            boolean xEdge = x == -radius || x == radius;
            for (int z = -radius; z <= radius; z++) {
                if (!xEdge && z != -radius && z != radius) {
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
            Function<StructureStarts.Added, @Nullable T> addedStart) {
        for (SectorCells.Read read : this.cells.reads(x, z)) {
            T found = read(read, candidate, addedStart);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private <T> @Nullable T read(SectorCells.Read read, Function<ChunkPos, @Nullable T> candidate,
            Function<StructureStarts.Added, @Nullable T> addedStart) {
        int spacing = this.placement.spacing();
        ChunkPos picked = this.placement.getPotentialStructureChunk(this.seed, read.cellX() * spacing,
                read.cellZ() * spacing);
        if (read.holds(picked) && !this.fold.isOver(picked)) {
            T found = candidate.apply(picked);
            if (found != null) {
                return found;
            }
        }

        return addedInCell(read, addedStart);
    }

    private <T> @Nullable T addedInCell(SectorCells.Read read,
            Function<StructureStarts.Added, @Nullable T> addedStart) {
        int spacing = this.placement.spacing();
        for (StructureStarts.Added start : this.added) {
            if (Math.floorDiv(start.chunk().x(), spacing) != read.cellX()
                    || Math.floorDiv(start.chunk().z(), spacing) != read.cellZ() || !read.holds(start.chunk())) {
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

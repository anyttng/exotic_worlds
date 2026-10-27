package com.exoticworlds.engine.gen;

import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.gen.StructureStarts;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

public final class SpreadRingScan {
    private final WorldFold fold;
    private final List<StructureStarts.Added> added;
    private final RandomSpreadStructurePlacement placement;
    private final long seed;
    private final int skew;
    private final SectorGridAxis xCells;
    private final SectorGridAxis zCells;

    private SpreadRingScan(WorldFold fold, List<StructureStarts.Added> added,
            RandomSpreadStructurePlacement placement, long seed, ChunkPos center) {
        TranslationLattice lattice = fold.chunkLattice();
        this.fold = fold;
        this.added = added;
        this.placement = placement;
        this.seed = seed;
        this.skew = lattice.skew();
        this.xCells = SectorGridAxis.of(lattice.x(), placement.spacing(), center.x);
        this.zCells = SectorGridAxis.of(lattice.z(), placement.spacing(), center.z);
    }

    public static SpreadRingScan of(WorldFold fold, AddedStructureStarts added,
            RandomSpreadStructurePlacement placement, long seed, ChunkPos center) {
        return new SpreadRingScan(fold, added.of(placement), placement, seed, center);
    }

    public <T> @Nullable T ring(int radius, Function<ChunkPos, @Nullable T> candidate,
            Function<StructureStarts.Added, @Nullable T> addedStart) {
        if (radius > Math.max(this.xCells.offsetCap(), this.zCells.offsetCap())) {
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
        if (Math.abs(x) > this.xCells.offsetCap() || Math.abs(z) > this.zCells.offsetCap()) {
            return null;
        }

        int probeX = this.xCells.probeChunkShifted(x, -this.zCells.lapsCrossed(z) * this.skew);
        int probeZ = this.zCells.probeChunk(z);
        ChunkPos picked = this.placement.getPotentialStructureChunk(this.seed, probeX, probeZ);
        if (!this.fold.isOver(picked)) {
            T found = candidate.apply(picked);
            if (found != null) {
                return found;
            }
        }

        return addedInCell(probeX, probeZ, addedStart);
    }

    private <T> @Nullable T addedInCell(int probeX, int probeZ,
            Function<StructureStarts.Added, @Nullable T> addedStart) {
        int spacing = this.placement.spacing();
        for (StructureStarts.Added start : this.added) {
            if (Math.floorDiv(start.chunk().x, spacing) != Math.floorDiv(probeX, spacing)
                    || Math.floorDiv(start.chunk().z, spacing) != Math.floorDiv(probeZ, spacing)) {
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

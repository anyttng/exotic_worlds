package com.exoticworlds.engine.gen.structure;

import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.gen.AddedStart;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

public final class SpreadRingScan {
    private final WorldFold fold;
    private final List<AddedStart> added;
    private final RandomSpreadStructurePlacement placement;
    private final long seed;
    private final int skewChunks;
    private final SectorGridAxis xCells;
    private final SectorGridAxis zCells;

    private SpreadRingScan(WorldFold fold, List<AddedStart> added,
            RandomSpreadStructurePlacement placement, long seed, ChunkPos center) {
        TranslationLattice lattice = fold.getChunkLattice();
        this.fold = fold;
        this.added = added;
        this.placement = placement;
        this.seed = seed;
        this.skewChunks = lattice.getSkew();
        this.xCells = SectorGridAxis.of(lattice.getX(), placement.spacing(), center.x());
        this.zCells = SectorGridAxis.of(lattice.getZ(), placement.spacing(), center.z());
    }

    public static SpreadRingScan of(WorldFold fold, AddedStructureStarts added,
            RandomSpreadStructurePlacement placement, long seed, ChunkPos center) {
        return new SpreadRingScan(fold, added.of(placement), placement, seed, center);
    }

    public <T> @Nullable T ring(int radius, Function<ChunkPos, @Nullable T> candidate,
            Function<AddedStart, @Nullable T> addedStart) {
        if (radius > Math.max(this.xCells.getOffsetCap(), this.zCells.getOffsetCap())) {
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
        if (Math.abs(x) > this.xCells.getOffsetCap() || Math.abs(z) > this.zCells.getOffsetCap()) {
            return null;
        }

        int probeX = this.xCells.probeChunkShifted(x, -this.zCells.lapsCrossed(z) * this.skewChunks);
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
            Function<AddedStart, @Nullable T> addedStart) {
        int spacingChunks = this.placement.spacing();
        for (AddedStart start : this.added) {
            if (Math.floorDiv(start.chunk().x(), spacingChunks) != Math.floorDiv(probeX, spacingChunks)
                    || Math.floorDiv(start.chunk().z(), spacingChunks) != Math.floorDiv(probeZ, spacingChunks)) {
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

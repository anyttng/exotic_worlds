package com.exoticworlds.compat.distanthorizons;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.lod.AlignedPeriod;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.ThreadScope;

import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public final class DhRowCopies {
    private static final ThreadScope<ChunkPos> PENDING_COPY = new ThreadScope<>();

    public static List<ChunkPos> copiesOf(DhLattice lattice, int chunkX, int chunkZ) {
        WorldFold fold = lattice.fold();
        AlignedPeriod period = DhFold.period(lattice, DhKeys.LEAF);
        int lapsX = period.laps(fold, Direction.Axis.X);
        int lapsZ = period.laps(fold, Direction.Axis.Z);
        if (lapsX == 1 && lapsZ == 1) {
            return List.of();
        }

        int widthX = fold.isLooped(Direction.Axis.X) ? fold.getWidthChunks(Direction.Axis.X) : 0;
        int widthZ = fold.isLooped(Direction.Axis.Z) ? fold.getWidthChunks(Direction.Axis.Z) : 0;
        int skewChunks = lattice.skewBlocks() / SectionPos.SECTION_SIZE;
        List<ChunkPos> copies = new ArrayList<>(lapsX * lapsZ - 1);
        for (int lapZ = 0; lapZ < lapsZ; lapZ++) {
            for (int lapX = 0; lapX < lapsX; lapX++) {
                if (lapX != 0 || lapZ != 0) {
                    copies.add(new ChunkPos(chunkX + lapX * widthX + lapZ * skewChunks, chunkZ + lapZ * widthZ));
                }
            }
        }

        return copies;
    }

    public static <T> T buildAt(ChunkPos copy, Supplier<T> build) {
        return PENDING_COPY.with(copy, build);
    }

    public static @Nullable ChunkPos getPendingCopy() {
        return PENDING_COPY.getCurrent();
    }

    private DhRowCopies() {
    }
}

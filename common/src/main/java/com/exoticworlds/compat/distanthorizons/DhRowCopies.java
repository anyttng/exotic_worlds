package com.exoticworlds.compat.distanthorizons;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;

import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public final class DhRowCopies {
    private static final ThreadLocal<ChunkPos> PENDING_COPY = new ThreadLocal<>();

    public static List<ChunkPos> copiesOf(DhLattice lattice, int chunkX, int chunkZ) {
        ToroidalShape shape = lattice.shape();
        DhFold.Period period = DhFold.period(lattice, DhKeys.LEAF);
        int lapsX = laps(shape, Direction.Axis.X, period.xBlocks());
        int lapsZ = laps(shape, Direction.Axis.Z, period.zBlocks());
        if (lapsX == 1 && lapsZ == 1) {
            return List.of();
        }

        int widthX = shape.loops(Direction.Axis.X) ? shape.widthChunks(Direction.Axis.X) : 0;
        int widthZ = shape.loops(Direction.Axis.Z) ? shape.widthChunks(Direction.Axis.Z) : 0;
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

    private static int laps(ToroidalShape shape, Direction.Axis axis, long periodBlocks) {
        return shape.loops(axis) ? (int) (periodBlocks / shape.widthBlocks(axis)) : 1;
    }

    public static <T> T buildAt(ChunkPos copy, Supplier<T> build) {
        PENDING_COPY.set(copy);
        try {
            return build.get();
        } finally {
            PENDING_COPY.remove();
        }
    }

    public static @Nullable ChunkPos pendingCopy() {
        return PENDING_COPY.get();
    }

    private DhRowCopies() {
    }
}

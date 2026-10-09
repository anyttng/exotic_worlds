package com.exoticworlds.compat.distanthorizons;

import java.util.function.Supplier;

import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.sql.dto.BeaconBeamDTO;
import com.seibel.distanthorizons.core.sql.dto.ChunkHashDTO;
import com.seibel.distanthorizons.core.sql.dto.FullDataSourceV2DTO;
import com.seibel.distanthorizons.core.sql.dto.IBaseDTO;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

public final class DhKeys {
    public static final byte LEAF = DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL;

    public static long foldSection(DhLattice lattice, long pos) {
        if (lattice == null) {
            return pos;
        }

        byte detail = DhSectionPos.getDetailLevel(pos);
        int rawX = DhSectionPos.getX(pos);
        int rawZ = DhSectionPos.getZ(pos);
        int x = DhFold.foldSectionX(lattice, detail, rawX, rawZ);
        int z = DhFold.foldSectionZ(lattice, detail, rawZ);
        if (x == rawX && z == rawZ) {
            DhProbes.keyKept(DhProbes.Key.SECTION);
            return pos;
        }

        long folded = DhSectionPos.encode(detail, x, z);
        DhProbes.sectionKeyFolded(pos, folded);
        return folded;
    }

    public static DhChunkPos foldChunk(DhLattice lattice, DhChunkPos pos) {
        return foldChunk(lattice, pos, pos.getX(), pos.getZ(), DhChunkPos::new);
    }

    public static ChunkPos foldChunk(DhLattice lattice, ChunkPos pos) {
        return foldChunk(lattice, pos, pos.x, pos.z, ChunkPos::new);
    }

    private static <P> P foldChunk(DhLattice lattice, P pos, int rawX, int rawZ, ChunkFactory<P> factory) {
        if (lattice == null) {
            return pos;
        }

        int x = DhFold.foldChunkX(lattice, LEAF, rawX, rawZ);
        int z = DhFold.foldChunkZ(lattice, LEAF, rawZ);
        if (x == rawX && z == rawZ) {
            DhProbes.keyKept(DhProbes.Key.CHUNK);
            return pos;
        }

        DhProbes.chunkKeyFolded(rawX, rawZ, x, z);
        return factory.at(x, z);
    }

    public static DhBlockPos foldBlock(DhLattice lattice, DhBlockPos pos) {
        if (lattice == null) {
            return pos;
        }

        BlockPos raw = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
        BlockPos block = lattice.shape().fold(raw);
        if (block == raw) {
            DhProbes.keyKept(DhProbes.Key.BEACON);
            return pos;
        }

        DhBlockPos folded = new DhBlockPos(block.getX(), block.getY(), block.getZ());
        DhProbes.beaconKeyFolded(pos, folded);
        return folded;
    }

    public static boolean containsACopy(DhLattice lattice, long sectionPos, long copyPos) {
        return DhFold.containsACopy(lattice, DhSectionPos.getDetailLevel(sectionPos), DhSectionPos.getX(sectionPos),
                DhSectionPos.getZ(sectionPos), DhSectionPos.getDetailLevel(copyPos), DhSectionPos.getX(copyPos),
                DhSectionPos.getZ(copyPos));
    }

    public static long nearestSection(DhLattice lattice, int refBlockX, int refBlockZ, long pos) {
        byte detail = DhSectionPos.getDetailLevel(pos);
        int rawX = DhSectionPos.getX(pos);
        int rawZ = DhSectionPos.getZ(pos);
        DhFold.Section nearest = DhFold.nearestSection(lattice, snapLevel(lattice), detail, refBlockX, refBlockZ,
                rawX, rawZ);
        if (nearest.x() == rawX && nearest.z() == rawZ) {
            return pos;
        }

        return DhSectionPos.encode(detail, nearest.x(), nearest.z());
    }

    public static boolean isNearestCopy(DhLattice lattice, int refBlockX, int refBlockZ, long pos) {
        byte snap = snapLevel(lattice);
        byte detail = DhSectionPos.getDetailLevel(pos);
        return DhFold.isNearestSection(lattice, Direction.Axis.X, snap, detail, refBlockX, DhSectionPos.getX(pos))
                && DhFold.isNearestSection(lattice, Direction.Axis.Z, snap, detail, refBlockZ, DhSectionPos.getZ(pos));
    }

    public static boolean isNearestBeam(DhLattice lattice, int refBlockX, int refBlockZ, DhBlockPos beam) {
        int width = DhFold.sectionWidthBlocks(LEAF);
        long leaf = DhSectionPos.encode(LEAF, Math.floorDiv(beam.getX(), width), Math.floorDiv(beam.getZ(), width));
        return isNearestCopy(lattice, refBlockX, refBlockZ, leaf);
    }

    public static boolean straddlesNearestCopy(DhLattice lattice, int refBlockX, int refBlockZ, long pos) {
        byte snap = snapLevel(lattice);
        byte detail = DhSectionPos.getDetailLevel(pos);
        int x = DhSectionPos.getX(pos);
        int z = DhSectionPos.getZ(pos);
        return DhFold.overlapsNearestWindow(lattice, Direction.Axis.X, snap, detail, refBlockX, x)
                && DhFold.overlapsNearestWindow(lattice, Direction.Axis.Z, snap, detail, refBlockZ, z)
                && !isNearestCopy(lattice, refBlockX, refBlockZ, pos);
    }

    public static byte snapLevel(DhLattice lattice) {
        return DhFold.snapDetailLevel(lattice, LEAF);
    }

    public static Object foldKey(DhLattice lattice, Object key) {
        if (key instanceof Long pos) {
            return foldSection(lattice, pos);
        } else if (key instanceof DhChunkPos pos) {
            return foldChunk(lattice, pos);
        } else if (key instanceof DhBlockPos pos) {
            return foldBlock(lattice, pos);
        }

        return key;
    }

    public static <T> T withFoldedKey(DhLattice lattice, IBaseDTO<?> dto, Supplier<T> statement) {
        Object raw = dto.getKey();
        reseat(dto, foldKey(lattice, raw));
        try {
            return statement.get();
        } finally {
            reseat(dto, raw);
        }
    }

    public static void reseat(IBaseDTO<?> dto, Object key) {
        if (dto instanceof FullDataSourceV2DTO section && key instanceof Long pos) {
            section.pos = pos;
        } else if (dto instanceof ChunkHashDTO chunk && key instanceof DhChunkPos pos) {
            chunk.pos = pos;
        } else if (dto instanceof BeaconBeamDTO beam && key instanceof DhBlockPos pos) {
            beam.blockPos = pos;
        }
    }

    private interface ChunkFactory<P> {
        P at(int x, int z);
    }

    private DhKeys() {
    }
}

package com.exoticworlds.compat.eminus;

import org.jspecify.annotations.Nullable;

import com.eminus.cell.CellKey;
import com.eminus.cell.DetailLevel;
import com.exoticworlds.compat.lod.AlignedPeriod;
import com.exoticworlds.core.WorldFold;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public final class CellFold {
    public static final CellFold NONE = new CellFold(null, 0);

    private static final double HALF = 0.5;
    private static final int UNALIGNED = 0;
    private static final int ANY_Y = 0;
    private static final String OPEN_AXIS = "open";
    private static final String AXIS_SEPARATOR = ";";
    private static final String FIELD_SEPARATOR = ":";

    private final @Nullable WorldFold fold;
    private final int skewBlocks;
    private final AlignedPeriod chunks;
    private final AlignedPeriod blocks;
    private final AlignedPeriod[] cells = new AlignedPeriod[DetailLevel.COUNT];
    private final String signature;

    private CellFold(@Nullable WorldFold fold, int skewBlocks) {
        this.fold = fold;
        this.skewBlocks = skewBlocks;
        if (fold == null) {
            chunks = new AlignedPeriod(0L, 0L, 0L);
            blocks = chunks;
            signature = OPEN_AXIS;
            return;
        }

        chunks = AlignedPeriod.of(fold, skewBlocks, SectionPos.SECTION_BITS);
        blocks = AlignedPeriod.of(fold, skewBlocks, UNALIGNED);
        for (int level = DetailLevel.MIN; level <= DetailLevel.MAX; level++) {
            cells[level] = AlignedPeriod.of(fold, skewBlocks,
                    Integer.numberOfTrailingZeros(DetailLevel.blocksPerCell(level)));
        }

        signature = axisSignature(fold, Direction.Axis.X) + AXIS_SEPARATOR + axisSignature(fold, Direction.Axis.Z)
                + AXIS_SEPARATOR + skewBlocks;
    }

    public static CellFold of(@Nullable WorldFold fold) {
        return fold == null || !fold.isWrapped()
                ? NONE
                : new CellFold(fold, fold.blockLattice().skew());
    }

    public boolean repeats() {
        return fold != null;
    }

    public String signature() {
        return signature;
    }

    public long foldKey(long key) {
        if (fold == null) {
            return key;
        }

        int level = CellKey.level(key);
        int side = DetailLevel.blocksPerCell(level);
        long blockZ = (long) CellKey.z(key) * side;
        long x = cells[level].foldX(fold, (long) CellKey.x(key) * side, blockZ);
        long z = cells[level].foldZ(fold, blockZ);
        return CellKey.pack(level, (int) Math.floorDiv(x, side), CellKey.y(key), (int) Math.floorDiv(z, side));
    }

    public int foldChunkX(int chunkX, int chunkZ) {
        if (fold == null) {
            return chunkX;
        }

        return (int) (chunks.foldX(fold, SectionPos.sectionToBlockCoord(chunkX),
                SectionPos.sectionToBlockCoord(chunkZ)) >> SectionPos.SECTION_BITS);
    }

    public int foldChunkZ(int chunkZ) {
        if (fold == null) {
            return chunkZ;
        }

        return (int) (chunks.foldZ(fold, SectionPos.sectionToBlockCoord(chunkZ)) >> SectionPos.SECTION_BITS);
    }

    public long foldBlock(long pos) {
        if (fold == null) {
            return pos;
        }

        int z = BlockPos.getZ(pos);
        long x = blocks.foldX(fold, BlockPos.getX(pos), z);
        return BlockPos.asLong((int) x, BlockPos.getY(pos), (int) blocks.foldZ(fold, z));
    }

    public long copyNear(long pos, double centerX, double centerZ) {
        if (fold == null) {
            return pos;
        }

        long x = BlockPos.getX(pos);
        long z = BlockPos.getZ(pos);
        double shear = 0.0;
        if (fold.loops(Direction.Axis.Z)) {
            int widthZ = width(Direction.Axis.Z);
            long laps = lapsOff(z + HALF - centerZ, widthZ);
            z -= laps * widthZ;
            x -= laps * skewBlocks;
            shear = (z + HALF - centerZ) / widthZ * skewBlocks;
        }

        if (fold.loops(Direction.Axis.X)) {
            int widthX = width(Direction.Axis.X);
            x -= lapsOff(x + HALF - centerX - shear, widthX) * widthX;
        }

        return BlockPos.asLong((int) x, BlockPos.getY(pos), (int) z);
    }

    public long copyNearChunk(int chunkX, int chunkZ, int centerChunkX, int centerChunkZ) {
        long copy = copyNear(BlockPos.asLong(SectionPos.sectionToBlockCoord(chunkX, SectionPos.SECTION_HALF_SIZE), ANY_Y,
                SectionPos.sectionToBlockCoord(chunkZ, SectionPos.SECTION_HALF_SIZE)),
                SectionPos.sectionToBlockCoord(centerChunkX, SectionPos.SECTION_HALF_SIZE),
                SectionPos.sectionToBlockCoord(centerChunkZ, SectionPos.SECTION_HALF_SIZE));
        return ChunkPos.pack(SectionPos.blockToSectionCoord(BlockPos.getX(copy)),
                SectionPos.blockToSectionCoord(BlockPos.getZ(copy)));
    }

    public long[] foldedChunks(int chunkX, int chunkZ) {
        if (fold == null) {
            return new long[] {ChunkPos.pack(chunkX, chunkZ)};
        }

        LongLinkedOpenHashSet folded = new LongLinkedOpenHashSet();
        int minX = SectionPos.sectionToBlockCoord(chunkX);
        int minZ = SectionPos.sectionToBlockCoord(chunkZ);
        for (int dz = 0; dz < SectionPos.SECTION_SIZE; dz++) {
            for (int dx = 0; dx < SectionPos.SECTION_SIZE; dx++) {
                long pos = foldBlock(BlockPos.asLong(minX + dx, ANY_Y, minZ + dz));
                folded.add(ChunkPos.pack(SectionPos.blockToSectionCoord(BlockPos.getX(pos)),
                        SectionPos.blockToSectionCoord(BlockPos.getZ(pos))));
            }
        }

        return folded.toLongArray();
    }

    private static long lapsOff(double offset, int width) {
        return (long) Math.floor(offset / width + HALF);
    }

    public int lapXBlocks() {
        return width(Direction.Axis.X);
    }

    public int lapZBlocks() {
        return width(Direction.Axis.Z);
    }

    public int skewBlocks() {
        return skewBlocks;
    }

    public int copies(int level) {
        return lapsX(level) * lapsZ(level);
    }

    public int copyOffsetX(int level, int copy) {
        int lapsX = lapsX(level);
        return copy % lapsX * width(Direction.Axis.X) + copy / lapsX * skewBlocks;
    }

    public int copyOffsetZ(int level, int copy) {
        return copy / lapsX(level) * width(Direction.Axis.Z);
    }

    public boolean meetsLap(double minX, double minZ, double maxX, double maxZ, double eyeX, double eyeZ,
            double margin) {
        if (fold == null) {
            return true;
        }

        double lowX = minX - eyeX - margin;
        double highX = maxX - eyeX + margin;
        double lowZ = minZ - eyeZ - margin;
        double highZ = maxZ - eyeZ + margin;
        double lowShift = 0.0;
        double highShift = 0.0;
        if (fold.loops(Direction.Axis.Z)) {
            double widthZ = width(Direction.Axis.Z);
            double half = widthZ * HALF;
            if (highZ <= -half || lowZ >= half) {
                return false;
            }

            double first = Math.clamp(lowZ / widthZ, -HALF, HALF) * skewBlocks;
            double last = Math.clamp(highZ / widthZ, -HALF, HALF) * skewBlocks;
            lowShift = Math.min(first, last);
            highShift = Math.max(first, last);
        }

        if (!fold.loops(Direction.Axis.X)) {
            return true;
        }

        double half = width(Direction.Axis.X) * HALF;
        return highX - lowShift > -half && lowX - highShift < half;
    }

    private int lapsX(int level) {
        return fold == null ? 1 : cells[level].laps(fold, Direction.Axis.X);
    }

    private int lapsZ(int level) {
        return fold == null ? 1 : cells[level].laps(fold, Direction.Axis.Z);
    }

    private int width(Direction.Axis axis) {
        return fold != null && fold.loops(axis) ? fold.widthBlocks(axis) : 0;
    }

    private static String axisSignature(WorldFold fold, Direction.Axis axis) {
        return fold.loops(axis)
                ? fold.minBlock(axis) + FIELD_SEPARATOR + fold.widthBlocks(axis)
                : OPEN_AXIS;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CellFold cells && cells.signature.equals(signature);
    }

    @Override
    public int hashCode() {
        return signature.hashCode();
    }

    @Override
    public String toString() {
        return signature;
    }
}

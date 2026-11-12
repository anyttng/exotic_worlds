package com.exoticworlds.compat.eminus;

import org.jspecify.annotations.Nullable;

import com.eminus.cell.CellKey;
import com.eminus.cell.DetailLevel;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.lod.AlignedPeriod;
import com.exoticworlds.core.ToroidalShapeView;
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

    private final @Nullable ToroidalShape shape;
    private final int skewBlocks;
    private final AlignedPeriod chunks;
    private final AlignedPeriod blocks;
    private final AlignedPeriod[] cells = new AlignedPeriod[DetailLevel.COUNT];
    private final String signature;

    private CellFold(@Nullable ToroidalShape shape, int skewBlocks) {
        this.shape = shape;
        this.skewBlocks = skewBlocks;
        if (shape == null) {
            chunks = new AlignedPeriod(0L, 0L, 0L);
            blocks = chunks;
            signature = OPEN_AXIS;
            return;
        }

        chunks = AlignedPeriod.of(shape, skewBlocks, SectionPos.SECTION_BITS);
        blocks = AlignedPeriod.of(shape, skewBlocks, UNALIGNED);
        for (int level = DetailLevel.MIN; level <= DetailLevel.MAX; level++) {
            cells[level] = AlignedPeriod.of(shape, skewBlocks,
                    Integer.numberOfTrailingZeros(DetailLevel.blocksPerCell(level)));
        }

        signature = axisSignature(shape, Direction.Axis.X) + AXIS_SEPARATOR + axisSignature(shape, Direction.Axis.Z)
                + AXIS_SEPARATOR + skewBlocks;
    }

    public static CellFold of(@Nullable WorldFold fold) {
        return fold == null || !fold.isWrapped()
                ? NONE
                : new CellFold(new ToroidalShapeView(fold), fold.blockLattice().skew());
    }

    public boolean repeats() {
        return shape != null;
    }

    public String signature() {
        return signature;
    }

    public long foldKey(long key) {
        if (shape == null) {
            return key;
        }

        int level = CellKey.level(key);
        int side = DetailLevel.blocksPerCell(level);
        long blockZ = (long) CellKey.z(key) * side;
        long x = foldX(shape, cells[level], (long) CellKey.x(key) * side, blockZ);
        long z = foldZ(shape, cells[level], blockZ);
        return CellKey.pack(level, (int) Math.floorDiv(x, side), CellKey.y(key), (int) Math.floorDiv(z, side));
    }

    public int foldChunkX(int chunkX, int chunkZ) {
        if (shape == null) {
            return chunkX;
        }

        return (int) (foldX(shape, chunks, SectionPos.sectionToBlockCoord(chunkX),
                SectionPos.sectionToBlockCoord(chunkZ)) >> SectionPos.SECTION_BITS);
    }

    public int foldChunkZ(int chunkZ) {
        if (shape == null) {
            return chunkZ;
        }

        return (int) (foldZ(shape, chunks, SectionPos.sectionToBlockCoord(chunkZ)) >> SectionPos.SECTION_BITS);
    }

    public long foldBlock(long pos) {
        if (shape == null) {
            return pos;
        }

        int z = BlockPos.getZ(pos);
        long x = foldX(shape, blocks, BlockPos.getX(pos), z);
        return BlockPos.asLong((int) x, BlockPos.getY(pos), (int) foldZ(shape, blocks, z));
    }

    public long copyNear(long pos, double centerX, double centerZ) {
        if (shape == null) {
            return pos;
        }

        long x = BlockPos.getX(pos);
        long z = BlockPos.getZ(pos);
        double shear = 0.0;
        if (shape.loops(Direction.Axis.Z)) {
            int widthZ = width(Direction.Axis.Z);
            long laps = lapsOff(z + HALF - centerZ, widthZ);
            z -= laps * widthZ;
            x -= laps * skewBlocks;
            shear = (z + HALF - centerZ) / widthZ * skewBlocks;
        }

        if (shape.loops(Direction.Axis.X)) {
            int widthX = width(Direction.Axis.X);
            x -= lapsOff(x + HALF - centerX - shear, widthX) * widthX;
        }

        return BlockPos.asLong((int) x, BlockPos.getY(pos), (int) z);
    }

    public long[] foldedChunks(int chunkX, int chunkZ) {
        if (shape == null) {
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
        if (shape == null) {
            return true;
        }

        double lowX = minX - eyeX - margin;
        double highX = maxX - eyeX + margin;
        double lowZ = minZ - eyeZ - margin;
        double highZ = maxZ - eyeZ + margin;
        double lowShift = 0.0;
        double highShift = 0.0;
        if (shape.loops(Direction.Axis.Z)) {
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

        if (!shape.loops(Direction.Axis.X)) {
            return true;
        }

        double half = width(Direction.Axis.X) * HALF;
        return highX - lowShift > -half && lowX - highShift < half;
    }

    private static long foldX(ToroidalShape shape, AlignedPeriod period, long blockX, long blockZ) {
        long x = blockX;
        if (period.xShiftBlocks() != 0L) {
            x -= Math.floorDiv(blockZ - shape.minBlock(Direction.Axis.Z), period.zBlocks()) * period.xShiftBlocks();
        }

        if (shape.loops(Direction.Axis.X)) {
            x -= Math.floorDiv(x - shape.minBlock(Direction.Axis.X), period.xBlocks()) * period.xBlocks();
        }

        return x;
    }

    private static long foldZ(ToroidalShape shape, AlignedPeriod period, long blockZ) {
        if (!shape.loops(Direction.Axis.Z)) {
            return blockZ;
        }

        return blockZ - Math.floorDiv(blockZ - shape.minBlock(Direction.Axis.Z), period.zBlocks()) * period.zBlocks();
    }

    private int lapsX(int level) {
        return shape != null && shape.loops(Direction.Axis.X)
                ? (int) (cells[level].xBlocks() / width(Direction.Axis.X))
                : 1;
    }

    private int lapsZ(int level) {
        return shape != null && shape.loops(Direction.Axis.Z)
                ? (int) (cells[level].zBlocks() / width(Direction.Axis.Z))
                : 1;
    }

    private int width(Direction.Axis axis) {
        return shape != null && shape.loops(axis) ? shape.widthBlocks(axis) : 0;
    }

    private static String axisSignature(ToroidalShape shape, Direction.Axis axis) {
        return shape.loops(axis)
                ? shape.minBlock(axis) + FIELD_SEPARATOR + shape.widthBlocks(axis)
                : OPEN_AXIS;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CellFold fold && fold.signature.equals(signature);
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

package com.exoticworlds.core;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DeckGroupFold implements WorldFold {
    private final FlatShape shape;

    private final DeckGroupLattice blocks;

    private final DeckGroupLattice chunks;

    private final @Nullable TranslationLattice blockLattice;

    private final @Nullable TranslationLattice chunkLattice;

    private final boolean wrapped;

    private final GenerationOptions generationOptions;

    public DeckGroupFold(FlatShape shape, GenerationOptions generationOptions) {
        this(shape, List.of(), generationOptions);
    }

    public DeckGroupFold(FlatShape shape, List<ForeignFrame> foreignFrames, GenerationOptions generationOptions) {
        this.shape = shape;
        boolean xLooped = shape.bounds().x() instanceof AxisBounds.Looped;
        boolean zLooped = shape.bounds().z() instanceof AxisBounds.Looped;
        this.wrapped = xLooped || zLooped;
        this.generationOptions = generationOptions;
        this.chunks = latticeOf(shape, foreignFrames, 1);
        this.blocks = latticeOf(shape, foreignFrames, CoordinateConstants.CHUNK_WIDTH);
        this.chunkLattice = this.chunks.translations;
        this.blockLattice = this.blocks.translations;
    }

    private static DeckGroupLattice latticeOf(FlatShape shape, List<ForeignFrame> foreignFrames, int unit) {
        return new DeckGroupLattice(shape, spans(foreignFrames, Direction.Axis.X, unit),
                spans(foreignFrames, Direction.Axis.Z, unit), unit);
    }

    private static List<ForeignSpan> spans(List<ForeignFrame> foreignFrames, Direction.Axis axis, int unit) {
        return foreignFrames.stream().map(frame -> frame.chunks(axis).scaled(unit)).toList();
    }

    public FlatShape shape() {
        return this.shape;
    }

    @Override
    public WorldLoopBounds bounds() {
        return this.shape.bounds();
    }

    @Override
    public boolean isWrapped() {
        return this.wrapped;
    }

    @Override
    public boolean decomposesPerAxis() {
        return this.shape.decomposesPerAxis();
    }

    @Override
    public boolean preservesLocalIndices() {
        return this.shape.preservesLocalIndices();
    }

    @Override
    public GenerationOptions generationOptions() {
        return this.generationOptions;
    }

    @Override
    public WrapDomain blockDomain(Direction.Axis axis) {
        return perAxisDomain(this.blocks, axis);
    }

    @Override
    public WrapDomain chunkDomain(Direction.Axis axis) {
        return perAxisDomain(this.chunks, axis);
    }

    @Override
    public boolean isOver(Vec3 pos) {
        return !foreign(this.blocks, pos.x, pos.z) && (this.blocks.x.isOver(pos.x) || this.blocks.z.isOver(pos.z));
    }

    @Override
    public boolean isOver(BlockPos pos) {
        return !foreign(this.blocks, pos.getX(), pos.getZ())
                && (this.blocks.x.isOver(pos.getX()) || this.blocks.z.isOver(pos.getZ()));
    }

    @Override
    public boolean isOver(ChunkPos pos) {
        return !foreign(this.chunks, pos.x, pos.z) && (this.chunks.x.isOver(pos.x) || this.chunks.z.isOver(pos.z));
    }

    @Override
    public int chunkOvershoot(ChunkPos pos) {
        return foreign(this.chunks, pos.x, pos.z)
                ? 0
                : Math.max(this.chunks.x.overshoot(pos.x), this.chunks.z.overshoot(pos.z));
    }

    private static boolean foreign(DeckGroupLattice lattice, double coordX, double coordZ) {
        return lattice.x.isForeign(coordX) || lattice.z.isForeign(coordZ);
    }

    private boolean foreignBox(double minX, double maxX, double minZ, double maxZ) {
        return (this.blocks.x.isForeign(minX) && this.blocks.x.isForeign(maxX))
                || (this.blocks.z.isForeign(minZ) && this.blocks.z.isForeign(maxZ));
    }

    private static SeamTransform foldMoveOfCell(DeckGroupLattice lattice, int cellX, int cellZ) {
        return foreign(lattice, cellX, cellZ) ? SeamTransform.IDENTITY : lattice.foldMoveOfCell(cellX, cellZ);
    }

    private static SeamTransform foldMoveOfCoord(DeckGroupLattice lattice, double coordX, double coordZ) {
        return foreign(lattice, coordX, coordZ) ? SeamTransform.IDENTITY : lattice.foldMoveOfCoord(coordX, coordZ);
    }

    private static long lapsOfCells(DeckGroupLattice lattice, int cellX, int cellZ) {
        return foreign(lattice, cellX, cellZ) ? DeckGroupLaps.NONE : lattice.laps.ofCells(cellX, cellZ);
    }

    private static long lapsOfCoords(DeckGroupLattice lattice, double coordX, double coordZ) {
        return foreign(lattice, coordX, coordZ) ? DeckGroupLaps.NONE : lattice.laps.ofCoords(coordX, coordZ);
    }

    private static SeamTransform nearestMoveOfCell(DeckGroupLattice lattice, int refX, int refZ, int targetX, int targetZ) {
        return foreign(lattice, refX, refZ) || foreign(lattice, targetX, targetZ)
                ? SeamTransform.IDENTITY
                : lattice.nearestMoveOfCell(refX, refZ, targetX, targetZ);
    }

    private static SeamTransform nearestMoveOfCoord(DeckGroupLattice lattice, double refX, double refZ, double targetX,
            double targetZ) {
        return foreign(lattice, refX, refZ) || foreign(lattice, targetX, targetZ)
                ? SeamTransform.IDENTITY
                : lattice.nearestMoveOfCoord(refX, refZ, targetX, targetZ);
    }

    @Override
    public int maxViewDistance() {
        return this.shape.bounds().maxViewDistance();
    }

    private WrapDomain perAxisDomain(DeckGroupLattice lattice, Direction.Axis axis) {
        if (!decomposesPerAxis()) {
            throw new IllegalStateException(this.shape.identification() + " does not decompose per axis");
        }

        return switch (axis) {
            case X -> lattice.x;
            case Z -> lattice.z;
            case Y -> throw new IllegalArgumentException("The fold contract carries no Y axis");
        };
    }

    @Override
    public TranslationLattice blockLattice() {
        return translationLattice(this.blockLattice);
    }

    @Override
    public TranslationLattice chunkLattice() {
        return translationLattice(this.chunkLattice);
    }

    private TranslationLattice translationLattice(@Nullable TranslationLattice lattice) {
        if (lattice == null) {
            throw new IllegalStateException(this.shape.identification() + " is not a translation lattice");
        }

        return lattice;
    }

    @Override
    public Vec3 fold(Vec3 pos) {
        DeckGroupLaps laps = this.blocks.laps;
        long folded = lapsOfCoords(this.blocks, pos.x, pos.z);
        return folded == DeckGroupLaps.NONE
                ? pos
                : new Vec3(laps.foldX(folded, pos.x), pos.y, laps.foldZ(folded, pos.z));
    }

    @Override
    public BlockPos fold(BlockPos pos) {
        DeckGroupLaps laps = this.blocks.laps;
        long folded = lapsOfCells(this.blocks, pos.getX(), pos.getZ());
        return folded == DeckGroupLaps.NONE
                ? pos
                : new BlockPos(laps.foldCellX(folded, pos.getX()), pos.getY(), laps.foldCellZ(folded, pos.getZ()));
    }

    @Override
    public ChunkPos fold(ChunkPos pos) {
        DeckGroupLaps laps = this.chunks.laps;
        long folded = lapsOfCells(this.chunks, pos.x, pos.z);
        return folded == DeckGroupLaps.NONE
                ? pos
                : new ChunkPos(laps.foldCellX(folded, pos.x), laps.foldCellZ(folded, pos.z));
    }

    @Override
    public SectionPos fold(SectionPos pos) {
        DeckGroupLaps laps = this.chunks.laps;
        long folded = lapsOfCells(this.chunks, pos.x(), pos.z());
        return folded == DeckGroupLaps.NONE
                ? pos
                : SectionPos.of(laps.foldCellX(folded, pos.x()), pos.y(), laps.foldCellZ(folded, pos.z()));
    }

    @Override
    public long foldBlockNode(long blockNode) {
        int x = BlockPos.getX(blockNode);
        int z = BlockPos.getZ(blockNode);
        DeckGroupLaps laps = this.blocks.laps;
        long folded = lapsOfCells(this.blocks, x, z);
        return folded == DeckGroupLaps.NONE
                ? blockNode
                : BlockPos.asLong(laps.foldCellX(folded, x), BlockPos.getY(blockNode), laps.foldCellZ(folded, z));
    }

    @Override
    public long foldChunkKey(long chunkKey) {
        int x = ChunkPos.getX(chunkKey);
        int z = ChunkPos.getZ(chunkKey);
        DeckGroupLaps laps = this.chunks.laps;
        long folded = lapsOfCells(this.chunks, x, z);
        return folded == DeckGroupLaps.NONE
                ? chunkKey
                : ChunkPos.asLong(laps.foldCellX(folded, x), laps.foldCellZ(folded, z));
    }

    @Override
    public long foldSectionNode(long sectionNode) {
        int x = SectionPos.x(sectionNode);
        int z = SectionPos.z(sectionNode);
        DeckGroupLaps laps = this.chunks.laps;
        long folded = lapsOfCells(this.chunks, x, z);
        return folded == DeckGroupLaps.NONE
                ? sectionNode
                : SectionPos.asLong(laps.foldCellX(folded, x), SectionPos.y(sectionNode), laps.foldCellZ(folded, z));
    }

    @Override
    public Folded<Vec3> foldOriented(Vec3 pos) {
        SeamTransform applied = foldMoveOfCoord(this.blocks, pos.x, pos.z);
        return new Folded<>(applied.apply(pos), applied.orientation());
    }

    @Override
    public Folded<BlockPos> foldOriented(BlockPos pos) {
        SeamTransform applied = foldMoveOfCell(this.blocks, pos.getX(), pos.getZ());
        return new Folded<>(applied.apply(pos), applied.orientation());
    }

    @Override
    public Folded<ChunkPos> foldOriented(ChunkPos pos) {
        SeamTransform applied = foldMoveOfCell(this.chunks, pos.x, pos.z);
        return new Folded<>(applied.apply(pos), applied.orientation());
    }

    @Override
    public Vec3 nearestCopy(Vec3 ref, Vec3 target) {
        return nearestMoveOfCoord(this.blocks, ref.x, ref.z, target.x, target.z).apply(target);
    }

    @Override
    public BlockPos nearestCopy(BlockPos ref, BlockPos target) {
        return nearestMoveOfCell(this.blocks, ref.getX(), ref.getZ(), target.getX(), target.getZ()).apply(target);
    }

    @Override
    public ChunkPos nearestCopy(ChunkPos ref, ChunkPos target) {
        return nearestMoveOfCell(this.chunks, ref.x, ref.z, target.x, target.z).apply(target);
    }

    @Override
    public long nearestCopyChunkKey(long refKey, long chunkKey) {
        int x = ChunkPos.getX(chunkKey);
        int z = ChunkPos.getZ(chunkKey);
        SeamTransform move = this.chunks.nearestMoveOfCell(ChunkPos.getX(refKey), ChunkPos.getZ(refKey), x, z);
        return move.isIdentity() ? chunkKey : ChunkPos.asLong(move.applyCellX(x), move.applyCellZ(z));
    }

    @Override
    public long nearestCopyBlockNode(long refNode, long blockNode) {
        int x = BlockPos.getX(blockNode);
        int z = BlockPos.getZ(blockNode);
        SeamTransform move = nearestMoveOfCell(this.blocks, BlockPos.getX(refNode), BlockPos.getZ(refNode), x, z);
        return move.isIdentity()
                ? blockNode
                : BlockPos.asLong(move.applyCellX(x), BlockPos.getY(blockNode), move.applyCellZ(z));
    }

    @Override
    public Folded<Vec3> nearestCopyOriented(Vec3 ref, Vec3 target) {
        SeamTransform move = nearestMoveOfCoord(this.blocks, ref.x, ref.z, target.x, target.z);
        return new Folded<>(move.apply(target), move.orientation());
    }

    @Override
    public Folded<BlockPos> nearestCopyOriented(BlockPos ref, BlockPos target) {
        SeamTransform move = nearestMoveOfCell(this.blocks, ref.getX(), ref.getZ(), target.getX(), target.getZ());
        return new Folded<>(move.apply(target), move.orientation());
    }

    @Override
    public DeckTransformation foldTransformation(Vec3 pos) {
        SeamTransform applied = foldMoveOfCoord(this.blocks,pos.x, pos.z);
        return applied.isIdentity() ? DeckTransformation.IDENTITY : new DeckTransformation(applied);
    }

    @Override
    public DeckTransformation nearestCopyTransformation(Vec3 ref, Vec3 target) {
        return carried(nearestMoveOfCoord(this.blocks,ref.x, ref.z, target.x, target.z));
    }

    @Override
    public DeckTransformation nearestCopyTransformation(BlockPos ref, BlockPos target) {
        return carried(nearestMoveOfCell(this.blocks,ref.getX(), ref.getZ(), target.getX(), target.getZ()));
    }

    private static DeckTransformation carried(SeamTransform move) {
        return move.isIdentity() ? DeckTransformation.IDENTITY : new DeckTransformation(move);
    }

    @Override
    public DeckTransformation deckTransformation(ChunkPos chunk, ChunkPos copy) {
        if (chunk.equals(copy)) {
            return DeckTransformation.IDENTITY;
        }

        DeckTransformation carried = new DeckTransformation(this.blocks.between(
                chunk.getMinBlockX(), chunk.getMinBlockZ(), copy.getMinBlockX(), copy.getMinBlockZ()));
        if (!carried.apply(chunk).equals(copy)) {
            throw new IllegalArgumentException(copy + " is not a copy of " + chunk + " in " + this.shape);
        }

        return carried;
    }

    @Override
    public BlockPos reseat(BlockPos pos, ChunkPos copy) {
        return deckTransformation(new ChunkPos(pos), copy).apply(pos);
    }

    @Override
    public Vec3 foldDelta(Vec3 from, Vec3 to) {
        return nearestCopy(from, to).subtract(from);
    }

    @Override
    public double sqrDistance(double xFrom, double yFrom, double zFrom, double xTo, double yTo, double zTo) {
        double dy = yTo - yFrom;
        double horizontal = foreign(this.blocks, xFrom, zFrom) || foreign(this.blocks, xTo, zTo)
                ? (xTo - xFrom) * (xTo - xFrom) + (zTo - zFrom) * (zTo - zFrom)
                : this.blocks.nearestCoordsSqrDistance(xFrom, zFrom, xTo, zTo);
        return horizontal + dy * dy;
    }

    @Override
    public int sqrChunkDistance(ChunkPos from, ChunkPos to) {
        return (int) this.chunks.nearestCellsSqrDistance(from.x, from.z, to.x, to.z);
    }

    @Override
    public double sqrDistanceToBox(AABB box, Vec3 point) {
        Vec3 center = box.getCenter();
        if (foreign(this.blocks, point.x, point.z) || foreign(this.blocks, center.x, center.z)) {
            return box.distanceToSqr(point);
        }

        return this.blocks.nearestBoxSqrGap(point.x, point.z, box) + SeamBoxes.sqrYGap(box, point);
    }

    @Override
    public boolean crossesBounds(AABB box) {
        return !foreignBox(box.minX, box.maxX, box.minZ, box.maxZ)
                && SeamBoxes.crosses(this.blocks.x, this.blocks.z, box);
    }

    @Override
    public boolean crossesBounds(BoundingBox region) {
        return !foreignBox(region.minX(), region.maxX(), region.minZ(), region.maxZ())
                && SeamBoxes.crosses(this.blocks.x, this.blocks.z, region);
    }

    @Override
    public List<Folded<AABB>> split(AABB box) {
        if (!crossesBounds(box)) {
            return List.of(Folded.of(box));
        }

        List<DeckGroupRegions.CoordPiece> reduced = this.blocks.regions.splitCoords(box);
        List<Folded<AABB>> pieces = new ArrayList<>(reduced.size());
        for (DeckGroupRegions.CoordPiece piece : reduced) {
            pieces.add(new Folded<>(piece.box(), piece.applied().orientation()));
        }

        return pieces;
    }

    @Override
    public List<Folded<BoundingBox>> split(BoundingBox region) {
        if (!crossesBounds(region)) {
            return List.of(Folded.of(region));
        }

        List<DeckGroupRegions.CellPiece> reduced = this.blocks.regions.splitCells(region);
        List<Folded<BoundingBox>> pieces = new ArrayList<>(reduced.size());
        for (DeckGroupRegions.CellPiece piece : reduced) {
            pieces.add(new Folded<>(piece.box(), piece.applied().orientation()));
        }

        return pieces;
    }

    @Override
    public boolean regionsOverlap(BoundingBox first, BoundingBox second) {
        if (first.minY() > second.maxY() || second.minY() > first.maxY()) {
            return false;
        }

        for (Folded<BoundingBox> firstPiece : split(first)) {
            for (Folded<BoundingBox> secondPiece : split(second)) {
                if (horizontallyIntersects(firstPiece.value(), secondPiece.value())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public List<DeckTransformation> copiesTouching(BoundingBox region, int reach) {
        if (reach < 0) {
            throw new IllegalArgumentException("A copy reach is never negative, got " + reach);
        }

        return this.blocks.regions.copiesTouching(region.minX(), region.maxX(), region.minZ(), region.maxZ(), reach);
    }

    @Override
    public boolean foldsOntoItself(BoundingBox region) {
        return this.blocks.regions.foldsOntoItself(region);
    }

    @Override
    public Folded<AABB> nearestCopyOriented(Vec3 ref, AABB box) {
        SeamTransform move = nearestMoveOfCoord(this.blocks, ref.x, ref.z,
                SeamBoxes.center(box.minX, box.maxX), SeamBoxes.center(box.minZ, box.maxZ));
        return move.isIdentity() ? Folded.of(box) : new Folded<>(move.apply(box), move.orientation());
    }

    private static boolean horizontallyIntersects(BoundingBox first, BoundingBox second) {
        return DeckGroupRegions.meets(first.minX(), first.maxX(), second.minX(), second.maxX())
                && DeckGroupRegions.meets(first.minZ(), first.maxZ(), second.minZ(), second.maxZ());
    }
}

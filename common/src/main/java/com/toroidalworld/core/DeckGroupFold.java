package com.toroidalworld.core;

import java.util.ArrayList;
import java.util.List;

import com.toroidalworld.api.v1.option.GenerationOptions;
import com.toroidalworld.core.WorldLoopBounds.AxisBounds;

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

    private final boolean wrapped;

    private final GenerationOptions generationOptions;

    public DeckGroupFold(FlatShape shape) {
        this(shape, GenerationOptions.DEFAULT);
    }

    public DeckGroupFold(FlatShape shape, GenerationOptions generationOptions) {
        this.shape = shape;
        boolean xLooped = shape.bounds().x() instanceof AxisBounds.Looped;
        boolean zLooped = shape.bounds().z() instanceof AxisBounds.Looped;
        this.wrapped = xLooped || zLooped;
        this.generationOptions = generationOptions;
        this.chunks = new DeckGroupLattice(shape, 1);
        this.blocks = new DeckGroupLattice(shape, CoordinateConstants.CHUNK_WIDTH);
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
        return this.blocks.x.isOver(pos.x) || this.blocks.z.isOver(pos.z);
    }

    @Override
    public boolean isOver(BlockPos pos) {
        return this.blocks.x.isOver(pos.getX()) || this.blocks.z.isOver(pos.getZ());
    }

    @Override
    public boolean isOver(ChunkPos pos) {
        return this.chunks.x.isOver(pos.x()) || this.chunks.z.isOver(pos.z());
    }

    @Override
    public int chunkOvershoot(ChunkPos pos) {
        return Math.max(this.chunks.x.overshoot(pos.x()), this.chunks.z.overshoot(pos.z()));
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
    public Vec3 fold(Vec3 pos) {
        SeamTransform applied = this.blocks.foldCoords(pos.x, pos.z);
        return applied.isIdentity() ? pos : new Vec3(applied.applyX(pos.x), pos.y, applied.applyZ(pos.z));
    }

    @Override
    public BlockPos fold(BlockPos pos) {
        SeamTransform applied = this.blocks.foldCells(pos.getX(), pos.getZ());
        return applied.isIdentity()
                ? pos
                : new BlockPos(applied.applyCellX(pos.getX()), pos.getY(), applied.applyCellZ(pos.getZ()));
    }

    @Override
    public ChunkPos fold(ChunkPos pos) {
        SeamTransform applied = this.chunks.foldCells(pos.x(), pos.z());
        return applied.isIdentity() ? pos : new ChunkPos(applied.applyCellX(pos.x()), applied.applyCellZ(pos.z()));
    }

    @Override
    public SectionPos fold(SectionPos pos) {
        SeamTransform applied = this.chunks.foldCells(pos.x(), pos.z());
        return applied.isIdentity()
                ? pos
                : SectionPos.of(applied.applyCellX(pos.x()), pos.y(), applied.applyCellZ(pos.z()));
    }

    @Override
    public long foldBlockNode(long blockNode) {
        int x = BlockPos.getX(blockNode);
        int z = BlockPos.getZ(blockNode);
        SeamTransform applied = this.blocks.foldCells(x, z);
        return applied.isIdentity()
                ? blockNode
                : BlockPos.asLong(applied.applyCellX(x), BlockPos.getY(blockNode), applied.applyCellZ(z));
    }

    @Override
    public long foldChunkKey(long chunkKey) {
        int x = ChunkPos.getX(chunkKey);
        int z = ChunkPos.getZ(chunkKey);
        SeamTransform applied = this.chunks.foldCells(x, z);
        return applied.isIdentity() ? chunkKey : ChunkPos.pack(applied.applyCellX(x), applied.applyCellZ(z));
    }

    @Override
    public long foldSectionNode(long sectionNode) {
        int x = SectionPos.x(sectionNode);
        int z = SectionPos.z(sectionNode);
        SeamTransform applied = this.chunks.foldCells(x, z);
        return applied.isIdentity()
                ? sectionNode
                : SectionPos.asLong(applied.applyCellX(x), SectionPos.y(sectionNode), applied.applyCellZ(z));
    }

    @Override
    public Folded<Vec3> foldOriented(Vec3 pos) {
        SeamTransform applied = this.blocks.foldCoords(pos.x, pos.z);
        Vec3 value = applied.isIdentity() ? pos : new Vec3(applied.applyX(pos.x), pos.y, applied.applyZ(pos.z));
        return new Folded<>(value, applied.orientation());
    }

    @Override
    public Folded<BlockPos> foldOriented(BlockPos pos) {
        SeamTransform applied = this.blocks.foldCells(pos.getX(), pos.getZ());
        BlockPos value = applied.isIdentity()
                ? pos
                : new BlockPos(applied.applyCellX(pos.getX()), pos.getY(), applied.applyCellZ(pos.getZ()));
        return new Folded<>(value, applied.orientation());
    }

    @Override
    public Folded<ChunkPos> foldOriented(ChunkPos pos) {
        SeamTransform applied = this.chunks.foldCells(pos.x(), pos.z());
        ChunkPos value = applied.isIdentity()
                ? pos
                : new ChunkPos(applied.applyCellX(pos.x()), applied.applyCellZ(pos.z()));
        return new Folded<>(value, applied.orientation());
    }

    @Override
    public Vec3 nearestCopy(Vec3 ref, Vec3 target) {
        SeamTransform move = this.blocks.nearestCoords(ref.x, ref.z, target.x, target.z);
        return move.isIdentity() ? target : new Vec3(move.applyX(target.x), target.y, move.applyZ(target.z));
    }

    @Override
    public BlockPos nearestCopy(BlockPos ref, BlockPos target) {
        SeamTransform move = this.blocks.nearestCells(ref.getX(), ref.getZ(), target.getX(), target.getZ());
        return move.isIdentity()
                ? target
                : new BlockPos(move.applyCellX(target.getX()), target.getY(), move.applyCellZ(target.getZ()));
    }

    @Override
    public ChunkPos nearestCopy(ChunkPos ref, ChunkPos target) {
        SeamTransform move = this.chunks.nearestCells(ref.x(), ref.z(), target.x(), target.z());
        return move.isIdentity() ? target : new ChunkPos(move.applyCellX(target.x()), move.applyCellZ(target.z()));
    }

    @Override
    public Folded<Vec3> nearestCopyOriented(Vec3 ref, Vec3 target) {
        SeamTransform move = this.blocks.nearestCoords(ref.x, ref.z, target.x, target.z);
        Vec3 value = move.isIdentity() ? target : new Vec3(move.applyX(target.x), target.y, move.applyZ(target.z));
        return new Folded<>(value, move.orientation());
    }

    @Override
    public Folded<BlockPos> nearestCopyOriented(BlockPos ref, BlockPos target) {
        SeamTransform move = this.blocks.nearestCells(ref.getX(), ref.getZ(), target.getX(), target.getZ());
        BlockPos value = move.isIdentity()
                ? target
                : new BlockPos(move.applyCellX(target.getX()), target.getY(), move.applyCellZ(target.getZ()));
        return new Folded<>(value, move.orientation());
    }

    @Override
    public DeckTransformation foldTransformation(Vec3 pos) {
        SeamTransform applied = this.blocks.foldCoords(pos.x, pos.z);
        return applied.isIdentity() ? DeckTransformation.IDENTITY : new DeckTransformation(applied);
    }

    @Override
    public DeckTransformation nearestCopyTransformation(Vec3 ref, Vec3 target) {
        return carried(this.blocks.nearestCoords(ref.x, ref.z, target.x, target.z));
    }

    @Override
    public DeckTransformation nearestCopyTransformation(BlockPos ref, BlockPos target) {
        return carried(this.blocks.nearestCells(ref.getX(), ref.getZ(), target.getX(), target.getZ()));
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
        return deckTransformation(ChunkPos.containing(pos), copy).apply(pos);
    }

    @Override
    public Vec3 foldDelta(Vec3 from, Vec3 to) {
        return nearestCopy(from, to).subtract(from);
    }

    @Override
    public double sqrDistance(Vec3 from, Vec3 to) {
        return sqrDistance(from.x, from.y, from.z, to.x, to.y, to.z);
    }

    @Override
    public double sqrDistance(double xFrom, double yFrom, double zFrom, double xTo, double yTo, double zTo) {
        SeamTransform move = this.blocks.nearestCoords(xFrom, zFrom, xTo, zTo);
        double dx = move.applyX(xTo) - xFrom;
        double dy = yTo - yFrom;
        double dz = move.applyZ(zTo) - zFrom;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public int sqrChunkDistance(ChunkPos from, ChunkPos to) {
        SeamTransform move = this.chunks.nearestCells(from.x(), from.z(), to.x(), to.z());
        int dx = move.applyCellX(to.x()) - from.x();
        int dz = move.applyCellZ(to.z()) - from.z();
        return dx * dx + dz * dz;
    }

    @Override
    public double sqrDistanceToBox(AABB box, Vec3 point) {
        double yGap = Math.max(Math.max(box.minY - point.y, point.y - box.maxY), 0.0);
        return this.blocks.nearestBoxGap(point.x, point.z, box.minX, box.maxX, box.minZ, box.maxZ) + yGap * yGap;
    }

    @Override
    public boolean crossesBounds(AABB box) {
        return !this.blocks.x.containsSpan(box.minX, box.maxX) || !this.blocks.z.containsSpan(box.minZ, box.maxZ);
    }

    @Override
    public boolean crossesBounds(BoundingBox region) {
        return this.blocks.x.isOver(region.minX()) || this.blocks.x.isOver(region.maxX())
                || this.blocks.z.isOver(region.minZ()) || this.blocks.z.isOver(region.maxZ());
    }

    @Override
    public List<Folded<AABB>> split(AABB box) {
        if (!crossesBounds(box)) {
            return List.of(Folded.of(box));
        }

        List<DeckGroupLattice.CoordPiece> reduced = this.blocks.splitCoords(box.minX, box.maxX, box.minZ, box.maxZ);
        List<Folded<AABB>> pieces = new ArrayList<>(reduced.size());
        for (DeckGroupLattice.CoordPiece piece : reduced) {
            pieces.add(new Folded<>(
                    new AABB(piece.minX(), box.minY, piece.minZ(), piece.maxX(), box.maxY, piece.maxZ()),
                    piece.applied().orientation()));
        }

        return pieces;
    }

    @Override
    public List<Folded<BoundingBox>> split(BoundingBox region) {
        if (!crossesBounds(region)) {
            return List.of(Folded.of(region));
        }

        List<DeckGroupLattice.CellPiece> reduced =
                this.blocks.splitCells(region.minX(), region.maxX(), region.minZ(), region.maxZ());
        List<Folded<BoundingBox>> pieces = new ArrayList<>(reduced.size());
        for (DeckGroupLattice.CellPiece piece : reduced) {
            pieces.add(new Folded<>(
                    new BoundingBox(piece.minX(), region.minY(), piece.minZ(),
                            piece.maxX(), region.maxY(), piece.maxZ()),
                    piece.applied().orientation()));
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

        return this.blocks.copiesTouching(region.minX(), region.maxX(), region.minZ(), region.maxZ(), reach);
    }

    @Override
    public boolean foldsOntoItself(BoundingBox region) {
        return this.blocks.foldsOntoItself(region.minX(), region.maxX(), region.minZ(), region.maxZ());
    }

    @Override
    public Folded<AABB> foldBox(Vec3 ref, AABB box) {
        double centerX = (box.minX + box.maxX) / 2.0;
        double centerZ = (box.minZ + box.maxZ) / 2.0;
        SeamTransform move = this.blocks.nearestCoords(ref.x, ref.z, centerX, centerZ);
        if (move.isIdentity()) {
            return Folded.of(box);
        }

        double firstX = move.applyX(box.minX);
        double secondX = move.applyX(box.maxX);
        double firstZ = move.applyZ(box.minZ);
        double secondZ = move.applyZ(box.maxZ);
        return new Folded<>(new AABB(
                Math.min(firstX, secondX), box.minY, Math.min(firstZ, secondZ),
                Math.max(firstX, secondX), box.maxY, Math.max(firstZ, secondZ)),
                move.orientation());
    }

    private static boolean horizontallyIntersects(BoundingBox first, BoundingBox second) {
        return DeckGroupLattice.meets(first.minX(), first.maxX(), second.minX(), second.maxX())
                && DeckGroupLattice.meets(first.minZ(), first.maxZ(), second.minZ(), second.maxZ());
    }
}

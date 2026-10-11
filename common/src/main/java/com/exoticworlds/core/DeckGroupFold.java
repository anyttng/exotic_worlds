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
        this.chunkLattice = this.chunks.translations;
        this.blockLattice = this.blocks.translations;
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
        return this.blocks.foldCoords(pos.x, pos.z).apply(pos);
    }

    @Override
    public BlockPos fold(BlockPos pos) {
        return this.blocks.foldCells(pos.getX(), pos.getZ()).apply(pos);
    }

    @Override
    public ChunkPos fold(ChunkPos pos) {
        return this.chunks.foldCells(pos.x(), pos.z()).apply(pos);
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
        return new Folded<>(applied.apply(pos), applied.orientation());
    }

    @Override
    public Folded<BlockPos> foldOriented(BlockPos pos) {
        SeamTransform applied = this.blocks.foldCells(pos.getX(), pos.getZ());
        return new Folded<>(applied.apply(pos), applied.orientation());
    }

    @Override
    public Folded<ChunkPos> foldOriented(ChunkPos pos) {
        SeamTransform applied = this.chunks.foldCells(pos.x(), pos.z());
        return new Folded<>(applied.apply(pos), applied.orientation());
    }

    @Override
    public Vec3 nearestCopy(Vec3 ref, Vec3 target) {
        return this.blocks.nearestCoords(ref.x, ref.z, target.x, target.z).apply(target);
    }

    @Override
    public BlockPos nearestCopy(BlockPos ref, BlockPos target) {
        return this.blocks.nearestCells(ref.getX(), ref.getZ(), target.getX(), target.getZ()).apply(target);
    }

    @Override
    public ChunkPos nearestCopy(ChunkPos ref, ChunkPos target) {
        return this.chunks.nearestCells(ref.x(), ref.z(), target.x(), target.z()).apply(target);
    }

    @Override
    public Folded<Vec3> nearestCopyOriented(Vec3 ref, Vec3 target) {
        SeamTransform move = this.blocks.nearestCoords(ref.x, ref.z, target.x, target.z);
        return new Folded<>(move.apply(target), move.orientation());
    }

    @Override
    public Folded<BlockPos> nearestCopyOriented(BlockPos ref, BlockPos target) {
        SeamTransform move = this.blocks.nearestCells(ref.getX(), ref.getZ(), target.getX(), target.getZ());
        return new Folded<>(move.apply(target), move.orientation());
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
        return this.blocks.nearestBoxGap(point.x, point.z, box) + SeamBoxes.sqrYGap(box, point);
    }

    @Override
    public boolean crossesBounds(AABB box) {
        return SeamBoxes.crosses(this.blocks.x, this.blocks.z, box);
    }

    @Override
    public boolean crossesBounds(BoundingBox region) {
        return SeamBoxes.crosses(this.blocks.x, this.blocks.z, region);
    }

    @Override
    public List<Folded<AABB>> split(AABB box) {
        if (!crossesBounds(box)) {
            return List.of(Folded.of(box));
        }

        List<DeckGroupLattice.CoordPiece> reduced = this.blocks.splitCoords(box);
        List<Folded<AABB>> pieces = new ArrayList<>(reduced.size());
        for (DeckGroupLattice.CoordPiece piece : reduced) {
            pieces.add(new Folded<>(piece.box(), piece.applied().orientation()));
        }

        return pieces;
    }

    @Override
    public List<Folded<BoundingBox>> split(BoundingBox region) {
        if (!crossesBounds(region)) {
            return List.of(Folded.of(region));
        }

        List<DeckGroupLattice.CellPiece> reduced = this.blocks.splitCells(region);
        List<Folded<BoundingBox>> pieces = new ArrayList<>(reduced.size());
        for (DeckGroupLattice.CellPiece piece : reduced) {
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

        return this.blocks.copiesTouching(region.minX(), region.maxX(), region.minZ(), region.maxZ(), reach);
    }

    @Override
    public boolean foldsOntoItself(BoundingBox region) {
        return this.blocks.foldsOntoItself(region);
    }

    @Override
    public Folded<AABB> foldBox(Vec3 ref, AABB box) {
        SeamTransform move = this.blocks.nearestCoords(ref.x, ref.z,
                SeamBoxes.centre(box.minX, box.maxX), SeamBoxes.centre(box.minZ, box.maxZ));
        return move.isIdentity() ? Folded.of(box) : new Folded<>(move.apply(box), move.orientation());
    }

    private static boolean horizontallyIntersects(BoundingBox first, BoundingBox second) {
        return DeckGroupLattice.meets(first.minX(), first.maxX(), second.minX(), second.maxX())
                && DeckGroupLattice.meets(first.minZ(), first.maxZ(), second.minZ(), second.maxZ());
    }
}

package com.exoticworlds.core;

import java.util.List;

import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface WorldFold {

    record Folded<T>(T value, FoldOrientation orientation) {
        public static <T> Folded<T> of(T value) {
            return new Folded<>(value, FoldOrientation.IDENTITY);
        }

        public boolean isIdentity() {
            return this.orientation.isIdentity();
        }
    }

    WorldLoopBounds bounds();

    boolean isWrapped();

    boolean decomposesPerAxis();

    boolean preservesLocalIndices();

    OptionValues options();

    WrapDomain blockDomain(Direction.Axis axis);

    WrapDomain chunkDomain(Direction.Axis axis);

    default boolean loops(Direction.Axis axis) {
        return axis != Direction.Axis.Y && bounds().axis(axis) instanceof AxisBounds.Looped;
    }

    default int minChunk(Direction.Axis axis) {
        return looped(axis).minChunk();
    }

    default int maxChunk(Direction.Axis axis) {
        return looped(axis).maxChunk();
    }

    default int widthChunks(Direction.Axis axis) {
        return looped(axis).chunkWidth();
    }

    default int minBlock(Direction.Axis axis) {
        return looped(axis).minBlock();
    }

    default int maxBlock(Direction.Axis axis) {
        return looped(axis).maxBlock();
    }

    default int widthBlocks(Direction.Axis axis) {
        return widthChunks(axis) * CoordinateConstants.CHUNK_WIDTH;
    }

    default double foldCoord(Direction.Axis axis, double coord) {
        return axis == Direction.Axis.Y ? coord : blockDomain(axis).wrap(coord);
    }

    default int foldBlock(Direction.Axis axis, int coord) {
        return axis == Direction.Axis.Y ? coord : blockDomain(axis).wrap(coord);
    }

    default int foldChunk(Direction.Axis axis, int chunk) {
        return axis == Direction.Axis.Y ? chunk : chunkDomain(axis).wrap(chunk);
    }

    default double nearestCoord(Direction.Axis axis, double ref, double coord) {
        return axis == Direction.Axis.Y ? coord : blockDomain(axis).unwrapAround(ref, coord);
    }

    TranslationLattice blockLattice();

    TranslationLattice chunkLattice();

    boolean isOver(Vec3 pos);

    boolean isOver(BlockPos pos);

    boolean isOver(ChunkPos pos);

    int chunkOvershoot(ChunkPos pos);

    int maxViewDistance();

    default int limitViewDistance(int viewDistance) {
        return Math.min(viewDistance, maxViewDistance());
    }

    Vec3 fold(Vec3 pos);

    BlockPos fold(BlockPos pos);

    ChunkPos fold(ChunkPos pos);

    SectionPos fold(SectionPos pos);

    long foldBlockNode(long blockNode);

    long foldChunkKey(long chunkKey);

    long foldSectionNode(long sectionNode);

    Folded<Vec3> foldOriented(Vec3 pos);

    Folded<BlockPos> foldOriented(BlockPos pos);

    Folded<ChunkPos> foldOriented(ChunkPos pos);

    Vec3 nearestCopy(Vec3 ref, Vec3 target);

    BlockPos nearestCopy(BlockPos ref, BlockPos target);

    ChunkPos nearestCopy(ChunkPos ref, ChunkPos target);

    long nearestCopyChunkKey(long refKey, long chunkKey);

    Folded<Vec3> nearestCopyOriented(Vec3 ref, Vec3 target);

    Folded<BlockPos> nearestCopyOriented(BlockPos ref, BlockPos target);

    Folded<AABB> nearestCopyOriented(Vec3 ref, AABB box);

    DeckTransformation foldTransformation(Vec3 pos);

    DeckTransformation deckTransformation(ChunkPos chunk, ChunkPos copy);

    DeckTransformation nearestCopyTransformation(Vec3 ref, Vec3 target);

    DeckTransformation nearestCopyTransformation(BlockPos ref, BlockPos target);

    BlockPos reseat(BlockPos pos, ChunkPos copy);

    default Vec3 foldDelta(Vec3 from, Vec3 to) {
        return nearestCopy(from, to).subtract(from);
    }

    double sqrDistance(double xFrom, double yFrom, double zFrom, double xTo, double yTo, double zTo);

    int sqrChunkDistance(ChunkPos from, ChunkPos to);

    boolean crossesBounds(AABB box);

    boolean crossesBounds(BoundingBox region);

    List<Folded<AABB>> split(AABB box);

    List<Folded<BoundingBox>> split(BoundingBox region);

    boolean regionsOverlap(BoundingBox first, BoundingBox second);

    default boolean boxesOverlap(AABB first, AABB second) {
        if (first.minY >= second.maxY || second.minY >= first.maxY) {
            return false;
        }

        return overlaps(split(first), second);
    }

    default boolean overlaps(List<Folded<AABB>> firstPieces, AABB second) {
        if (!crossesBounds(second)) {
            return anyPieceIntersects(firstPieces, second);
        }

        List<Folded<AABB>> secondPieces = split(second);
        for (int index = 0; index < secondPieces.size(); index++) {
            if (anyPieceIntersects(firstPieces, secondPieces.get(index).value())) {
                return true;
            }
        }

        return false;
    }

    private static boolean anyPieceIntersects(List<Folded<AABB>> pieces, AABB box) {
        for (int index = 0; index < pieces.size(); index++) {
            if (pieces.get(index).value().intersects(box)) {
                return true;
            }
        }

        return false;
    }

    List<DeckTransformation> copiesTouching(BoundingBox region, int reach);

    default List<DeckTransformation> copiesTouching(BoundingBox region) {
        return copiesTouching(region, Integer.MAX_VALUE);
    }

    boolean foldsOntoItself(BoundingBox region);

    private AxisBounds.Looped looped(Direction.Axis axis) {
        if (loops(axis) && bounds().axis(axis) instanceof AxisBounds.Looped looped) {
            return looped;
        }

        throw new IllegalArgumentException("Axis " + axis + " does not loop, check loops(axis) first");
    }
}

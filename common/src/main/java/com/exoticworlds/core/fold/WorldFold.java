package com.exoticworlds.core.fold;

import java.util.List;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.world.WorldLoopBounds;

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

    WorldLoopBounds getBounds();

    boolean isWrapped();

    boolean isDecomposedPerAxis();

    boolean isPreservingLocalIndices();

    ShapeOptions getOptions();

    WrapDomain getBlockDomain(Direction.Axis axis);

    WrapDomain getChunkDomain(Direction.Axis axis);

    default boolean isLooped(Direction.Axis axis) {
        return axis != Direction.Axis.Y && getBounds().getAxis(axis) instanceof AxisBounds.Looped;
    }

    default int getMinChunk(Direction.Axis axis) {
        return looped(axis).minChunk();
    }

    default int getWidthChunks(Direction.Axis axis) {
        return looped(axis).getWidthChunks();
    }

    default int getMinBlock(Direction.Axis axis) {
        return looped(axis).getMinBlock();
    }

    default int getMaxBlock(Direction.Axis axis) {
        return looped(axis).getMaxBlock();
    }

    default int getWidthBlocks(Direction.Axis axis) {
        return getWidthChunks(axis) * CoordinateConstants.CHUNK_WIDTH_BLOCKS;
    }

    TranslationLattice getBlockLattice();

    TranslationLattice getChunkLattice();

    boolean isOver(Vec3 pos);

    boolean isOver(BlockPos pos);

    boolean isOver(ChunkPos pos);

    int chunkOvershoot(ChunkPos pos);

    int getMaxViewDistanceChunks();

    default int limitViewDistance(int viewDistanceChunks) {
        return Math.min(viewDistanceChunks, getMaxViewDistanceChunks());
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

    double sqrDistanceBlocks(double xFrom, double yFrom, double zFrom, double xTo, double yTo, double zTo);

    int sqrDistanceChunks(ChunkPos from, ChunkPos to);

    boolean isCrossingBounds(AABB box);

    boolean isCrossingBounds(BoundingBox region);

    List<Folded<AABB>> split(AABB box);

    List<Folded<BoundingBox>> split(BoundingBox region);

    boolean areRegionsOverlapping(BoundingBox first, BoundingBox second);

    default boolean areBoxesOverlapping(AABB first, AABB second) {
        if (first.minY >= second.maxY || second.minY >= first.maxY) {
            return false;
        }

        return isOverlapping(split(first), second);
    }

    default boolean isOverlapping(List<Folded<AABB>> firstPieces, AABB second) {
        if (!isCrossingBounds(second)) {
            return isAnyPieceIntersecting(firstPieces, second);
        }

        List<Folded<AABB>> secondPieces = split(second);
        for (int index = 0; index < secondPieces.size(); index++) {
            if (isAnyPieceIntersecting(firstPieces, secondPieces.get(index).value())) {
                return true;
            }
        }

        return false;
    }

    private static boolean isAnyPieceIntersecting(List<Folded<AABB>> pieces, AABB box) {
        for (int index = 0; index < pieces.size(); index++) {
            if (pieces.get(index).value().intersects(box)) {
                return true;
            }
        }

        return false;
    }

    List<DeckTransformation> copiesTouching(BoundingBox region, int reachLaps);

    default List<DeckTransformation> copiesTouching(BoundingBox region) {
        return copiesTouching(region, Integer.MAX_VALUE);
    }

    boolean isFoldedOntoItself(BoundingBox region);

    private AxisBounds.Looped looped(Direction.Axis axis) {
        if (isLooped(axis) && getBounds().getAxis(axis) instanceof AxisBounds.Looped looped) {
            return looped;
        }

        throw new IllegalArgumentException("Axis " + axis + " does not loop, check isLooped(axis) first");
    }
}

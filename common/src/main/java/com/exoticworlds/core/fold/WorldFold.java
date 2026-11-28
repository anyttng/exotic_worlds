package com.exoticworlds.core.fold;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
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

    @Nullable CuboidAtlas getCuboidAtlas();

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

    default long foldChunkKeyFrom(long refKey, long chunkKey) {
        return foldChunkKey(chunkKey);
    }

    default ChunkPos foldFrom(ChunkPos ref, ChunkPos pos) {
        return fold(pos);
    }

    default long foldSectionNodeFrom(long refNode, long sectionNode) {
        return foldSectionNode(sectionNode);
    }

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

    default @Nullable DeckTransformation foldTransformationAlong(Vec3 from, Vec3 to) {
        return foldTransformation(to);
    }

    DeckTransformation deckTransformation(ChunkPos chunk, ChunkPos copy);

    default DeckTransformation deckTransformationFrom(ChunkPos ref, ChunkPos chunk) {
        return deckTransformation(chunk, fold(chunk));
    }

    DeckTransformation nearestCopyTransformation(Vec3 ref, Vec3 target);

    DeckTransformation nearestCopyTransformation(BlockPos ref, BlockPos target);

    default DeckTransformation nearestCopyTransformation(ChunkPos ref, ChunkPos target) {
        return deckTransformation(target, nearestCopy(ref, target));
    }

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

    int stepsToCoverTheWorld(int step);

    default List<ChunkPos> copiesInReach(ChunkPos ref, ChunkPos target, int reachChunks) {
        ChunkPos nearest = nearestCopy(ref, target);
        TranslationLattice lattice = getChunkLattice();
        int[] xLaps = candidateLaps(lattice.getX(), nearest.x() - ref.x(), reachChunks);
        int[] zLaps = candidateLaps(lattice.getZ(), nearest.z() - ref.z(), reachChunks);
        List<ChunkPos> copies = new ArrayList<>(xLaps.length * zLaps.length);
        for (int xLap : xLaps) {
            for (int zLap : zLaps) {
                copies.add(new ChunkPos(
                        nearest.x() + xLap * lattice.getX().domainLength + zLap * lattice.getSkew(),
                        nearest.z() + zLap * lattice.getZ().domainLength));
            }
        }

        return copies;
    }

    private static int[] candidateLaps(WrapDomain domain, int delta, int reachChunks) {
        if (Math.abs(delta) <= reachChunks || !domain.isLooped()) {
            return new int[] {0};
        }

        return new int[] {0, delta > 0 ? -1 : 1};
    }

    default boolean canFitInHalfBlocks(Direction.Axis axis, double spanBlocks) {
        return getBlockLattice().getAxis(axis).canFitInHalf(spanBlocks);
    }

    default boolean canFitInHalfChunks(Direction.Axis axis, double spanChunks) {
        return getChunkLattice().getAxis(axis).canFitInHalf(spanChunks);
    }

    private AxisBounds.Looped looped(Direction.Axis axis) {
        if (isLooped(axis) && getBounds().getAxis(axis) instanceof AxisBounds.Looped looped) {
            return looped;
        }

        throw new IllegalArgumentException("Axis " + axis + " does not loop, check isLooped(axis) first");
    }
}

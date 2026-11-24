package com.exoticworlds.bridge.v1.geometry;

import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.api.v1.SeamShift;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.FoldOrientation;
import com.exoticworlds.core.fold.RectangleCopies;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class ToroidalShapeView implements ToroidalShape {
    private final WorldFold fold;

    ToroidalShapeView(WorldFold fold) {
        this.fold = fold;
    }

    @Override
    public boolean loops(Direction.Axis axis) {
        return this.fold.isLooped(axis);
    }

    @Override
    public int minChunk(Direction.Axis axis) {
        return this.fold.getMinChunk(axis);
    }

    @Override
    public int maxChunk(Direction.Axis axis) {
        return this.fold.getMaxChunk(axis);
    }

    @Override
    public int widthChunks(Direction.Axis axis) {
        return this.fold.getWidthChunks(axis);
    }

    @Override
    public int minBlock(Direction.Axis axis) {
        return this.fold.getMinBlock(axis);
    }

    @Override
    public int maxBlock(Direction.Axis axis) {
        return this.fold.getMaxBlock(axis);
    }

    @Override
    public int widthBlocks(Direction.Axis axis) {
        return this.fold.getWidthBlocks(axis);
    }

    @Override
    public double foldCoord(Direction.Axis axis, double coord) {
        return this.fold.foldCoord(axis, coord);
    }

    @Override
    public int foldBlock(Direction.Axis axis, int coord) {
        return this.fold.foldBlock(axis, coord);
    }

    @Override
    public int foldChunk(Direction.Axis axis, int chunk) {
        return this.fold.foldChunk(axis, chunk);
    }

    @Override
    public double nearestCoord(Direction.Axis axis, double ref, double coord) {
        return this.fold.nearestCoord(axis, ref, coord);
    }

    @Override
    public BlockPos fold(BlockPos pos) {
        return this.fold.fold(pos);
    }

    @Override
    public Vec3 fold(Vec3 pos) {
        return this.fold.fold(pos);
    }

    @Override
    public ChunkPos fold(ChunkPos pos) {
        return this.fold.fold(pos);
    }

    @Override
    public Vec3 nearestCopy(Vec3 ref, Vec3 target) {
        return this.fold.nearestCopy(ref, target);
    }

    @Override
    public BlockPos nearestCopy(BlockPos ref, BlockPos target) {
        return this.fold.nearestCopy(ref, target);
    }

    @Override
    public Vec3 shortestDelta(Vec3 from, Vec3 to) {
        return this.fold.foldDelta(from, to);
    }

    @Override
    public double sqrDistance(Vec3 from, Vec3 to) {
        return this.fold.sqrDistanceBlocks(from.x, from.y, from.z, to.x, to.y, to.z);
    }

    @Override
    public boolean boxesOverlap(AABB first, AABB second) {
        return this.fold.areBoxesOverlapping(first, second);
    }

    @Override
    public List<Oriented<AABB>> split(AABB box) {
        List<WorldFold.Folded<AABB>> pieces = this.fold.split(box);
        List<Oriented<AABB>> oriented = new ArrayList<>(pieces.size());
        for (WorldFold.Folded<AABB> piece : pieces) {
            oriented.add(oriented(piece));
        }

        return oriented;
    }

    @Override
    public List<SeamShift> copiesMeeting(AABB box) {
        List<DeckTransformation> copies = RectangleCopies.meeting(this.fold, box);
        List<SeamShift> shifts = new ArrayList<>(copies.size());
        for (DeckTransformation copy : copies) {
            shifts.add(new SeamShiftView(copy));
        }

        return shifts;
    }

    @Override
    public List<Oriented<Vec3>> copiesInside(AABB box, Vec3 pos) {
        return RectangleCopies.inside(this.fold, box, pos, ToroidalShapeView::oriented);
    }

    @Override
    public List<Oriented<BlockPos>> copiesInside(AABB box, BlockPos pos) {
        return RectangleCopies.inside(this.fold, box, pos, ToroidalShapeView::oriented);
    }

    @Override
    public boolean decomposesPerAxis() {
        return this.fold.isDecomposedPerAxis();
    }

    @Override
    public boolean preservesLocalIndices() {
        return this.fold.isPreservingLocalIndices();
    }

    @Override
    public Oriented<BlockPos> foldOriented(BlockPos pos) {
        return oriented(this.fold.foldOriented(pos));
    }

    @Override
    public Oriented<Vec3> foldOriented(Vec3 pos) {
        return oriented(this.fold.foldOriented(pos));
    }

    @Override
    public Oriented<ChunkPos> foldOriented(ChunkPos pos) {
        return oriented(this.fold.foldOriented(pos));
    }

    @Override
    public AABB nearestCopy(Vec3 ref, AABB box) {
        return this.fold.nearestCopyOriented(ref, box).value();
    }

    @Override
    public SeamShift shiftToNearestCopy(Vec3 ref, Vec3 anchor) {
        return new SeamShiftView(this.fold.nearestCopyTransformation(ref, anchor));
    }

    @Override
    public SeamShift shiftToNearestCopy(BlockPos ref, BlockPos anchor) {
        return new SeamShiftView(this.fold.nearestCopyTransformation(ref, anchor));
    }

    @Override
    public Oriented<Vec3> nearestCopyOriented(Vec3 ref, Vec3 target) {
        return oriented(this.fold.nearestCopyOriented(ref, target));
    }

    @Override
    public Oriented<BlockPos> nearestCopyOriented(BlockPos ref, BlockPos target) {
        return oriented(this.fold.nearestCopyOriented(ref, target));
    }

    @Override
    public Oriented<AABB> nearestCopyOriented(Vec3 ref, AABB box) {
        return oriented(this.fold.nearestCopyOriented(ref, box));
    }

    private static <T> Oriented<T> oriented(WorldFold.Folded<T> folded) {
        return oriented(folded.value(), folded.orientation());
    }

    private static <T> Oriented<T> oriented(T value, FoldOrientation orientation) {
        return new Oriented<>(value, orientationOf(orientation));
    }

    static Orientation orientationOf(FoldOrientation orientation) {
        if (orientation.isSwappingAxes()) {
            throw new IllegalStateException("The api's Orientation carries no quarter turn, got " + orientation);
        }

        return new Orientation(orientation.isXFlipped(), orientation.isZFlipped());
    }
}

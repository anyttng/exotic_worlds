package com.exoticworlds.bridge.v2.geometry;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.geometry.OrientedV2;
import com.exoticworlds.api.v2.geometry.SeamShiftV2;
import com.exoticworlds.api.v2.geometry.WorldGeometryV2;
import com.exoticworlds.api.v2.shape.LoopSpansV2;
import com.exoticworlds.bridge.v2.shape.ShapeSpansAdapter;
import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.fold.RectangleCopies;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class WorldGeometryView implements WorldGeometryV2 {
    private final WorldFold fold;
    private @Nullable LoopSpansV2 spans;

    WorldGeometryView(WorldFold fold) {
        this.fold = fold;
    }

    @Override
    public LoopSpansV2 getSpans() {
        LoopSpansV2 stated = this.spans;
        if (stated == null) {
            stated = spansOf(this.fold);
            this.spans = stated;
        }

        return stated;
    }

    private static LoopSpansV2 spansOf(WorldFold fold) {
        return ShapeSpansAdapter.loopSpans(FlatShape.latticeTorus(fold.getBounds(), fold.getChunkLattice().getSkew()));
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
    public AABB nearestCopy(Vec3 ref, AABB box) {
        return this.fold.nearestCopyOriented(ref, box).value();
    }

    @Override
    public SeamShiftV2 shiftToNearestCopy(Vec3 ref, Vec3 anchor) {
        return new GeometryShiftView(this.fold.nearestCopyTransformation(ref, anchor));
    }

    @Override
    public SeamShiftV2 shiftToNearestCopy(BlockPos ref, BlockPos anchor) {
        return new GeometryShiftView(this.fold.nearestCopyTransformation(ref, anchor));
    }

    @Override
    public Vec3 shortestDelta(Vec3 from, Vec3 to) {
        return this.fold.foldDelta(from, to);
    }

    @Override
    public double sqrDistanceBlocks(Vec3 from, Vec3 to) {
        return this.fold.sqrDistanceBlocks(from.x, from.y, from.z, to.x, to.y, to.z);
    }

    @Override
    public boolean areBoxesOverlapping(AABB first, AABB second) {
        return this.fold.areBoxesOverlapping(first, second);
    }

    @Override
    public List<OrientedV2<AABB>> split(AABB box) {
        List<WorldFold.Folded<AABB>> pieces = this.fold.split(box);
        List<OrientedV2<AABB>> oriented = new ArrayList<>(pieces.size());
        for (WorldFold.Folded<AABB> piece : pieces) {
            oriented.add(oriented(piece));
        }

        return oriented;
    }

    @Override
    public List<SeamShiftV2> copiesMeeting(AABB box) {
        List<DeckTransformation> copies = RectangleCopies.meeting(this.fold, box);
        List<SeamShiftV2> shifts = new ArrayList<>(copies.size());
        for (DeckTransformation copy : copies) {
            shifts.add(new GeometryShiftView(copy));
        }

        return shifts;
    }

    @Override
    public List<OrientedV2<Vec3>> copiesInside(AABB box, Vec3 pos) {
        return RectangleCopies.inside(this.fold, box, pos,
                (value, orientation) -> new OrientedV2<>(value, OrientationView.of(orientation)));
    }

    @Override
    public List<OrientedV2<BlockPos>> copiesInside(AABB box, BlockPos pos) {
        return RectangleCopies.inside(this.fold, box, pos,
                (value, orientation) -> new OrientedV2<>(value, OrientationView.of(orientation)));
    }

    @Override
    public boolean areLocalIndicesPreserved() {
        return this.fold.isPreservingLocalIndices();
    }

    @Override
    public OrientedV2<BlockPos> foldOriented(BlockPos pos) {
        return oriented(this.fold.foldOriented(pos));
    }

    @Override
    public OrientedV2<Vec3> foldOriented(Vec3 pos) {
        return oriented(this.fold.foldOriented(pos));
    }

    @Override
    public OrientedV2<ChunkPos> foldOriented(ChunkPos pos) {
        return oriented(this.fold.foldOriented(pos));
    }

    @Override
    public OrientedV2<Vec3> nearestCopyOriented(Vec3 ref, Vec3 target) {
        return oriented(this.fold.nearestCopyOriented(ref, target));
    }

    @Override
    public OrientedV2<BlockPos> nearestCopyOriented(BlockPos ref, BlockPos target) {
        return oriented(this.fold.nearestCopyOriented(ref, target));
    }

    @Override
    public OrientedV2<AABB> nearestCopyOriented(Vec3 ref, AABB box) {
        return oriented(this.fold.nearestCopyOriented(ref, box));
    }

    private static <T> OrientedV2<T> oriented(WorldFold.Folded<T> folded) {
        return new OrientedV2<>(folded.value(), OrientationView.of(folded.orientation()));
    }
}

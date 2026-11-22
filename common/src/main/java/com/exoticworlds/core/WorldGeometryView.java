package com.exoticworlds.core;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.api.v2.geometry.Oriented;
import com.exoticworlds.api.v2.geometry.SeamShift;
import com.exoticworlds.api.v2.geometry.WorldGeometry;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class WorldGeometryView implements WorldGeometry {
    private final WorldFold fold;
    private @Nullable LoopSpans spans;

    WorldGeometryView(WorldFold fold) {
        this.fold = fold;
    }

    @Override
    public LoopSpans spans() {
        LoopSpans stated = this.spans;
        if (stated == null) {
            stated = spansOf(this.fold);
            this.spans = stated;
        }

        return stated;
    }

    private static LoopSpans spansOf(WorldFold fold) {
        WorldLoopBounds bounds = fold.bounds();
        LoopSpans spans = LoopSpans.NONE;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (bounds.axis(axis) instanceof AxisBounds.Looped looped) {
                spans = spans.and(axis, looped.minChunk(), looped.maxChunk());
            }
        }

        return spans.withSkew(fold.chunkLattice().skew());
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
    public SeamShift shiftToNearestCopy(Vec3 ref, Vec3 anchor) {
        return new GeometryShiftView(this.fold.nearestCopyTransformation(ref, anchor));
    }

    @Override
    public SeamShift shiftToNearestCopy(BlockPos ref, BlockPos anchor) {
        return new GeometryShiftView(this.fold.nearestCopyTransformation(ref, anchor));
    }

    @Override
    public Vec3 shortestDelta(Vec3 from, Vec3 to) {
        return this.fold.foldDelta(from, to);
    }

    @Override
    public double sqrDistance(Vec3 from, Vec3 to) {
        return this.fold.sqrDistance(from.x, from.y, from.z, to.x, to.y, to.z);
    }

    @Override
    public boolean boxesOverlap(AABB first, AABB second) {
        return this.fold.boxesOverlap(first, second);
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
            shifts.add(new GeometryShiftView(copy));
        }

        return shifts;
    }

    @Override
    public List<Oriented<Vec3>> copiesInside(AABB box, Vec3 pos) {
        return RectangleCopies.inside(this.fold, box, pos, Oriented::new);
    }

    @Override
    public List<Oriented<BlockPos>> copiesInside(AABB box, BlockPos pos) {
        return RectangleCopies.inside(this.fold, box, pos, Oriented::new);
    }

    @Override
    public boolean preservesLocalIndices() {
        return this.fold.preservesLocalIndices();
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
        return new Oriented<>(folded.value(), folded.orientation());
    }
}

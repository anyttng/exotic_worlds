package com.exoticworlds.api.v2.geometry;

import java.util.List;

import com.exoticworlds.api.v2.shape.LoopSpans;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The geometry of one wrapped level: the spans it folds into and the folding operations that geometry defines. Obtain
 * one from {@link WorldGeometries#of(net.minecraft.world.level.Level)} on the server or from
 * {@link com.exoticworlds.api.v2.client.ClientGeometries} on the client; a geometry exists only for a level that
 * wraps.
 *
 * <p>Every member takes a whole position. No member folds one axis alone, because crossing a seam on one axis may
 * move or turn the other. {@link Direction.Axis#Y} passes through every member untouched. A geometry is an immutable
 * view: cheap to hold, valid as long as its level.</p>
 *
 * <p>Every fold here hands back the argument instance itself when it moved nothing: {@code fold(pos) == pos},
 * {@code nearestCopy(ref, target) == target}, and likewise for the {@code value()} of an {@link Oriented} result. A
 * caller may read that {@code ==} as "nothing moved" and skip the copy, the allocation or the write it would
 * otherwise make. {@link #shortestDelta(Vec3, Vec3)} is the one exception: its result is not one of its arguments,
 * so it can only answer by value.</p>
 */
public interface WorldGeometry {

    /**
     * Which horizontal axes loop, over what span of chunks, and how far crossing the Z seam shifts X. A bound or a
     * width is read here; a copy of the world is never rebuilt from a width, since a skewed copy one row over is also
     * moved sideways: {@link #copiesMeeting(AABB)} lists the copies.
     *
     * @throws IllegalStateException if this geometry is no flat shape that spans can state
     */
    LoopSpans spans();

    /**
     * The position folded into the world; an in-bounds position comes back as the argument instance itself.
     */
    BlockPos fold(BlockPos pos);

    /**
     * The position folded into the world; an in-bounds position comes back as the argument instance itself.
     */
    Vec3 fold(Vec3 pos);

    /**
     * The chunk position folded into the world; an in-bounds position comes back as the argument instance itself.
     */
    ChunkPos fold(ChunkPos pos);

    /**
     * The copy of {@code target} nearest {@code ref}: the coordinates a renderer or a distance check should use so
     * that something just across the seam reads as beside the reference, not a world away. {@code target} may lie any
     * number of laps out. A {@code target} already nearest {@code ref} comes back as the argument instance itself.
     */
    Vec3 nearestCopy(Vec3 ref, Vec3 target);

    /** {@link #nearestCopy(Vec3, Vec3)} on the block grid. */
    BlockPos nearestCopy(BlockPos ref, BlockPos target);

    /**
     * The whole box carried into the copy nearest {@code ref}, chosen by where its center lands. The box moves
     * rigidly: its size never changes and it is never split at a seam, so a box straddling one comes back straddling
     * it. That is what a range check, a selection or a rendered bound wants: one box in the frame the reference is
     * in, not the pieces the world would cut it into. A box already nearest {@code ref} comes back as the argument
     * instance itself.
     */
    AABB nearestCopy(Vec3 ref, AABB box);

    /**
     * The one move that carries a whole rigid group into the copy nearest {@code ref}, chosen by where
     * {@code anchor}, one position of the group, lands. Apply the result to every member: moving each member on its
     * own with {@link #nearestCopy(Vec3, Vec3)} lets two of them pick different copies and tears the group in half.
     */
    SeamShift shiftToNearestCopy(Vec3 ref, Vec3 anchor);

    /** {@link #shiftToNearestCopy(Vec3, Vec3)} on the block grid. */
    SeamShift shiftToNearestCopy(BlockPos ref, BlockPos anchor);

    /**
     * The shortest vector from {@code from} to {@code to}, measured through the seam where that is shorter: what a
     * waypoint arrow, a distance readout or a direction indicator needs. Equal to
     * {@code nearestCopy(from, to).subtract(from)}. Always a fresh vector: unlike the folds, it never hands an
     * argument back.
     */
    Vec3 shortestDelta(Vec3 from, Vec3 to);

    /**
     * The squared distance from {@code from} to {@code to}, measured through the seam where that is shorter: what a
     * range check or a nearest-target search asks once per candidate. Equal to
     * {@code shortestDelta(from, to).lengthSqr()}, Y included, without building either vector.
     */
    double sqrDistance(Vec3 from, Vec3 to);

    /**
     * Whether two boxes overlap in some copy of the world: what an area query asks of a box that may straddle the
     * seam or lie any number of laps out. Faces that only touch do not overlap, as in {@link AABB#intersects(AABB)}.
     */
    boolean boxesOverlap(AABB first, AABB second);

    /**
     * The pieces the world cuts {@code box} into at the seam, each folded into the world: what a mod scanning the
     * blocks or entities of an area walks instead of the box itself. A box inside the world comes back as the one
     * piece, the argument instance itself; one wider than the world is cut down to a single lap. Each piece reports
     * the orientation its fold applied.
     */
    List<Oriented<AABB>> split(AABB box);

    /**
     * Every copy of the world that meets a horizontal rectangle, as the one move that carries the world into it:
     * what a map drawing its tiles asks once per frame. Near a seam, or where the rectangle is wider than the world,
     * that is more than one copy. The rectangle is {@code box} on X and Z, half-open like every span; its Y is
     * ignored, and an empty one meets nothing. Where the world itself meets the rectangle, its
     * {@linkplain SeamShift#isIdentity() identity} shift comes first.
     */
    List<SeamShift> copiesMeeting(AABB box);

    /**
     * Every copy of {@code pos} lying inside a horizontal rectangle: what a marker drawn on a map asks. The rectangle
     * is read as in {@link #copiesMeeting(AABB)}: a position on its min edge is inside, one on its max edge is not.
     * {@code pos} may lie any number of laps out, and a copy that lands where {@code pos} already is comes back as
     * the argument instance itself.
     */
    List<Oriented<Vec3>> copiesInside(AABB box, Vec3 pos);

    /** {@link #copiesInside(AABB, Vec3)} on the block grid, a block read at its own coordinates. */
    List<Oriented<BlockPos>> copiesInside(AABB box, BlockPos pos);

    /**
     * Whether a fold leaves a position's index inside its own chunk alone. {@code false} where crossing a seam turns
     * the local indices, so anything keyed by the low bits of a coordinate (block state arrays, heightmaps,
     * post-processing shorts) must be rebuilt, not carried across.
     */
    boolean preservesLocalIndices();

    /** {@link #fold(BlockPos)}, reporting the orientation the fold applied. */
    Oriented<BlockPos> foldOriented(BlockPos pos);

    /** {@link #fold(Vec3)}, reporting the orientation the fold applied. */
    Oriented<Vec3> foldOriented(Vec3 pos);

    /** {@link #fold(ChunkPos)}, reporting the orientation the fold applied. */
    Oriented<ChunkPos> foldOriented(ChunkPos pos);

    /** {@link #nearestCopy(Vec3, Vec3)}, reporting the orientation of the copy it chose. */
    Oriented<Vec3> nearestCopyOriented(Vec3 ref, Vec3 target);

    /** {@link #nearestCopy(BlockPos, BlockPos)}, reporting the orientation of the copy it chose. */
    Oriented<BlockPos> nearestCopyOriented(BlockPos ref, BlockPos target);

    /** {@link #nearestCopy(Vec3, AABB)}, reporting the orientation of the copy it chose. */
    Oriented<AABB> nearestCopyOriented(Vec3 ref, AABB box);
}

package com.exoticworlds.api.v2.geometry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * One move into one copy of the world, ready to be applied to a whole group of positions that has to stay rigid, such
 * as a contraption, a vehicle, a multiblock or a set of nodes that must not come apart.
 *
 * <p>Moving each position of such a group on its own with {@link WorldGeometryV2#nearestCopy(Vec3, Vec3) nearestCopy}
 * tears it. Two positions beside each other but on opposite sides of half a world width from the reference pick
 * different copies, and the group ends up in two places. Take one shift from an anchor and apply it to every member
 * instead, and the group arrives whole.</p>
 *
 * <p>A shift is not a vector. It may also turn the space it moves, so subtracting the anchor's nearest copy from the
 * original and adding that difference to the rest is wrong there. {@link #getOrientation()} says what that turn does to
 * any direction, velocity or offset carried along.</p>
 *
 * <p>Every {@code apply} hands back the argument instance itself where the shift moved nothing, and an {@linkplain
 * #isIdentity() identity} shift is the normal answer for a group already in the right copy.</p>
 */
public interface SeamShiftV2 {

    /** Whether this shift moves nothing, as it does for a group already in the copy the reference is in. */
    boolean isIdentity();

    /** What this shift does to the space it moves. Apply it to any direction or velocity carried with the group. */
    OrientationV2 getOrientation();

    /** {@code pos} moved by this shift. */
    Vec3 apply(Vec3 pos);

    /** {@code pos} moved by this shift. */
    BlockPos apply(BlockPos pos);

    /** {@code chunk} moved by this shift. */
    ChunkPos apply(ChunkPos chunk);

    /** {@code box} moved by this shift, rigidly. Its size never changes and it is never split at a seam. */
    AABB apply(AABB box);
}

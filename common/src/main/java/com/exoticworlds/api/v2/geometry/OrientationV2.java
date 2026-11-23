package com.exoticworlds.api.v2.geometry;

import net.minecraft.world.phys.Vec3;

/**
 * How a fold or a shift turned the space around the position it moved. Exotic Worlds hands these out; a mod reads
 * them and never implements this interface, so a member added later breaks nothing.
 *
 * <p>An orientation is asked what it does to a value, never which axes it reversed: a turn that swaps the horizontal
 * axes has no answer in flips alone. {@link net.minecraft.core.Direction.Axis#Y} is never turned.</p>
 */
public interface OrientationV2 {

    /** Whether nothing was turned: the only orientation a shape that preserves local indices reports. */
    boolean isIdentity();

    /** Whether handedness was kept: a half turn or a quarter turn keeps it, a mirror does not. */
    boolean preservesHandedness();

    /**
     * A direction, velocity or offset carried through the same turn. Apply this to any vector that traveled with the
     * moved position, or it points the wrong way on the far side of the seam. An identity orientation hands the
     * argument instance itself back.
     */
    Vec3 applyToDelta(Vec3 delta);
}

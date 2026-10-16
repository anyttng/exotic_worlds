package com.exoticworlds.compat.simpleclouds;

import org.joml.Matrix2f;

import net.minecraft.world.phys.Vec2;

public record CloudRegionSeat(CloudShape shape, float stretch, float rotation, float m00, float m01, float m10,
        float m11, CloudLattice lattice) {
    public static CloudRegionSeat of(CloudShape shape, float stretch, float rotation, Matrix2f transform) {
        return new CloudRegionSeat(shape, stretch, rotation, transform.m00, transform.m01, transform.m10, transform.m11,
                SimpleCloudsShapes.latticeOf(shape, transform.m00, transform.m01, transform.m10, transform.m11));
    }

    public boolean holds(CloudShape shape, float stretch, float rotation) {
        return this.shape == shape && Float.compare(this.stretch, stretch) == 0
                && Float.compare(this.rotation, rotation) == 0;
    }

    public Vec2 seated(float posX, float posZ, float x, float z) {
        return this.lattice.seated(posX, posZ, this.m00, this.m01, this.m10, this.m11, x, z);
    }
}

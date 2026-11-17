package com.exoticworlds.compat.map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;

import net.minecraft.world.phys.Vec3;

class ClientShapesTest {
    private static final ToroidalShape LATTICE = MapShapes.latticeTorus(-16, 16, 16);
    private static final ToroidalShape TORUS = MapShapes.torus(-16, 16);
    private static final ToroidalShape CYLINDER = MapShapes.cylinder(-16, 16);

    @Test
    void aPointPastTheZSeamOfALatticeTorusFoldsWhole() {
        assertEquals(new Vec3(-155.5, 0, -249.5), ClientShapes.fold(LATTICE, 100.5, 262.5),
                "crossing the Z seam moves X by 256 blocks, so a per-axis fold would have kept it at 100.5");
        assertEquals(new Vec3(100.5, 0, -249.5), ClientShapes.fold(TORUS, 100.5, 262.5));
        assertEquals(new Vec3(100.5, 0, 262.5), ClientShapes.fold(CYLINDER, 100.5, 262.5));
        assertEquals(new Vec3(100.5, 0, 262.5), ClientShapes.fold(null, 100.5, 262.5));
    }
}

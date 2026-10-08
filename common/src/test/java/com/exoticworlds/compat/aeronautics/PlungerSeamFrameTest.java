package com.exoticworlds.compat.aeronautics;

import static com.exoticworlds.compat.CompatFoldFixture.CYLINDER;
import static com.exoticworlds.compat.CompatFoldFixture.PER_AXIS;
import static com.exoticworlds.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

import net.minecraft.world.phys.Vec3;

class PlungerSeamFrameTest {
    private static final WorldFold UNWRAPPED = null;

    private static final double OWN_Y = 64.0;
    private static final double OTHER_Y = 70.0;
    private static final double Z = 10.0;
    private static final double OPEN_AXIS_Z = 400.0;

    private static Vec3 own(double x) {
        return new Vec3(x, OWN_Y, Z);
    }

    private static Vec3 other(double x) {
        return new Vec3(x, OTHER_Y, Z);
    }

    @Test
    void anOtherEndAcrossTheSeamIsSeatedOnTheOwnEndsSide() {
        assertEquals(other(-250.0 + WORLD_BLOCKS), PlungerSeamFrame.seatOther(PER_AXIS, own(250.0), other(-250.0)));
        assertEquals(other(250.0 - WORLD_BLOCKS), PlungerSeamFrame.seatOther(PER_AXIS, own(-250.0), other(250.0)));
    }

    @Test
    void theDifferenceAcrossTheSeamIsTheShortOne() {
        Vec3 difference = PlungerSeamFrame.seatOther(PER_AXIS, own(250.0), other(-250.0)).subtract(own(250.0));

        assertEquals(new Vec3(WORLD_BLOCKS - 500.0, OTHER_Y - OWN_Y, 0.0), difference);
    }

    @Test
    void anInlandOtherEndComesBackByIdentity() {
        Vec3 inland = other(-4.0);

        assertSame(inland, PlungerSeamFrame.seatOther(PER_AXIS, own(4.0), inland));
    }

    @Test
    void aCylinderSeatsAcrossItsLoopingAxisAndLeavesTheOpenOne() {
        Vec3 ownEnd = new Vec3(250.0, OWN_Y, OPEN_AXIS_Z);
        Vec3 across = new Vec3(-250.0, OTHER_Y, -OPEN_AXIS_Z);

        assertEquals(new Vec3(-250.0 + WORLD_BLOCKS, OTHER_Y, -OPEN_AXIS_Z),
                PlungerSeamFrame.seatOther(CYLINDER, ownEnd, across));
    }

    @Test
    void anUnwrappedWorldGivesTheOtherEndBackByIdentity() {
        Vec3 across = other(-250.0);

        assertSame(across, PlungerSeamFrame.seatOther(UNWRAPPED, own(250.0), across));
        assertSame(across, PlungerSeamFrame.seatOther(WorldFolds.NOOP, own(250.0), across));
    }
}

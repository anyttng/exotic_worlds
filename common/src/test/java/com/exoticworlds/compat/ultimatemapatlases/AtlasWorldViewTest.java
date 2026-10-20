package com.exoticworlds.compat.ultimatemapatlases;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapShapes;

import net.minecraft.world.phys.Vec3;

class AtlasWorldViewTest {
    private static final int MAP_BLOCKS = 128;
    private static final int WIDTH = 256;
    private static final int HEIGHT = 128;

    @Test
    void theCeilingFollowsTheCopiesSetting() {
        ToroidalShape shape = MapShapes.torus(-16, 16);
        assertEquals(4.0F, AtlasWorldView.zoomCeiling(shape, MapCopies.SINGLE, WIDTH, HEIGHT, MAP_BLOCKS),
                "one copy: the 512-block world covers a 256 px view at 4 maps across");
        assertEquals(16.0F, AtlasWorldView.zoomCeiling(shape, MapCopies.REPEATED, WIDTH, HEIGHT, MAP_BLOCKS),
                "repeated copies: the world stays 64 px wide at 16 maps across");
        assertEquals(Float.MAX_VALUE, AtlasWorldView.zoomCeiling(null, MapCopies.SINGLE, WIDTH, HEIGHT, MAP_BLOCKS),
                "an unwrapped level has no ceiling");
    }

    @Test
    void aDiscreteZoomRestsOnTheLastStepUnderTheCeiling() {
        assertEquals(14.0F, AtlasWorldView.discreteZoomSteps(16.0F), "a ceiling of 16 rests 14 steps out");
        assertEquals(0.0F, AtlasWorldView.discreteZoomSteps(3.5F), "a ceiling under the second step rests on the first");
        assertEquals(0.0F, AtlasWorldView.discreteZoomSteps(1.0F), "a ceiling under the first step never goes negative");
    }

    @Test
    void aSingleViewFoldsThenHoldsInsideTheWorld() {
        ToroidalShape shape = MapShapes.torus(-16, 16);
        assertEquals(new Vec3(88.0, 0.0, 0.0),
                AtlasWorldView.held(shape, new Vec3(600.0, 0.0, 0.0), 2.0F, WIDTH, HEIGHT, MAP_BLOCKS, true),
                "600 folds to 88, already half a 256-block window inside the world");
        assertEquals(new Vec3(128.0, 0.0, 192.0),
                AtlasWorldView.held(shape, new Vec3(600.0, 0.0, 300.0), 2.0F, WIDTH, HEIGHT, MAP_BLOCKS, false),
                "unfolded, X stops 128 blocks inside the bound and Z, with a 64-block half height, 64 inside");
    }

    @Test
    void aSkewedFoldMovesBothAxesBeforeTheHold() {
        ToroidalShape shape = MapShapes.latticeTorus(-16, 16, 8);
        Vec3 folded = shape.fold(new Vec3(0.0, 0.0, 300.0));
        assertEquals(new Vec3(folded.x, 0.0, folded.z),
                AtlasWorldView.held(shape, new Vec3(0.0, 0.0, 300.0), 1.0F, WIDTH, HEIGHT, MAP_BLOCKS, true),
                "the Z lap moves X by the skew, and both land inside the held window");
    }

    @Test
    void aCylinderHoldsItsLoopedAxisAlone() {
        assertEquals(new Vec3(128.0, 0.0, 5000.0), AtlasWorldView.held(MapShapes.cylinder(-16, 16),
                new Vec3(600.0, 0.0, 5000.0), 2.0F, WIDTH, HEIGHT, MAP_BLOCKS, false),
                "X holds inside the world and the unbounded Z goes where it was asked");
    }
}

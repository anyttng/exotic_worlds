package com.exoticworlds.compat.map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.world.phys.Vec3;

class AtlasWorldViewTest {
    private static final int MAP_BLOCKS = 128;
    private static final int WIDTH_PIXELS = 256;
    private static final int HEIGHT_PIXELS = 128;
    private static final float WIDGET_MAX_ZOOM = 20.0F;
    private static final WorldFold WIDE = MapFolds.torus(-937, 938);

    @Test
    void theCeilingFollowsTheCopiesSetting() {
        WorldFold shape = MapFolds.torus(-16, 16);
        assertEquals(4.0F, AtlasWorldView.zoomCeiling(shape, MapCopies.SINGLE, WIDTH_PIXELS, HEIGHT_PIXELS, MAP_BLOCKS),
                "one copy: the 512-block world covers a 256 px view at 4 maps across");
        assertEquals(16.0F, AtlasWorldView.zoomCeiling(shape, MapCopies.REPEATED, WIDTH_PIXELS, HEIGHT_PIXELS, MAP_BLOCKS),
                "repeated copies: the world stays 64 px wide at 16 maps across");
        assertEquals(Float.MAX_VALUE, AtlasWorldView.zoomCeiling(null, MapCopies.SINGLE, WIDTH_PIXELS, HEIGHT_PIXELS, MAP_BLOCKS),
                "an unwrapped level has no ceiling");
    }

    @Test
    void aWideWorldsCeilingSitsPastTheWidgetsOwn() {
        assertEquals(234.375F, AtlasWorldView.zoomCeiling(WIDE, MapCopies.SINGLE, WIDTH_PIXELS, HEIGHT_PIXELS, MAP_BLOCKS),
                "one copy: 30000 blocks cover a 256 px view at 234.375 maps across, X holding tighter than Z");
        assertEquals(937.5F, AtlasWorldView.zoomCeiling(WIDE, MapCopies.REPEATED, WIDTH_PIXELS, HEIGHT_PIXELS, MAP_BLOCKS),
                "repeated copies: 30000 blocks stay 64 px wide at 937.5 maps across");
    }

    @Test
    void aDiscreteZoomRestsOnTheLastStepUnderTheCeiling() {
        assertEquals(14.0F, AtlasWorldView.discreteZoomSteps(16.0F), "a ceiling of 16 rests 14 steps out");
        assertEquals(0.0F, AtlasWorldView.discreteZoomSteps(3.5F), "a ceiling under the second step rests on the first");
        assertEquals(0.0F, AtlasWorldView.discreteZoomSteps(1.0F), "a ceiling under the first step never goes negative");
    }

    @Test
    void aSingleViewFoldsThenHoldsInsideTheWorld() {
        WorldFold shape = MapFolds.torus(-16, 16);
        assertEquals(new Vec3(88.0, 0.0, 0.0),
                AtlasWorldView.held(shape, new Vec3(600.0, 0.0, 0.0), 2.0F, WIDTH_PIXELS, HEIGHT_PIXELS, MAP_BLOCKS, true),
                "600 folds to 88, already half a 256-block window inside the world");
        assertEquals(new Vec3(128.0, 0.0, 192.0),
                AtlasWorldView.held(shape, new Vec3(600.0, 0.0, 300.0), 2.0F, WIDTH_PIXELS, HEIGHT_PIXELS, MAP_BLOCKS, false),
                "unfolded, X stops 128 blocks inside the bound and Z, with a 64-block half height, 64 inside");
    }

    @Test
    void aSkewedFoldMovesBothAxesBeforeTheHold() {
        WorldFold shape = MapFolds.latticeTorus(-16, 16, 8);
        Vec3 folded = shape.fold(new Vec3(0.0, 0.0, 300.0));
        assertEquals(new Vec3(folded.x, 0.0, folded.z),
                AtlasWorldView.held(shape, new Vec3(0.0, 0.0, 300.0), 1.0F, WIDTH_PIXELS, HEIGHT_PIXELS, MAP_BLOCKS, true),
                "the Z lap moves X by the skew, and both land inside the held window");
    }

    @Test
    void aCylinderHoldsItsLoopedAxisAlone() {
        assertEquals(new Vec3(128.0, 0.0, 5000.0), AtlasWorldView.held(MapFolds.cylinder(-16, 16),
                new Vec3(600.0, 0.0, 5000.0), 2.0F, WIDTH_PIXELS, HEIGHT_PIXELS, MAP_BLOCKS, false),
                "X holds inside the world and the unbounded Z goes where it was asked");
    }

    @Test
    void aSingleHoldCapsTheZoomAndHoldsBothCenters() {
        HeldWidget widget = new HeldWidget(WIDGET_MAX_ZOOM, new Vec3(600.0, 0.0, 0.0));
        AtlasWorldView.hold(MapFolds.torus(-16, 16), MapCopies.SINGLE, widget, true);
        assertEquals(4.0F, widget.zoom, "the zoom stops where one copy covers the view");
        assertEquals(4.0F, widget.targetZoom, "the target zoom stops there too");
        assertEquals(2.0F, widget.zoomSteps, "the discrete steps rest on the last one under 4");
        assertEquals(new Vec3(0.0, 0.0, 0.0), widget.center,
                "600 folds to 88, and a 512-block window over a 512-block world sits at its middle");
        assertEquals(widget.center, widget.targetCenter, "the target center is held the same way");
    }

    @Test
    void aRepeatedHoldCapsTheZoomAndLeavesTheCenters() {
        HeldWidget widget = new HeldWidget(WIDGET_MAX_ZOOM, new Vec3(600.0, 0.0, 0.0));
        AtlasWorldView.hold(MapFolds.torus(-16, 16), MapCopies.REPEATED, widget, true);
        assertEquals(16.0F, widget.zoom, "the world stays 64 px wide");
        assertEquals(14.0F, widget.zoomSteps, "the discrete steps rest on the last one under 16");
        assertEquals(new Vec3(600.0, 0.0, 0.0), widget.center, "copies fill the view, so the center goes where it was");
    }

    @Test
    void aWideWorldHoldsTheViewAndLeavesTheZoom() {
        HeldWidget widget = new HeldWidget(2.0F, new Vec3(15100.0, 0.0, 0.0));
        AtlasWorldView.hold(WIDE, MapCopies.SINGLE, widget, true);
        assertEquals(2.0F, widget.zoom, "a ceiling of 234.375 never reaches a zoom of 2");
        assertEquals(-1.0F, widget.zoomSteps, "the discrete steps are left alone");
        assertEquals(new Vec3(-14864.0, 0.0, 0.0), widget.center,
                "15100 lies past the +X edge at 15008, folds to -14900 and stops 128 blocks inside -14992");
        assertEquals(widget.center, widget.targetCenter, "the target center is held the same way");
    }

    @Test
    void anUnwrappedLevelIsLeftAlone() {
        HeldWidget widget = new HeldWidget(WIDGET_MAX_ZOOM, new Vec3(600.0, 0.0, 0.0));
        AtlasWorldView.hold(null, MapCopies.SINGLE, widget, true);
        assertEquals(WIDGET_MAX_ZOOM, widget.zoom, "no world bounds the zoom");
        assertEquals(new Vec3(600.0, 0.0, 0.0), widget.center, "no world holds the view");
    }

    private static final class HeldWidget implements AtlasWorldWidget {
        private float zoom;
        private float targetZoom;
        private float zoomSteps = -1.0F;
        private Vec3 center;
        private Vec3 targetCenter;

        private HeldWidget(float zoom, Vec3 center) {
            this.zoom = zoom;
            this.targetZoom = zoom;
            this.center = center;
            this.targetCenter = center;
        }

        @Override
        public int exotic_worlds$getMapBlocks() {
            return MAP_BLOCKS;
        }

        @Override
        public int exotic_worlds$getWidthPixels() {
            return WIDTH_PIXELS;
        }

        @Override
        public int exotic_worlds$getHeightPixels() {
            return HEIGHT_PIXELS;
        }

        @Override
        public float exotic_worlds$getZoom() {
            return this.zoom;
        }

        @Override
        public void exotic_worlds$setZoom(float zoom) {
            this.zoom = zoom;
        }

        @Override
        public float exotic_worlds$getTargetZoom() {
            return this.targetZoom;
        }

        @Override
        public void exotic_worlds$setTargetZoom(float zoom) {
            this.targetZoom = zoom;
        }

        @Override
        public void exotic_worlds$setZoomSteps(float steps) {
            this.zoomSteps = steps;
        }

        @Override
        public Vec3 exotic_worlds$getCenter() {
            return this.center;
        }

        @Override
        public void exotic_worlds$setCenter(Vec3 center) {
            this.center = center;
        }

        @Override
        public Vec3 exotic_worlds$getTargetCenter() {
            return this.targetCenter;
        }

        @Override
        public void exotic_worlds$setTargetCenter(Vec3 center) {
            this.targetCenter = center;
        }
    }
}

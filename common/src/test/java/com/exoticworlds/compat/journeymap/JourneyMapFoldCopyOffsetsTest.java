package com.exoticworlds.compat.journeymap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.WorldCopies;

class JourneyMapFoldCopyOffsetsTest {
    private static final Rectangle2D.Double SCREEN = new Rectangle2D.Double(0.0, 0.0, 1000.0, 600.0);
    private static final List<WorldCopies.Copy> WORLD_ALONE = List.of(WorldCopies.IDENTITY);
    private static final List<WorldCopies.Copy> ALONG_X = alongX(-400, 0, 400, 800, 1200);

    @Test
    void aBaseOnScreenDrawnAloneDrawsOnceAtTheBase() {
        double[][] offsets = JourneyMapFold.offsetsOnScreen(WORLD_ALONE, 1.0, rect(100.0, 100.0, 50.0, 50.0), SCREEN);
        assertArrayEquals(new double[][] {{0.0, 0.0}}, offsets, "a world drawn alone got a copy");
    }

    @Test
    void aBaseOffScreenDrawnAloneDrawsNothing() {
        double[][] offsets = JourneyMapFold.offsetsOnScreen(WORLD_ALONE, 1.0, rect(2000.0, 100.0, 50.0, 50.0), SCREEN);
        assertEquals(0, offsets.length, "a world drawn alone drew an overlay past its screen");
    }

    @Test
    void everyDrawnCopyMeetingTheScreenIsListed() {
        double[][] offsets = JourneyMapFold.offsetsOnScreen(ALONG_X, 1.0, rect(100.0, 100.0, 50.0, 50.0), SCREEN);
        assertArrayEquals(new double[][] {{0.0, 0.0}, {400.0, 0.0}, {800.0, 0.0}}, offsets,
                "copies 0, 400 and 800 put the 50-px overlay at 100 inside a 1000-px screen; -400 sits at -300");
    }

    @Test
    void aBaseOffScreenStillDrawsTheCopyThatIsOn() {
        double[][] offsets = JourneyMapFold.offsetsOnScreen(ALONG_X, 1.0, rect(-300.0, 100.0, 50.0, 50.0), SCREEN);
        assertArrayEquals(new double[][] {{400.0, 0.0}, {800.0, 0.0}, {1200.0, 0.0}}, offsets,
                "the base at -300 is off screen; its copies at 100, 500 and 900 are on");
    }

    @Test
    void aCopyLargerThanTheScreenStillReachesAcrossTheSeam() {
        double[][] offsets = JourneyMapFold.offsetsOnScreen(alongX(-4000, 0, 4000), 1.0, rect(-3050.0, 100.0, 100.0, 50.0),
                SCREEN);
        assertArrayEquals(new double[][] {{4000.0, 0.0}}, offsets,
                "one world up puts the overlay at 950, the only copy inside the screen");
    }

    @Test
    void onlyTheDrawnCopiesAreListed() {
        double[][] offsets = JourneyMapFold.offsetsOnScreen(alongX(0, 400), 1.0, rect(100.0, 100.0, 50.0, 50.0), SCREEN);
        assertArrayEquals(new double[][] {{0.0, 0.0}, {400.0, 0.0}}, offsets,
                "the copy at 900 meets the screen but the terrain under it was not drawn");
    }

    @Test
    void theCopiesListedAreTheOnesDrawnInBothAxes() {
        List<WorldCopies.Copy> copies = List.of(new WorldCopies.Copy(0, 0), new WorldCopies.Copy(0, 500),
                new WorldCopies.Copy(600, 0), new WorldCopies.Copy(600, 500));
        double[][] offsets = JourneyMapFold.offsetsOnScreen(copies, 1.0, rect(100.0, 100.0, 50.0, 50.0), SCREEN);
        assertArrayEquals(new double[][] {{0.0, 0.0}, {0.0, 500.0}, {600.0, 0.0}, {600.0, 500.0}}, offsets,
                "a drawn copy was dropped or reordered");
    }

    @Test
    void aSkewedCopyKeepsItsSidewaysMove() {
        double[][] offsets = JourneyMapFold.offsetsOnScreen(List.of(new WorldCopies.Copy(256, 512)), 1.0,
                rect(100.0, -400.0, 50.0, 50.0), SCREEN);
        assertArrayEquals(new double[][] {{256.0, 512.0}}, offsets, "the copy one Z lap up lost its skew");
    }

    @Test
    void theMoveIsScaledByTheZoom() {
        double[][] offsets = JourneyMapFold.offsetsOnScreen(alongX(512), JourneyMapFold.pixelsPerBlock(256),
                rect(100.0, 100.0, 50.0, 50.0), SCREEN);
        assertArrayEquals(new double[][] {{256.0, 0.0}}, offsets, "512 blocks at zoom 256 are not 256 px");
    }

    @Test
    void anOverlayWiderThanACopyOverlapsItsOwnCopies() {
        double[][] offsets = JourneyMapFold.offsetsOnScreen(alongX(-200, -100, 0, 100, 200), 1.0, rect(0.0, 0.0, 250.0, 50.0),
                SCREEN);
        assertEquals(5, offsets.length, "copies -200..200 all meet a 1000-px screen with a 250-px overlay");
    }

    @Test
    void aSingleCopyOverlayKeepsTheCopyNearestTheScreenCentre() {
        double[][] offsets = {{-400.0, 0.0}, {0.0, 0.0}, {400.0, 0.0}};
        double[][] kept = JourneyMapFold.nearestCopyOffset(offsets, rect(100.0, 275.0, 50.0, 50.0), SCREEN);
        assertArrayEquals(new double[][] {{400.0, 0.0}}, kept,
                "the copy at 500..550 sits nearest the screen centre at 500");
    }

    @Test
    void aSingleCopyOverlayOffEveryCopyStaysOff() {
        assertEquals(0, JourneyMapFold.nearestCopyOffset(new double[0][], rect(0.0, 0.0, 1.0, 1.0), SCREEN).length,
                "no visible copy produced one");
    }

    private static List<WorldCopies.Copy> alongX(int... moves) {
        return Arrays.stream(moves).mapToObj(dx -> new WorldCopies.Copy(dx, 0)).toList();
    }

    private static Rectangle2D.Double rect(double x, double y, double width, double height) {
        return new Rectangle2D.Double(x, y, width, height);
    }
}

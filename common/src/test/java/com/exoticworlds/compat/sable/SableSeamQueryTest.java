package com.exoticworlds.compat.sable;

import static com.exoticworlds.compat.CompatFoldFixture.PER_AXIS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;

class SableSeamQueryTest {
    private static final BoundingBox3d QUERY_ACROSS_THE_X_SEAM = new BoundingBox3d(250.0, 64.0, 0.0, 262.0, 70.0, 8.0);
    private static final BoundingBox3d SUB_LEVEL_PAST_THE_X_SEAM = new BoundingBox3d(-255.0, 64.0, 0.0, -250.0, 70.0, 8.0);
    private static final BoundingBox3d SUB_LEVEL_PAST_THE_X_SEAM_ABOVE = new BoundingBox3d(-255.0, 71.0, 0.0, -250.0, 80.0, 8.0);
    private static final BoundingBox3d SUB_LEVEL_PAST_THE_X_SEAM_ON_TOP = new BoundingBox3d(-255.0, 70.0, 0.0, -250.0, 80.0, 8.0);
    private static final BoundingBox3d SUB_LEVEL_PAST_THE_X_SEAM_ASIDE = new BoundingBox3d(-255.0, 64.0, 9.0, -250.0, 70.0, 16.0);

    private static final BoundingBox3d QUERY_ACROSS_THE_Z_SEAM = new BoundingBox3d(0.0, 64.0, 250.0, 8.0, 70.0, 262.0);
    private static final BoundingBox3d SUB_LEVEL_PAST_THE_Z_SEAM = new BoundingBox3d(0.0, 64.0, -255.0, 8.0, 70.0, -250.0);

    private static final BoundingBox3d QUERY_INSIDE_THE_NEAR_HALF = new BoundingBox3d(245.0, 64.0, 0.0, 255.0, 70.0, 8.0);
    private static final BoundingBox3d SUB_LEVEL_TOUCHING_IT = new BoundingBox3d(240.0, 64.0, 0.0, 250.0, 70.0, 8.0);
    private static final BoundingBox3d SUB_LEVEL_ELSEWHERE = new BoundingBox3d(0.0, 64.0, 0.0, 8.0, 70.0, 8.0);

    @Test
    void aSubLevelPastTheXSeamIsReadFromTheOtherSide() {
        assertFalse(SUB_LEVEL_PAST_THE_X_SEAM.intersects(QUERY_ACROSS_THE_X_SEAM),
                "the fixture already intersects without the fold, so it proves nothing");
        assertTrue(intersects(PER_AXIS, SUB_LEVEL_PAST_THE_X_SEAM, QUERY_ACROSS_THE_X_SEAM));
    }

    @Test
    void aSubLevelPastTheZSeamIsReadFromTheOtherSide() {
        assertFalse(SUB_LEVEL_PAST_THE_Z_SEAM.intersects(QUERY_ACROSS_THE_Z_SEAM),
                "the fixture already intersects without the fold, so it proves nothing");
        assertTrue(intersects(PER_AXIS, SUB_LEVEL_PAST_THE_Z_SEAM, QUERY_ACROSS_THE_Z_SEAM));
    }

    @Test
    void aSubLevelPastTheSeamButAboveTheQueryIsAMiss() {
        assertFalse(intersects(PER_AXIS, SUB_LEVEL_PAST_THE_X_SEAM_ABOVE, QUERY_ACROSS_THE_X_SEAM));
    }

    @Test
    void aSubLevelPastTheSeamRestingOnTheQuerysTopStillMeetsIt() {
        assertTrue(intersects(PER_AXIS, SUB_LEVEL_PAST_THE_X_SEAM_ON_TOP, QUERY_ACROSS_THE_X_SEAM));
    }

    @Test
    void aSubLevelPastTheSeamButBesideTheQueryIsAMiss() {
        assertFalse(intersects(PER_AXIS, SUB_LEVEL_PAST_THE_X_SEAM_ASIDE, QUERY_ACROSS_THE_X_SEAM));
    }

    @Test
    void aSubLevelTheQueryAlreadyReachesNeedsNoShift() {
        assertTrue(intersects(PER_AXIS, SUB_LEVEL_TOUCHING_IT, QUERY_INSIDE_THE_NEAR_HALF));
    }

    @Test
    void aSubLevelNeitherSideReachesIsStillAMiss() {
        assertFalse(intersects(PER_AXIS, SUB_LEVEL_ELSEWHERE, QUERY_INSIDE_THE_NEAR_HALF));
    }

    @Test
    void aFoldThatWrapsNothingLeavesSableItsOwnAnswer() {
        assertFalse(intersects(WorldFolds.NOOP, SUB_LEVEL_PAST_THE_X_SEAM, QUERY_ACROSS_THE_X_SEAM));
        assertTrue(intersects(WorldFolds.NOOP, SUB_LEVEL_TOUCHING_IT, QUERY_INSIDE_THE_NEAR_HALF));
    }

    private static boolean intersects(WorldFold fold, BoundingBox3d subLevel, BoundingBox3d query) {
        return SableSeamQuery.intersects(fold, SableSeamQuery.centerOf(query), subLevel, query);
    }
}

package com.exoticworlds.compat.sable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.JomlVectors;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.world.phys.Vec3;

class SableSeamDistanceTest {
    private static final int HALF_WIDTH_CHUNKS_X = 16;
    private static final int HALF_WIDTH_CHUNKS_Z = 8;
    private static final WorldFold FOLD = WorldFolds.of(ShapeFixtures.torus(
            ShapeFixtures.loopedBounds(-HALF_WIDTH_CHUNKS_X, HALF_WIDTH_CHUNKS_X, -HALF_WIDTH_CHUNKS_Z, HALF_WIDTH_CHUNKS_Z)));

    private static final Vector3dc FROM = new Vector3d(250.0, 70.0, 100.0);
    private static final Vector3dc TO = new Vector3d(-250.0, 100.0, -100.0);
    private static final Vector3dc TO_HIGH_ABOVE = new Vector3d(-250.0, 200.0, -100.0);

    private static double rectilinear(Vector3dc from, Vector3dc to) {
        return Math.max(Math.abs(to.x() - from.x()), Math.max(Math.abs(to.y() - from.y()), Math.abs(to.z() - from.z())));
    }

    @Test
    void theRectilinearDistanceIsTheWidestFoldedAxis() {
        assertEquals(56.0, rectilinear(FROM, NearestCopy.toward(FOLD, FROM, TO)), 0.0,
                "z folds to 56, wider than the folded x of 12 and the y of 30");
    }

    @Test
    void theRectilinearDistanceCountsHeightUnfolded() {
        assertEquals(130.0, rectilinear(FROM, NearestCopy.toward(FOLD, FROM, TO_HIGH_ABOVE)), 0.0,
                "y is 130 and no axis folds past it");
    }

    @Test
    void theSquaredDistanceSumsEveryFoldedAxis() {
        assertEquals(4180.0, FROM.distanceSquared(NearestCopy.toward(FOLD, FROM, TO)), 0.0,
                "x folds to 12, z to 56, and y stays 30");
    }

    @Test
    void anUnwrappedLevelHandsTheFarPointBack() {
        assertSame(TO, NearestCopy.toward(null, FROM, TO));
    }

    @Test
    void theSquaredDistanceToCoordinatesSumsEveryFoldedAxis() {
        Vec3 to = SableSeamDistance.toward(FOLD, JomlVectors.read(FROM), TO.x(), TO.y(), TO.z());

        assertEquals(4180.0, FROM.distanceSquared(to.x, to.y, to.z), 0.0, "x folds to 12, z to 56, and y stays 30");
    }

    @Test
    void anUnwrappedLevelHandsTheCoordinatesBack() {
        assertEquals(JomlVectors.read(TO), SableSeamDistance.toward((WorldFold) null, JomlVectors.read(FROM), TO.x(), TO.y(), TO.z()));
    }
}

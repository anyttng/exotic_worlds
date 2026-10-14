package com.exoticworlds.compat.mekanism;

import static com.exoticworlds.compat.CompatFoldFixture.CYLINDER;
import static com.exoticworlds.compat.CompatFoldFixture.DECK_TORUS;
import static com.exoticworlds.compat.CompatFoldFixture.PER_AXIS;
import static com.exoticworlds.compat.CompatFoldFixture.SKEWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

class MekanismSeamTest {
    private static final List<WorldFold> PLAIN = List.of(PER_AXIS, DECK_TORUS);

    private static final Vec3 ORIGIN_BEFORE_Z_SEAM = new Vec3(10.5, 64.5, 250.5);
    private static final Vec3 TARGET_PAST_Z_SEAM = new Vec3(10.5, 64.0, 258.5);
    private static final Vec3 ORIGIN_BEFORE_X_SEAM = new Vec3(250.5, 64.5, 10.5);
    private static final Vec3 TARGET_PAST_X_SEAM = new Vec3(258.5, 64.0, 10.5);
    private static final Vec3 INLAND_ORIGIN = new Vec3(10.5, 64.5, 10.5);
    private static final Vec3 TARGET_ABOVE = new Vec3(10.5, 80.0, 10.5);
    private static final Vec3 TARGET_PAST_UNLOOPED_Z = new Vec3(10.5, 64.0, 300.5);

    private static Vec3 mixedStop(Vec3 origin, Direction.Axis axis, Vec3 entity) {
        return origin.with(axis, entity.get(axis));
    }

    private static Vec3 wholeMixedStop(WorldFold fold, Vec3 origin, Direction.Axis axis, Vec3 entity) {
        return fold.nearestCopy(origin, mixedStop(origin, axis, entity));
    }

    @Test
    void latticeZBeamEndsOnItsOwnAxis() {
        Vec3 entity = SKEWED.fold(TARGET_PAST_Z_SEAM);

        assertEquals(mixedStop(ORIGIN_BEFORE_Z_SEAM, Direction.Axis.Z, TARGET_PAST_Z_SEAM),
                MekanismSeam.stopOnAxis(SKEWED, ORIGIN_BEFORE_Z_SEAM, Direction.Axis.Z, entity));
    }

    @Test
    void latticeZBeamSeatedWholeLeavesItsAxis() {
        Vec3 entity = SKEWED.fold(TARGET_PAST_Z_SEAM);

        assertNotEquals(ORIGIN_BEFORE_Z_SEAM.x,
                wholeMixedStop(SKEWED, ORIGIN_BEFORE_Z_SEAM, Direction.Axis.Z, entity).x);
    }

    @Test
    void latticeXBeamEndsOnItsOwnAxis() {
        Vec3 entity = SKEWED.fold(TARGET_PAST_X_SEAM);

        assertEquals(mixedStop(ORIGIN_BEFORE_X_SEAM, Direction.Axis.X, TARGET_PAST_X_SEAM),
                MekanismSeam.stopOnAxis(SKEWED, ORIGIN_BEFORE_X_SEAM, Direction.Axis.X, entity));
    }

    @Test
    void plainTorusStopIsUnchanged() {
        for (WorldFold fold : PLAIN) {
            Vec3 acrossZ = fold.fold(TARGET_PAST_Z_SEAM);
            Vec3 acrossX = fold.fold(TARGET_PAST_X_SEAM);

            assertEquals(wholeMixedStop(fold, ORIGIN_BEFORE_Z_SEAM, Direction.Axis.Z, acrossZ),
                    MekanismSeam.stopOnAxis(fold, ORIGIN_BEFORE_Z_SEAM, Direction.Axis.Z, acrossZ));
            assertEquals(wholeMixedStop(fold, ORIGIN_BEFORE_X_SEAM, Direction.Axis.X, acrossX),
                    MekanismSeam.stopOnAxis(fold, ORIGIN_BEFORE_X_SEAM, Direction.Axis.X, acrossX));
        }
    }

    @Test
    void verticalBeamTakesTheTargetHeight() {
        for (WorldFold fold : List.of(PER_AXIS, DECK_TORUS, SKEWED)) {
            assertEquals(mixedStop(INLAND_ORIGIN, Direction.Axis.Y, TARGET_ABOVE),
                    MekanismSeam.stopOnAxis(fold, INLAND_ORIGIN, Direction.Axis.Y, TARGET_ABOVE));
        }
    }

    @Test
    void unloopedAxisKeepsMekanismsStop() {
        assertEquals(mixedStop(ORIGIN_BEFORE_Z_SEAM, Direction.Axis.Z, TARGET_PAST_UNLOOPED_Z),
                MekanismSeam.stopOnAxis(CYLINDER, ORIGIN_BEFORE_Z_SEAM, Direction.Axis.Z, TARGET_PAST_UNLOOPED_Z));
        assertEquals(mixedStop(ORIGIN_BEFORE_X_SEAM, Direction.Axis.X, TARGET_PAST_X_SEAM),
                MekanismSeam.stopOnAxis(WorldFolds.NOOP, ORIGIN_BEFORE_X_SEAM, Direction.Axis.X, TARGET_PAST_X_SEAM));
    }
}

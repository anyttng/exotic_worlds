package com.exoticworlds.compat.sable;

import static com.exoticworlds.compat.CompatFoldFixture.PER_AXIS;
import static com.exoticworlds.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WorldFold;

import net.minecraft.world.phys.Vec3;

class SeamFrameTest {
    private static final Vector3dc POSE_NEAR_SEAM = new Vector3d(254.5, 70.0, 3.5);
    private static final Vector3dc POSE_ONE_LAP_BACK = new Vector3d(254.5 - WORLD_BLOCKS, 70.0, 3.5);
    private static final Vector3dc POSE_FURTHER_IN = new Vector3d(250.5, 70.0, 3.5);
    private static final Vector3dc POSE_FURTHER_IN_ONE_LAP_BACK = new Vector3d(250.5 - WORLD_BLOCKS, 70.0, 3.5);
    private static final Vec3 ENTITY_ON_FAR_HALF = new Vec3(-253.5, 71.0, 3.5);
    private static final Vec3 ENTITY_ON_NEAR_HALF = new Vec3(252.0, 71.0, 3.5);

    @Test
    void unboundPoseKeepsItsPlace() {
        assertSame(POSE_NEAR_SEAM, SeamFrame.seatPose(POSE_NEAR_SEAM));
        assertFalse(SeamFrame.isBound());
    }

    @Test
    void entityOnTheFarHalfPullsThePoseOneLapBack() {
        Vector3dc seated = bound(PER_AXIS, ENTITY_ON_FAR_HALF, () -> SeamFrame.seatPose(POSE_NEAR_SEAM));
        assertEquals(POSE_ONE_LAP_BACK, seated);
    }

    @Test
    void entityOnTheNearHalfLeavesThePoseAlone() {
        Vector3dc seated = bound(PER_AXIS, ENTITY_ON_NEAR_HALF, () -> SeamFrame.seatPose(POSE_NEAR_SEAM));
        assertSame(POSE_NEAR_SEAM, seated);
    }

    @Test
    void enterBindsTheSubjectsAnchorAndExitUnbindsIt() {
        Object entered = SeamFrame.enter(PER_AXIS, null, ENTITY_ON_FAR_HALF, Function.identity());
        assertTrue(SeamFrame.isBound());
        assertEquals(POSE_ONE_LAP_BACK, SeamFrame.seatPose(POSE_NEAR_SEAM));
        SeamFrame.exit(entered);
        assertFalse(SeamFrame.isBound());
    }

    @Test
    void enteringAnUnwrappedLevelWithNothingBoundKeepsEverything() {
        Object entered = SeamFrame.enter((WorldFold) null, null, ENTITY_ON_FAR_HALF, Function.identity());
        assertSame(SeamFrame.KEPT, entered);
        assertFalse(SeamFrame.isBound());
        SeamFrame.exit(entered);
        assertFalse(SeamFrame.isBound());
    }

    @Test
    void enteringAnUnwrappedLevelStillUnbindsAnOuterFrame() {
        bound(PER_AXIS, ENTITY_ON_FAR_HALF, () -> {
            Object entered = SeamFrame.enter((WorldFold) null, null, ENTITY_ON_FAR_HALF, Function.identity());
            assertFalse(SeamFrame.isBound());
            SeamFrame.exit(entered);
            assertTrue(SeamFrame.isBound());
            return null;
        });
    }

    @Test
    void theBindingIsScopedAndRestoresTheOuterOne() {
        bound(PER_AXIS, ENTITY_ON_FAR_HALF, () -> {
            Vector3dc inner = bound(PER_AXIS, ENTITY_ON_NEAR_HALF, () -> SeamFrame.seatPose(POSE_NEAR_SEAM));
            assertSame(POSE_NEAR_SEAM, inner);
            assertEquals(POSE_ONE_LAP_BACK, SeamFrame.seatPose(POSE_NEAR_SEAM));
            return null;
        });
        assertFalse(SeamFrame.isBound());
    }

    @Test
    void aBodyThatThrowsLeavesNoBindingBehind() {
        assertThrows(IllegalStateException.class, () -> bound(PER_AXIS, ENTITY_ON_FAR_HALF, () -> {
            throw new IllegalStateException("the body");
        }));
        assertFalse(SeamFrame.isBound());
    }

    @Test
    void aThrowFromTheInnerBodyHandsTheOuterBindingBack() {
        bound(PER_AXIS, ENTITY_ON_FAR_HALF, () -> {
            assertThrows(IllegalStateException.class, () -> bound(PER_AXIS, ENTITY_ON_NEAR_HALF, () -> {
                throw new IllegalStateException("the inner body");
            }));
            assertEquals(POSE_ONE_LAP_BACK, SeamFrame.seatPose(POSE_NEAR_SEAM));
            return null;
        });
        assertFalse(SeamFrame.isBound());
    }

    @Test
    void anUnboundScopeReadsThePoseRawAndHandsTheOuterBindingBack() {
        bound(PER_AXIS, ENTITY_ON_FAR_HALF, () -> {
            assertSame(POSE_NEAR_SEAM, SeamFrame.unbound(() -> SeamFrame.seatPose(POSE_NEAR_SEAM)));
            assertFalse(SeamFrame.unbound(SeamFrame::isBound));
            assertTrue(SeamFrame.isBound());
            assertEquals(POSE_ONE_LAP_BACK, SeamFrame.seatPose(POSE_NEAR_SEAM));
            return null;
        });
        assertFalse(SeamFrame.isBound());
    }

    @Test
    void anUnwrappedLevelBindsNothing() {
        Vector3dc seated = bound(null, ENTITY_ON_FAR_HALF, () -> SeamFrame.seatPose(POSE_NEAR_SEAM));
        assertSame(POSE_NEAR_SEAM, seated);
    }

    @Test
    void theSamePoseIsSeatedOnceAndAMovedPoseAnew() {
        bound(PER_AXIS, ENTITY_ON_FAR_HALF, () -> {
            Vector3dc first = SeamFrame.seatPose(POSE_NEAR_SEAM);
            assertSame(first, SeamFrame.seatPose(new Vector3d(POSE_NEAR_SEAM)));

            Vector3dc moved = SeamFrame.seatPose(POSE_FURTHER_IN);
            assertNotSame(first, moved);
            assertEquals(POSE_FURTHER_IN_ONE_LAP_BACK, moved);
            assertEquals(POSE_ONE_LAP_BACK, first);
            return null;
        });
    }

    @Test
    void theAnchorIsResolvedOncePerBindingHoweverManyPosesAreRead() {
        AtomicInteger asked = new AtomicInteger();
        bound(PER_AXIS, counting(asked), ENTITY_ON_FAR_HALF, () -> {
            SeamFrame.seatPose(POSE_NEAR_SEAM);
            SeamFrame.seatPose(POSE_FURTHER_IN);
            SeamFrame.seatPose(POSE_NEAR_SEAM);
            return null;
        });
        assertEquals(1, asked.get());
    }

    @Test
    void aBindingNoPoseAsksAboutNeverResolvesItsAnchor() {
        AtomicInteger asked = new AtomicInteger();
        bound(PER_AXIS, counting(asked), ENTITY_ON_FAR_HALF, () -> null);
        assertEquals(0, asked.get());
    }

    @Test
    void eachNestedBindingResolvesItsOwnAnchorOnce() {
        AtomicInteger outer = new AtomicInteger();
        AtomicInteger inner = new AtomicInteger();
        bound(PER_AXIS, counting(outer), ENTITY_ON_FAR_HALF, () -> {
            SeamFrame.seatPose(POSE_NEAR_SEAM);
            bound(PER_AXIS, counting(inner), ENTITY_ON_NEAR_HALF, () -> {
                SeamFrame.seatPose(POSE_NEAR_SEAM);
                SeamFrame.seatPose(POSE_FURTHER_IN);
                return null;
            });
            SeamFrame.seatPose(POSE_FURTHER_IN);
            return null;
        });
        assertEquals(1, outer.get());
        assertEquals(1, inner.get());
    }

    private static <R> R bound(@Nullable WorldFold fold, Vec3 anchor, Supplier<R> body) {
        return bound(fold, Function.identity(), anchor, body);
    }

    private static <R> R bound(@Nullable WorldFold fold, Function<Vec3, Vec3> anchorOf, Vec3 subject,
            Supplier<R> body) {
        Object entered = SeamFrame.enter(fold, null, subject, anchorOf);
        try {
            return body.get();
        } finally {
            SeamFrame.exit(entered);
        }
    }

    private static Function<Vec3, Vec3> counting(AtomicInteger asked) {
        return anchor -> {
            asked.incrementAndGet();
            return anchor;
        };
    }
}

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
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WorldFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class SeamFrameTest {
    private static final Vector3dc POSE_NEAR_SEAM = new Vector3d(254.5, 70.0, 3.5);
    private static final Vector3dc POSE_ONE_LAP_BACK = new Vector3d(254.5 - WORLD_BLOCKS, 70.0, 3.5);
    private static final Vector3dc POSE_FURTHER_IN = new Vector3d(250.5, 70.0, 3.5);
    private static final Vector3dc POSE_FURTHER_IN_ONE_LAP_BACK = new Vector3d(250.5 - WORLD_BLOCKS, 70.0, 3.5);
    private static final Vec3 ENTITY_ON_FAR_HALF = new Vec3(-253.5, 71.0, 3.5);
    private static final Vec3 ENTITY_ON_NEAR_HALF = new Vec3(252.0, 71.0, 3.5);
    private static final String ARGUMENT = "argument";

    @Test
    void unboundPoseKeepsItsPlace() {
        assertSame(POSE_NEAR_SEAM, SeamFrame.seatPose(POSE_NEAR_SEAM));
        assertFalse(SeamFrame.isBound());
    }

    @Test
    void entityOnTheFarHalfPullsThePoseOneLapBack() {
        Vector3dc seated = SeamFrame.with(PER_AXIS, (Level) null, () -> ENTITY_ON_FAR_HALF,
                () -> SeamFrame.seatPose(POSE_NEAR_SEAM));
        assertEquals(POSE_ONE_LAP_BACK, seated);
    }

    @Test
    void entityOnTheNearHalfLeavesThePoseAlone() {
        Vector3dc seated = SeamFrame.with(PER_AXIS, (Level) null, () -> ENTITY_ON_NEAR_HALF,
                () -> SeamFrame.seatPose(POSE_NEAR_SEAM));
        assertSame(POSE_NEAR_SEAM, seated);
    }

    @Test
    void theOperationFormBindsTheSubjectsAnchorAndPassesItsArguments() {
        Operation<Object[]> original = args -> new Object[] {args[0], SeamFrame.seatPose(POSE_NEAR_SEAM)};
        Object[] answer = SeamFrame.with(PER_AXIS, (Level) null, ENTITY_ON_FAR_HALF, Function.identity(), original,
                ARGUMENT);
        assertEquals(ARGUMENT, answer[0]);
        assertEquals(POSE_ONE_LAP_BACK, answer[1]);
        assertFalse(SeamFrame.isBound());
    }

    @Test
    void theOperationFormOnAnUnwrappedLevelRunsTheOriginalUnbound() {
        Operation<Boolean> original = args -> SeamFrame.isBound();
        assertFalse(SeamFrame.with((WorldFold) null, (Level) null, ENTITY_ON_FAR_HALF, Function.identity(), original));
    }

    @Test
    void theOperationFormOnAnUnwrappedLevelStillUnbindsAnOuterFrame() {
        Operation<Boolean> original = args -> SeamFrame.isBound();
        SeamFrame.with(PER_AXIS, (Level) null, () -> ENTITY_ON_FAR_HALF, () -> {
            assertFalse(SeamFrame.with((WorldFold) null, (Level) null, ENTITY_ON_FAR_HALF, Function.identity(),
                    original));
            assertTrue(SeamFrame.isBound());
            return null;
        });
    }

    @Test
    void theBindingIsScopedAndRestoresTheOuterOne() {
        SeamFrame.with(PER_AXIS, (Level) null, () -> ENTITY_ON_FAR_HALF, () -> {
            Vector3dc inner = SeamFrame.with(PER_AXIS, (Level) null, () -> ENTITY_ON_NEAR_HALF,
                    () -> SeamFrame.seatPose(POSE_NEAR_SEAM));
            assertSame(POSE_NEAR_SEAM, inner);
            assertEquals(POSE_ONE_LAP_BACK, SeamFrame.seatPose(POSE_NEAR_SEAM));
            return null;
        });
        assertFalse(SeamFrame.isBound());
    }

    @Test
    void aBodyThatThrowsLeavesNoBindingBehind() {
        assertThrows(IllegalStateException.class,
                () -> SeamFrame.with(PER_AXIS, (Level) null, () -> ENTITY_ON_FAR_HALF, () -> {
                    throw new IllegalStateException("the body");
                }));
        assertFalse(SeamFrame.isBound());
    }

    @Test
    void anOriginalThatThrowsLeavesNoBindingBehind() {
        Operation<Void> original = args -> {
            throw new IllegalStateException("the original");
        };
        assertThrows(IllegalStateException.class,
                () -> SeamFrame.with(PER_AXIS, (Level) null, ENTITY_ON_FAR_HALF, Function.identity(), original));
        assertFalse(SeamFrame.isBound());
    }

    @Test
    void aThrowFromTheInnerBodyHandsTheOuterBindingBack() {
        SeamFrame.with(PER_AXIS, (Level) null, () -> ENTITY_ON_FAR_HALF, () -> {
            assertThrows(IllegalStateException.class,
                    () -> SeamFrame.with(PER_AXIS, (Level) null, () -> ENTITY_ON_NEAR_HALF, () -> {
                        throw new IllegalStateException("the inner body");
                    }));
            assertEquals(POSE_ONE_LAP_BACK, SeamFrame.seatPose(POSE_NEAR_SEAM));
            return null;
        });
        assertFalse(SeamFrame.isBound());
    }

    @Test
    void anUnboundScopeReadsThePoseRawAndHandsTheOuterBindingBack() {
        SeamFrame.with(PER_AXIS, (Level) null, () -> ENTITY_ON_FAR_HALF, () -> {
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
        Vector3dc seated = SeamFrame.with((WorldFold) null, (Level) null, () -> ENTITY_ON_FAR_HALF,
                () -> SeamFrame.seatPose(POSE_NEAR_SEAM));
        assertSame(POSE_NEAR_SEAM, seated);
    }

    @Test
    void theSamePoseIsSeatedOnceAndAMovedPoseAnew() {
        SeamFrame.with(PER_AXIS, (Level) null, () -> ENTITY_ON_FAR_HALF, () -> {
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
        SeamFrame.with(PER_AXIS, (Level) null, counting(asked, ENTITY_ON_FAR_HALF), () -> {
            SeamFrame.seatPose(POSE_NEAR_SEAM);
            SeamFrame.seatPose(POSE_FURTHER_IN);
            SeamFrame.seatPose(POSE_NEAR_SEAM);
            return null;
        });
        assertEquals(1, asked.get());
    }

    @Test
    void theOperationFormResolvesItsAnchorOnceAndOnlyWhenAsked() {
        AtomicInteger asked = new AtomicInteger();
        Function<Vec3, Vec3> counted = anchor -> {
            asked.incrementAndGet();
            return anchor;
        };
        SeamFrame.with(PER_AXIS, (Level) null, ENTITY_ON_FAR_HALF, counted, args -> null);
        assertEquals(0, asked.get());

        SeamFrame.with(PER_AXIS, (Level) null, ENTITY_ON_FAR_HALF, counted, args -> {
            SeamFrame.seatPose(POSE_NEAR_SEAM);
            SeamFrame.seatPose(POSE_FURTHER_IN);
            return null;
        });
        assertEquals(1, asked.get());
    }

    @Test
    void aBindingNoPoseAsksAboutNeverResolvesItsAnchor() {
        AtomicInteger asked = new AtomicInteger();
        SeamFrame.with(PER_AXIS, (Level) null, counting(asked, ENTITY_ON_FAR_HALF), () -> null);
        assertEquals(0, asked.get());
    }

    @Test
    void eachNestedBindingResolvesItsOwnAnchorOnce() {
        AtomicInteger outer = new AtomicInteger();
        AtomicInteger inner = new AtomicInteger();
        SeamFrame.with(PER_AXIS, (Level) null, counting(outer, ENTITY_ON_FAR_HALF), () -> {
            SeamFrame.seatPose(POSE_NEAR_SEAM);
            SeamFrame.with(PER_AXIS, (Level) null, counting(inner, ENTITY_ON_NEAR_HALF), () -> {
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

    private static Supplier<Vec3> counting(AtomicInteger asked, Vec3 anchor) {
        return () -> {
            asked.incrementAndGet();
            return anchor;
        };
    }
}

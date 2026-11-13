package com.exoticworlds.compat.create;

import static com.exoticworlds.compat.CompatFoldFixture.DECK_TORUS;
import static com.exoticworlds.compat.CompatFoldFixture.MIRRORED;
import static com.exoticworlds.compat.CompatFoldFixture.PER_AXIS;
import static com.exoticworlds.compat.CompatFoldFixture.SKEWED;
import static com.exoticworlds.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

class CreateSeamFoldTest {
    private static final List<WorldFold> UNSKEWED = List.of(PER_AXIS, DECK_TORUS);
    private static final List<WorldFold> TRANSLATING = List.of(PER_AXIS, DECK_TORUS, SKEWED);
    private static final List<WorldFold> ALL = List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED);

    private static final BlockPos ANCHOR = new BlockPos(250, 64, 10);
    private static final BlockPos TARGET_ACROSS = new BlockPos(-254, 64, 10);
    private static final BlockPos PAST_THE_BOUNDS = new BlockPos(300, 64, 10);

    private static BlockPos rawDelta(BlockPos anchor, BlockPos target) {
        return target.subtract(anchor);
    }

    @Test
    void aDeltaAcrossTheSeamRunsFromTheAnchorToTheNearestCopy() {
        for (WorldFold fold : ALL) {
            BlockPos nearest = fold.nearestCopy(ANCHOR, TARGET_ACROSS);
            BlockPos delta = CreateSeamFold.delta(fold, ANCHOR, TARGET_ACROSS, rawDelta(ANCHOR, TARGET_ACROSS));

            assertEquals(nearest, ANCHOR.offset(delta), "in " + fold);
        }
    }

    @Test
    void theDeltaKeepsTheSignOfTheSubtractionItReplaces() {
        for (WorldFold fold : UNSKEWED) {
            BlockPos delta = CreateSeamFold.delta(fold, ANCHOR, TARGET_ACROSS, rawDelta(ANCHOR, TARGET_ACROSS));

            assertEquals(new BlockPos(WORLD_BLOCKS - 504, 0, 0), delta, "in " + fold);
        }
    }

    @Test
    void aTargetAlreadyNearestGivesTheRawDeltaBackByIdentity() {
        BlockPos target = new BlockPos(240, 64, 10);
        BlockPos raw = rawDelta(ANCHOR, target);
        for (WorldFold fold : ALL) {
            assertSame(raw, CreateSeamFold.delta(fold, ANCHOR, target, raw), "in " + fold);
        }
    }

    @Test
    void anUnwrappedWorldGivesTheRawDeltaBackByIdentity() {
        BlockPos raw = rawDelta(ANCHOR, TARGET_ACROSS);

        assertSame(raw, CreateSeamFold.delta(WorldFolds.NOOP, ANCHOR, TARGET_ACROSS, raw));
        assertSame(raw, CreateSeamFold.delta(null, ANCHOR, TARGET_ACROSS, raw));
        assertSame(raw, CreateSeamFold.foldDelta((Level) null, ANCHOR, TARGET_ACROSS, raw));
    }

    @Test
    void theFarEndDeltaIsTheDeltaTheFarEndStores() {
        for (WorldFold fold : ALL) {
            BlockPos forward = CreateSeamFold.delta(fold, ANCHOR, TARGET_ACROSS, rawDelta(ANCHOR, TARGET_ACROSS));
            BlockPos stored = CreateSeamFold.delta(fold, TARGET_ACROSS, ANCHOR, rawDelta(TARGET_ACROSS, ANCHOR));

            assertEquals(stored, CreateSeamFold.farEndDelta(fold, ANCHOR, forward, forward.multiply(-1)),
                    "in " + fold);
        }
    }

    @Test
    void aNegatedDeltaIsTheFarEndsOnlyWhereTheFoldIsATranslation() {
        for (WorldFold fold : TRANSLATING) {
            BlockPos forward = CreateSeamFold.delta(fold, ANCHOR, TARGET_ACROSS, rawDelta(ANCHOR, TARGET_ACROSS));
            BlockPos stored = CreateSeamFold.delta(fold, TARGET_ACROSS, ANCHOR, rawDelta(TARGET_ACROSS, ANCHOR));

            assertEquals(forward.multiply(-1), stored, "in " + fold);
        }

        BlockPos forward = CreateSeamFold.delta(MIRRORED, ANCHOR, TARGET_ACROSS, rawDelta(ANCHOR, TARGET_ACROSS));
        BlockPos stored = CreateSeamFold.delta(MIRRORED, TARGET_ACROSS, ANCHOR, rawDelta(TARGET_ACROSS, ANCHOR));

        assertNotEquals(forward.multiply(-1), stored);
    }

    @Test
    void aConnectionInsideTheBoundsGivesTheNegationBackByIdentity() {
        BlockPos delta = rawDelta(ANCHOR, new BlockPos(240, 64, 10));
        BlockPos raw = delta.multiply(-1);
        for (WorldFold fold : ALL) {
            assertSame(raw, CreateSeamFold.farEndDelta(fold, ANCHOR, delta, raw), "in " + fold);
        }
    }

    @Test
    void anUnwrappedWorldGivesTheFarEndDeltaBackByIdentity() {
        BlockPos delta = rawDelta(ANCHOR, TARGET_ACROSS);
        BlockPos raw = delta.multiply(-1);

        assertSame(raw, CreateSeamFold.farEndDelta(WorldFolds.NOOP, ANCHOR, delta, raw));
        assertSame(raw, CreateSeamFold.farEndDelta((Level) null, ANCHOR, delta, raw));
    }

    @Test
    void anUnwrappedWorldGivesTheCanonicalPositionBackByIdentity() {
        assertSame(PAST_THE_BOUNDS, CreateSeamFold.canonical((ServerLevel) null, PAST_THE_BOUNDS));
    }

    @Test
    void anUnwrappedWorldLeavesTheNearestCopyUntouched() {
        assertSame(TARGET_ACROSS, CreateSeamFold.nearest(WorldFolds.NOOP, ANCHOR, TARGET_ACROSS));
        assertSame(TARGET_ACROSS, CreateSeamFold.nearest(null, ANCHOR, TARGET_ACROSS));
        assertSame(TARGET_ACROSS, CreateSeamFold.nearestCopy((Level) null, ANCHOR, TARGET_ACROSS));
    }
}

package com.exoticworlds.compat.c2me;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

import com.ishland.c2me.base.common.scheduler.LockTokenImpl;
import com.ishland.flowsched.executor.LockToken;

class C2meSeamFoldTest {
    private static final int OWNER = 7;
    private static final int OTHER_OWNER = 8;

    private static final long SEAM_KEY = 42L;
    private static final long INNER_KEY = 43L;

    @Test
    void aFoldedRepeatKeepsOnlyItsFirstToken() {
        LockToken first = worldgen(OWNER, SEAM_KEY);
        LockToken inner = worldgen(OWNER, INNER_KEY);

        assertArrayEquals(
                new LockToken[] {first, inner},
                C2meSeamFold.withoutRepeats(new LockToken[] {first, inner, worldgen(OWNER, SEAM_KEY)}),
                "a chunk folded onto one already locked has to be locked once, in its first place");
    }

    @Test
    void tokensThatDifferByOwnerOrUsageAllStay() {
        LockToken[] tokens = {
                worldgen(OWNER, SEAM_KEY),
                worldgen(OTHER_OWNER, SEAM_KEY),
                new LockTokenImpl(OWNER, SEAM_KEY, LockTokenImpl.Usage.LIGHTING)};

        assertArrayEquals(tokens, C2meSeamFold.withoutRepeats(tokens),
                "one position under another owner or usage is a different lock");
    }

    private static LockToken worldgen(int ownerTag, long chunkKey) {
        return new LockTokenImpl(ownerTag, chunkKey, LockTokenImpl.Usage.WORLDGEN);
    }
}

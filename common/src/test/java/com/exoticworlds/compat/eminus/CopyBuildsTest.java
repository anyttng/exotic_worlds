package com.exoticworlds.compat.eminus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class CopyBuildsTest {
    private static final long STORED = 7L;
    private static final long OTHER_STORED = 8L;
    private static final float FAR = 1.0F;
    private static final float NEAR = 9.0F;

    private final CopyBuilds<String> copies = new CopyBuilds<>();

    @Test
    void aSecondCopyBeforeTheTaskIsTakenJoinsItAndIsDeliveredWithIt() {
        assertEquals(CopyBuilds.Offer.QUEUE, copies.offer(STORED, member(100L, 1L, NEAR)));
        assertEquals(CopyBuilds.Offer.JOINED, copies.offer(STORED, member(612L, 2L, FAR)));

        copies.taken(1L);
        assertEquals(List.of(612L), keys(copies.delivered(1L)));
        assertTrue(copies.delivered(1L).isEmpty(), "a group is delivered once");
    }

    @Test
    void aCopyAfterTheTaskIsTakenQueuesATaskOfItsOwn() {
        copies.offer(STORED, member(100L, 1L, NEAR));
        copies.taken(1L);

        assertEquals(CopyBuilds.Offer.QUEUE, copies.offer(STORED, member(612L, 2L, FAR)));
        assertTrue(copies.delivered(1L).isEmpty(), "the copy asked after the build read its cell never rides it");
        assertTrue(copies.delivered(2L).isEmpty());
    }

    @Test
    void aNearerCopyTakesTheLeadAndTheFartherTaskIsSkipped() {
        copies.offer(STORED, member(612L, 1L, FAR));
        assertEquals(CopyBuilds.Offer.QUEUE, copies.offer(STORED, member(100L, 2L, NEAR)));

        assertTrue(copies.skip(1L), "the farther task waits behind no one");
        assertFalse(copies.skip(2L));
        copies.taken(2L);
        assertEquals(List.of(612L), keys(copies.delivered(2L)));
    }

    @Test
    void copiesOfAnotherStoredCellNeverJoin() {
        copies.offer(STORED, member(100L, 1L, NEAR));

        assertEquals(CopyBuilds.Offer.QUEUE, copies.offer(OTHER_STORED, member(200L, 2L, FAR)));
    }

    @Test
    void aFailedGroupHandsItsFollowersBackOnce() {
        copies.offer(STORED, member(100L, 1L, NEAR));
        copies.offer(STORED, member(612L, 2L, FAR));

        assertEquals(List.of(612L), keys(copies.delivered(1L)));
        assertEquals(CopyBuilds.Offer.QUEUE, copies.offer(STORED, member(612L, 3L, FAR)));
    }

    private static CopyBuilds.Member<String> member(long key, long request, float priority) {
        return new CopyBuilds.Member<>(key, request, priority, null, 0);
    }

    private static List<Long> keys(List<CopyBuilds.Member<String>> members) {
        return members.stream().map(CopyBuilds.Member::key).toList();
    }
}

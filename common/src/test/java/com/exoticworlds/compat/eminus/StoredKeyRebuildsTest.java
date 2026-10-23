package com.exoticworlds.compat.eminus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class StoredKeyRebuildsTest {
    private static final String FIRST = "first";
    private static final String COPY = "copy";
    private static final String GAVE_WAY = "gave way";

    private final List<String> changed = new ArrayList<>();

    @Test
    void theSecondCopyOfAStoredKeyTakesTheChangeAndTheFirstIsLeftToEminus() {
        assertFalse(StoredKeyRebuilds.changeAllButFirst(List.of(FIRST, COPY), this::change));

        assertEquals(List.of(COPY), changed);
    }

    @Test
    void aCopyThatGaveWayMakesTheWalkDue() {
        assertTrue(StoredKeyRebuilds.changeAllButFirst(List.of(FIRST, GAVE_WAY), this::change));
    }

    @Test
    void everyCopyTakesTheChangeAfterOneMadeTheWalkDue() {
        assertTrue(StoredKeyRebuilds.changeAllButFirst(List.of(FIRST, GAVE_WAY, COPY), this::change));

        assertEquals(List.of(GAVE_WAY, COPY), changed);
    }

    @Test
    void aStoredKeyWithOneNodeChangesNoCopy() {
        assertFalse(StoredKeyRebuilds.changeAllButFirst(List.of(FIRST), this::change));

        assertTrue(changed.isEmpty());
    }

    private boolean change(String node) {
        changed.add(node);
        return node.equals(GAVE_WAY);
    }
}

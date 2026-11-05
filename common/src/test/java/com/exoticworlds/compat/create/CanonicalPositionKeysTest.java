package com.exoticworlds.compat.create;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class CanonicalPositionKeysTest {
    @Test
    void aReaderWithoutAFoldKeepsTheSetItWasHanded() {
        Set<BlockPos> built = new HashSet<>();

        assertSame(built, CanonicalPositionKeys.set(null, built));
    }

    @Test
    void aReaderWithoutAFoldKeepsTheMapItWasHanded() {
        Map<BlockPos, Object> built = new HashMap<>();

        assertSame(built, CanonicalPositionKeys.map(null, built));
    }
}

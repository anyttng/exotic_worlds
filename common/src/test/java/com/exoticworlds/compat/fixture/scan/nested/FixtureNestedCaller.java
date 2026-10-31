package com.exoticworlds.compat.fixture.scan.nested;

import com.exoticworlds.compat.fixture.mod.FixtureModLater;

public final class FixtureNestedCaller {
    public static Object create() {
        return new FixtureModLater();
    }

    private FixtureNestedCaller() {
    }
}

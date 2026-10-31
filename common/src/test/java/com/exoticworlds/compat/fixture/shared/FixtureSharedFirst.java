package com.exoticworlds.compat.fixture.shared;

import com.exoticworlds.compat.fixture.mod.FixtureModLater;

public final class FixtureSharedFirst {
    public static Object create() {
        return new FixtureModLater();
    }

    private FixtureSharedFirst() {
    }
}

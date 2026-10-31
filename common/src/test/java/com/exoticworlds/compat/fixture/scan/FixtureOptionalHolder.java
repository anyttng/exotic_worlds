package com.exoticworlds.compat.fixture.scan;

import com.exoticworlds.compat.fixture.mod.FixtureModLater;

public final class FixtureOptionalHolder {
    public static Object create() {
        return new FixtureModLater();
    }

    private FixtureOptionalHolder() {
    }
}

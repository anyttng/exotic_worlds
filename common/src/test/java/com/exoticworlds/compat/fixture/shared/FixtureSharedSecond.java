package com.exoticworlds.compat.fixture.shared;

import com.exoticworlds.compat.fixture.mod.FixtureModApi;

public final class FixtureSharedSecond {
    public static int read(FixtureModApi api) {
        return api.answer();
    }

    private FixtureSharedSecond() {
    }
}

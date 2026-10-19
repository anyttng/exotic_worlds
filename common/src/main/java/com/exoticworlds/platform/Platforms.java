package com.exoticworlds.platform;

import org.jspecify.annotations.Nullable;

public final class Platforms {
    private static @Nullable Platform platform;

    public static void set(Platform chosen) {
        platform = chosen;
    }

    public static Platform get() {
        if (platform == null) {
            throw new IllegalStateException("Platform is not set — the loader entrypoint must call Platforms.set first.");
        }

        return platform;
    }

    private Platforms() {
    }
}

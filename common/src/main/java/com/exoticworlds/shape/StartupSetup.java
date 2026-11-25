package com.exoticworlds.shape;

import com.exoticworlds.platform.Platforms;

public final class StartupSetup {

    public static void registerAll() {
        ShapeSetup.registerAll(Platforms.get().isClient());
    }

    private StartupSetup() {
    }
}

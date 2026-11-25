package com.exoticworlds.shape;

public final class StartupSetup {

    public static void registerAll() {
        WorldOptionSetup.registerAll();
        WorldShapeSetup.registerAll();
    }

    private StartupSetup() {
    }
}

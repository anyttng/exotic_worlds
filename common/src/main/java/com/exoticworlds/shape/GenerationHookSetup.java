package com.exoticworlds.shape;

import com.exoticworlds.shape.noise.FieldNoiseShaping;

public final class GenerationHookSetup {

    public static void registerAll() {
        FieldNoiseShaping.register();
    }

    private GenerationHookSetup() {
    }
}

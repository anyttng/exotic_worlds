package com.exoticworlds.core;

public final class NoiseShapings {
    private static final String KEY = "noise_shaping";

    private static final StartupRegistry<String, NoiseShaping> SHAPING = new StartupRegistry<>("Noise shaping");

    public static void register(NoiseShaping shaping) {
        SHAPING.register(KEY, shaping);
    }

    public static NoiseShaping get() {
        NoiseShaping shaping = SHAPING.entries().get(KEY);
        if (shaping == null) {
            throw new IllegalStateException("No noise shaping is registered: a folded noise compiled before "
                    + "GenerationHookSetup.registerAll ran.");
        }

        return shaping;
    }

    private NoiseShapings() {
    }
}

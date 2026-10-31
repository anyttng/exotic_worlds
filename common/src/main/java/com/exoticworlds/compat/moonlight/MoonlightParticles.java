package com.exoticworlds.compat.moonlight;

public final class MoonlightParticles {
    public static void register() {
        if (MoonlightMixinPlugin.MOONLIGHT.admitted()) {
            ParticleMessageRewriter.register();
        }
    }

    private MoonlightParticles() {
    }
}

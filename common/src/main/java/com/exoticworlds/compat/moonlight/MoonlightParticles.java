package com.exoticworlds.compat.moonlight;

import com.exoticworlds.compat.ModPresence;

public final class MoonlightParticles {
    private static final String PARTICLE_MESSAGE_CLASS =
            "net/mehvahdjukaar/moonlight/core/network/ClientBoundParticleAroundBlockMessage.class";

    public static void register() {
        if (ModPresence.probe(PARTICLE_MESSAGE_CLASS)) {
            ParticleMessageRewriter.register();
        }
    }

    private MoonlightParticles() {
    }
}

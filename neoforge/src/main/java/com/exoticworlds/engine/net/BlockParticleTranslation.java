package com.exoticworlds.engine.net;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;

public final class BlockParticleTranslation {
    public static void register() {
        PacketTranslator.registerParticleRewriter(BlockParticleOption.class, (particle, context, clientOrigin) -> {
            BlockPos serverPos = particle.getPos();
            if (serverPos == null) {
                return particle;
            }

            return new BlockParticleOption(particle.getType(), particle.getState(),
                    context.toClient(serverPos));
        });
    }

    private BlockParticleTranslation() {
    }
}

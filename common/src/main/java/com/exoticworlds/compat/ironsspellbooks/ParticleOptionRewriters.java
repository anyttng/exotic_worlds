package com.exoticworlds.compat.ironsspellbooks;

import org.joml.Vector3f;

import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import io.redspace.ironsspellbooks.particle.SoulfireRayParticleOptions;
import io.redspace.ironsspellbooks.particle.TraceParticleOptions;
import io.redspace.ironsspellbooks.particle.ZapParticleOption;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

final class ParticleOptionRewriters {
    static void register() {
        PacketTranslator.registerParticleRewriter(ZapParticleOption.class,
                (particle, context, clientOrigin) -> zap(particle, (TranslationContext) context, clientOrigin));
        PacketTranslator.registerParticleRewriter(SoulfireRayParticleOptions.class,
                (particle, context, clientOrigin) -> soulfireRay(particle, (TranslationContext) context,
                        clientOrigin));
        PacketTranslator.registerParticleRewriter(TraceParticleOptions.class,
                (particle, context, clientOrigin) -> trace(particle, (TranslationContext) context, clientOrigin));
    }

    static ParticleOptions zap(ZapParticleOption particle, TranslationContext context, Vec3 clientOrigin) {
        Vec3 destination = particle.getDestination();
        Vec3 seated = NearestCopy.toward(context.fold(), clientOrigin, destination);
        return seated.equals(destination) ? particle : new ZapParticleOption(seated);
    }

    static ParticleOptions soulfireRay(SoulfireRayParticleOptions particle, TranslationContext context,
            Vec3 clientOrigin) {
        Vec3 destination = particle.getDestination();
        Vec3 seated = NearestCopy.toward(context.fold(), clientOrigin, destination);
        return seated.equals(destination) ? particle : new SoulfireRayParticleOptions(seated);
    }

    static ParticleOptions trace(TraceParticleOptions particle, TranslationContext context, Vec3 clientOrigin) {
        Vec3 destination = new Vec3(particle.destination);
        Vec3 seated = NearestCopy.toward(context.fold(), clientOrigin, destination);
        return seated.equals(destination)
                ? particle
                : new TraceParticleOptions(seated.toVector3f(), new Vector3f(particle.color));
    }

    private ParticleOptionRewriters() {
    }
}

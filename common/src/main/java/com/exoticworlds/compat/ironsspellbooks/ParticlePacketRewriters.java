package com.exoticworlds.compat.ironsspellbooks;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.exoticworlds.compat.ironsspellbooks.mixin.AbsorptionParticlesPacketAccessor;
import com.exoticworlds.compat.ironsspellbooks.mixin.BloodSiphonParticlesPacketAccessor;
import com.exoticworlds.compat.ironsspellbooks.mixin.FieryExplosionParticlesPacketAccessor;
import com.exoticworlds.compat.ironsspellbooks.mixin.FortifyAreaParticlesPacketAccessor;
import com.exoticworlds.compat.ironsspellbooks.mixin.FrostStepParticlesPacketAccessor;
import com.exoticworlds.compat.ironsspellbooks.mixin.HealParticlesPacketAccessor;
import com.exoticworlds.compat.ironsspellbooks.mixin.OakskinParticlesPacketAccessor;
import com.exoticworlds.compat.ironsspellbooks.mixin.RegenCloudParticlesPacketAccessor;
import com.exoticworlds.compat.ironsspellbooks.mixin.ShockwaveParticlesPacketAccessor;
import com.exoticworlds.compat.ironsspellbooks.mixin.TeleportParticlesPacketAccessor;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import io.redspace.ironsspellbooks.network.particles.AbsorptionParticlesPacket;
import io.redspace.ironsspellbooks.network.particles.BloodSiphonParticlesPacket;
import io.redspace.ironsspellbooks.network.particles.FieryExplosionParticlesPacket;
import io.redspace.ironsspellbooks.network.particles.FortifyAreaParticlesPacket;
import io.redspace.ironsspellbooks.network.particles.FrostStepParticlesPacket;
import io.redspace.ironsspellbooks.network.particles.HealParticlesPacket;
import io.redspace.ironsspellbooks.network.particles.OakskinParticlesPacket;
import io.redspace.ironsspellbooks.network.particles.RegenCloudParticlesPacket;
import io.redspace.ironsspellbooks.network.particles.ShockwaveParticlesPacket;
import io.redspace.ironsspellbooks.network.particles.TeleportParticlesPacket;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

final class ParticlePacketRewriters {
    static void register() {
        registerAtOnePosition(AbsorptionParticlesPacket.class,
                packet -> ((AbsorptionParticlesPacketAccessor) packet).exotic_worlds$getPos(),
                AbsorptionParticlesPacket::new);
        registerAtOnePosition(FortifyAreaParticlesPacket.class,
                packet -> ((FortifyAreaParticlesPacketAccessor) packet).exotic_worlds$getPos(),
                FortifyAreaParticlesPacket::new);
        registerAtOnePosition(HealParticlesPacket.class,
                packet -> ((HealParticlesPacketAccessor) packet).exotic_worlds$getPos(), HealParticlesPacket::new);
        registerAtOnePosition(OakskinParticlesPacket.class,
                packet -> ((OakskinParticlesPacketAccessor) packet).exotic_worlds$getPos(),
                OakskinParticlesPacket::new);
        registerAtOnePosition(RegenCloudParticlesPacket.class,
                packet -> ((RegenCloudParticlesPacketAccessor) packet).exotic_worlds$getPos(),
                RegenCloudParticlesPacket::new);
        PacketTranslator.registerClientboundPayloadRewriter(FieryExplosionParticlesPacket.class,
                ParticlePacketRewriters::fieryExplosion);
        PacketTranslator.registerClientboundPayloadRewriter(ShockwaveParticlesPacket.class,
                ParticlePacketRewriters::shockwave);
        PacketTranslator.registerClientboundPayloadRewriter(TeleportParticlesPacket.class,
                ParticlePacketRewriters::teleport);
        PacketTranslator.registerClientboundPayloadRewriter(FrostStepParticlesPacket.class,
                ParticlePacketRewriters::frostStep);
        PacketTranslator.registerClientboundPayloadRewriter(BloodSiphonParticlesPacket.class,
                ParticlePacketRewriters::bloodSiphon);
    }

    private static <P extends CustomPacketPayload> void registerAtOnePosition(Class<P> payloadType,
            Function<P, Vec3> position, Function<Vec3, P> rebuild) {
        PacketTranslator.registerClientboundPayloadRewriter(payloadType, (packet, context) -> {
            Vec3 raw = position.apply(packet);
            Vec3 seated = context.nearestCopy(raw);
            return seated.equals(raw) ? packet : rebuild.apply(seated);
        });
    }

    static CustomPacketPayload fieryExplosion(FieryExplosionParticlesPacket packet, TranslationContext context) {
        FieryExplosionParticlesPacketAccessor explosion = (FieryExplosionParticlesPacketAccessor) packet;
        Vec3 raw = explosion.exotic_worlds$getPos1();
        Vec3 seated = context.nearestCopy(raw);
        return seated.equals(raw)
                ? packet
                : new FieryExplosionParticlesPacket(seated, explosion.exotic_worlds$getRadius());
    }

    static CustomPacketPayload shockwave(ShockwaveParticlesPacket packet, TranslationContext context) {
        ShockwaveParticlesPacketAccessor shockwave = (ShockwaveParticlesPacketAccessor) packet;
        Vec3 raw = shockwave.exotic_worlds$getPos();
        Vec3 seated = context.nearestCopy(raw);
        ParticleType<?> type =
                BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(shockwave.exotic_worlds$getParticleName()));
        return seated.equals(raw) || type == null
                ? packet
                : new ShockwaveParticlesPacket(seated, shockwave.exotic_worlds$getRadius(), type);
    }

    static CustomPacketPayload teleport(TeleportParticlesPacket packet, TranslationContext context) {
        TeleportParticlesPacketAccessor teleport = (TeleportParticlesPacketAccessor) packet;
        return seatEach(packet, teleport.exotic_worlds$getPos1(), teleport.exotic_worlds$getPos2(), context,
                TeleportParticlesPacket::new);
    }

    static CustomPacketPayload frostStep(FrostStepParticlesPacket packet, TranslationContext context) {
        FrostStepParticlesPacketAccessor frostStep = (FrostStepParticlesPacketAccessor) packet;
        return seatEach(packet, frostStep.exotic_worlds$getPos1(), frostStep.exotic_worlds$getPos2(), context,
                FrostStepParticlesPacket::new);
    }

    static CustomPacketPayload bloodSiphon(BloodSiphonParticlesPacket packet, TranslationContext context) {
        BloodSiphonParticlesPacketAccessor siphon = (BloodSiphonParticlesPacketAccessor) packet;
        Vec3 target = siphon.exotic_worlds$getPos1();
        Vec3 caster = siphon.exotic_worlds$getPos2();
        Vec3 seatedTarget = context.nearestCopy(target);
        Vec3 seatedCaster = NearestCopy.toward(context.fold(), seatedTarget, caster);
        return seatedTarget.equals(target) && seatedCaster.equals(caster)
                ? packet
                : new BloodSiphonParticlesPacket(seatedTarget, seatedCaster);
    }

    private static <P extends CustomPacketPayload> CustomPacketPayload seatEach(P packet, Vec3 first, Vec3 second,
            TranslationContext context, BiFunction<Vec3, Vec3, P> rebuild) {
        Vec3 seatedFirst = context.nearestCopy(first);
        Vec3 seatedSecond = context.nearestCopy(second);
        return seatedFirst.equals(first) && seatedSecond.equals(second)
                ? packet
                : rebuild.apply(seatedFirst, seatedSecond);
    }

    private ParticlePacketRewriters() {
    }
}

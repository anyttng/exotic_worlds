package com.exoticworlds.compat.apprenticecodex;

import java.util.function.Function;

import com.exoticworlds.compat.apprenticecodex.mixin.ForceFieldDefenseEffectPacketAccessor;
import com.exoticworlds.compat.apprenticecodex.mixin.HealingBloomPulsePacketAccessor;
import com.exoticworlds.compat.apprenticecodex.mixin.HeavenlyFistPulsePacketAccessor;
import com.exoticworlds.compat.apprenticecodex.mixin.ManaSiphonOrbEffectPacketAccessor;
import com.exoticworlds.compat.apprenticecodex.mixin.TerraResonancePulsePacketAccessor;
import com.exoticworlds.compat.apprenticecodex.mixin.TotemOfPermafrostPulsePacketAccessor;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import jp.aquafactory.apprenticecodex.network.packet.ForceFieldDefenseEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.HealingBloomPulsePacket;
import jp.aquafactory.apprenticecodex.network.packet.HeavenlyFistPulsePacket;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomImpulseEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.ManaManeuverGearFallEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.ManaSiphonOrbEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.TerraResonancePulsePacket;
import jp.aquafactory.apprenticecodex.network.packet.TotemOfPermafrostPulsePacket;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

final class EffectPacketRewriters {
    static void register() {
        PacketTranslator.registerClientboundPayloadRewriter(HealingBloomPulsePacket.class,
                EffectPacketRewriters::healingBloom);
        PacketTranslator.registerClientboundPayloadRewriter(HeavenlyFistPulsePacket.class,
                EffectPacketRewriters::heavenlyFist);
        PacketTranslator.registerClientboundPayloadRewriter(TotemOfPermafrostPulsePacket.class,
                EffectPacketRewriters::totemOfPermafrost);
        PacketTranslator.registerClientboundPayloadRewriter(TerraResonancePulsePacket.class,
                EffectPacketRewriters::terraResonance);
        PacketTranslator.registerClientboundPayloadRewriter(ForceFieldDefenseEffectPacket.class,
                EffectPacketRewriters::forceField);
        PacketTranslator.registerClientboundPayloadRewriter(ManaSiphonOrbEffectPacket.class,
                EffectPacketRewriters::manaSiphon);
        PacketTranslator.registerClientboundPayloadRewriter(HoverrideBroomImpulseEffectPacket.class,
                EffectPacketRewriters::hoverrideBroom);
        PacketTranslator.registerClientboundPayloadRewriter(ManaManeuverGearFallEffectPacket.class,
                EffectPacketRewriters::manaManeuverGear);
    }

    static CustomPacketPayload healingBloom(HealingBloomPulsePacket packet, TranslationContext context) {
        HealingBloomPulsePacketAccessor pulse = (HealingBloomPulsePacketAccessor) packet;
        return seat(packet, new Vec3(pulse.exotic_worlds$getX(), pulse.exotic_worlds$getY(), pulse.exotic_worlds$getZ()),
                context, seated -> new HealingBloomPulsePacket(seated, pulse.exotic_worlds$getMaxRadius()));
    }

    static CustomPacketPayload heavenlyFist(HeavenlyFistPulsePacket packet, TranslationContext context) {
        HeavenlyFistPulsePacketAccessor pulse = (HeavenlyFistPulsePacketAccessor) packet;
        return seat(packet, new Vec3(pulse.exotic_worlds$getX(), pulse.exotic_worlds$getY(), pulse.exotic_worlds$getZ()),
                context, seated -> new HeavenlyFistPulsePacket(seated, pulse.exotic_worlds$getMaxRadius()));
    }

    static CustomPacketPayload totemOfPermafrost(TotemOfPermafrostPulsePacket packet, TranslationContext context) {
        TotemOfPermafrostPulsePacketAccessor pulse = (TotemOfPermafrostPulsePacketAccessor) packet;
        return seat(packet, new Vec3(pulse.exotic_worlds$getX(), pulse.exotic_worlds$getY(), pulse.exotic_worlds$getZ()),
                context, seated -> new TotemOfPermafrostPulsePacket(seated, pulse.exotic_worlds$getMaxRadius()));
    }

    static CustomPacketPayload terraResonance(TerraResonancePulsePacket packet, TranslationContext context) {
        TerraResonancePulsePacketAccessor pulse = (TerraResonancePulsePacketAccessor) packet;
        return seat(packet, new Vec3(pulse.exotic_worlds$getX(), pulse.exotic_worlds$getY(), pulse.exotic_worlds$getZ()),
                context, seated -> new TerraResonancePulsePacket(seated, pulse.exotic_worlds$getSelectedFace(),
                        pulse.exotic_worlds$getMaxRadius()));
    }

    static CustomPacketPayload forceField(ForceFieldDefenseEffectPacket packet, TranslationContext context) {
        ForceFieldDefenseEffectPacketAccessor effect = (ForceFieldDefenseEffectPacketAccessor) packet;
        return seat(packet,
                new Vec3(effect.exotic_worlds$getX(), effect.exotic_worlds$getY(), effect.exotic_worlds$getZ()),
                context, seated -> new ForceFieldDefenseEffectPacket(seated,
                        new Vec3(effect.exotic_worlds$getNormalX(), effect.exotic_worlds$getNormalY(),
                                effect.exotic_worlds$getNormalZ()),
                        effect.exotic_worlds$getSizeScale(), effect.exotic_worlds$getLifetimeScale(),
                        effect.exotic_worlds$shouldRenderWave(), effect.exotic_worlds$isFailed(),
                        effect.exotic_worlds$isAbsorbing()));
    }

    static CustomPacketPayload manaSiphon(ManaSiphonOrbEffectPacket packet, TranslationContext context) {
        ManaSiphonOrbEffectPacketAccessor effect = (ManaSiphonOrbEffectPacketAccessor) packet;
        return seat(packet,
                new Vec3(effect.exotic_worlds$getImpactX(), effect.exotic_worlds$getImpactY(),
                        effect.exotic_worlds$getImpactZ()),
                context, seated -> new ManaSiphonOrbEffectPacket(seated, effect.exotic_worlds$getOwnerEntityId(),
                        effect.exotic_worlds$getOrbs()));
    }

    static CustomPacketPayload hoverrideBroom(HoverrideBroomImpulseEffectPacket packet, TranslationContext context) {
        return seat(packet, packet.center(), context,
                seated -> new HoverrideBroomImpulseEffectPacket(seated, packet.direction()));
    }

    static CustomPacketPayload manaManeuverGear(ManaManeuverGearFallEffectPacket packet, TranslationContext context) {
        return seat(packet, new Vec3(packet.x(), packet.y(), packet.z()), context,
                seated -> new ManaManeuverGearFallEffectPacket(seated, packet.maxRadius()));
    }

    private static <P extends CustomPacketPayload> CustomPacketPayload seat(P packet, Vec3 raw,
            TranslationContext context, Function<Vec3, P> rebuild) {
        Vec3 seated = context.nearestCopy(raw);
        return seated.equals(raw) ? packet : rebuild.apply(seated);
    }

    private EffectPacketRewriters() {
    }
}

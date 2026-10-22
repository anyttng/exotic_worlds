package com.exoticworlds.compat.arsnouveau;

import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.compat.arsnouveau.mixin.PacketANEffectAccessor;
import com.exoticworlds.compat.arsnouveau.mixin.PacketAddFadingLightAccessor;
import com.exoticworlds.compat.arsnouveau.mixin.PacketTimedEventAccessor;
import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;
import com.hollingsworth.arsnouveau.api.event.ChimeraSummonEvent;
import com.hollingsworth.arsnouveau.api.registry.ParticleColorRegistry;
import com.hollingsworth.arsnouveau.api.util.NBTUtil;
import com.hollingsworth.arsnouveau.client.particle.ColorPos;
import com.hollingsworth.arsnouveau.common.network.HighlightAreaPacket;
import com.hollingsworth.arsnouveau.common.network.PacketANEffect;
import com.hollingsworth.arsnouveau.common.network.PacketAddFadingLight;
import com.hollingsworth.arsnouveau.common.network.PacketClientDelayEffect;
import com.hollingsworth.arsnouveau.common.network.PacketTimedEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class EffectPacketRewriters {
    private static final String EVENT_ID_KEY = "id";
    private static final String CHIMERA_POSITION_KEY = "loc";

    static void register() {
        PacketTranslator.registerClientboundPayloadRewriter(PacketANEffect.class, EffectPacketRewriters::effect);
        PacketTranslator.registerClientboundPayloadRewriter(PacketAddFadingLight.class,
                EffectPacketRewriters::fadingLight);
        PacketTranslator.registerClientboundPayloadRewriter(PacketClientDelayEffect.class,
                EffectPacketRewriters::delayedSpell);
        PacketTranslator.registerClientboundPayloadRewriter(HighlightAreaPacket.class, EffectPacketRewriters::highlight);
        PacketTranslator.registerClientboundPayloadRewriter(PacketTimedEvent.class, EffectPacketRewriters::timedEvent);
    }

    static CustomPacketPayload effect(PacketANEffect packet, TranslationContext context) {
        PacketANEffectAccessor effect = (PacketANEffectAccessor) packet;
        Vec3 position = new Vec3(effect.exotic_worlds$x(), effect.exotic_worlds$y(), effect.exotic_worlds$z());
        Vec3 seated = context.nearestCopy(position);
        return seated.equals(position)
                ? packet
                : new PacketANEffect(effect.exotic_worlds$type(), seated.x, seated.y, seated.z,
                        ParticleColorRegistry.from(effect.exotic_worlds$particleNbt()), effect.exotic_worlds$args());
    }

    static CustomPacketPayload fadingLight(PacketAddFadingLight packet, TranslationContext context) {
        PacketAddFadingLightAccessor light = (PacketAddFadingLightAccessor) packet;
        Vec3 position = new Vec3(light.exotic_worlds$x(), light.exotic_worlds$y(), light.exotic_worlds$z());
        Vec3 seated = context.nearestCopy(position);
        return seated.equals(position) ? packet : new PacketAddFadingLight(seated.x, seated.y, seated.z);
    }

    static CustomPacketPayload delayedSpell(PacketClientDelayEffect packet, TranslationContext context) {
        BlockHitResult hit = packet.hitPos;
        if (hit == null) {
            return packet;
        }

        BlockPos seated = context.toClient(hit.getBlockPos());
        if (seated.equals(hit.getBlockPos())) {
            return packet;
        }

        DeckTransformation move = context.transformer().nearestCopyTransformation(seated, hit.getBlockPos());
        BlockHitResult seatedHit = new BlockHitResult(move.apply(hit.getLocation()), hit.getDirection(), seated,
                hit.isInside());
        PacketClientDelayEffect copy =
                new PacketClientDelayEffect(packet.duration, null, packet.spell, null, seatedHit, null);
        copy.shooterID = packet.shooterID;
        copy.hitEntityID = packet.hitEntityID;
        return copy;
    }

    static CustomPacketPayload highlight(HighlightAreaPacket packet, TranslationContext context) {
        List<ColorPos> seated = new ArrayList<>(packet.colorPos.size());
        boolean moved = false;
        for (ColorPos colorPos : packet.colorPos) {
            Vec3 seatedPos = context.nearestCopy(colorPos.pos);
            moved |= !seatedPos.equals(colorPos.pos);
            seated.add(new ColorPos(seatedPos, colorPos.color));
        }

        return moved ? new HighlightAreaPacket(seated, packet.ticks) : packet;
    }

    static CustomPacketPayload timedEvent(PacketTimedEvent packet, TranslationContext context) {
        CompoundTag tag = ((PacketTimedEventAccessor) packet).exotic_worlds$tag();
        if (!ChimeraSummonEvent.ID.equals(tag.getString(EVENT_ID_KEY))) {
            return packet;
        }

        BlockPos position = NBTUtil.getBlockPos(tag, CHIMERA_POSITION_KEY);
        BlockPos seated = context.nearestCopy(position);
        if (seated.equals(position)) {
            return packet;
        }

        return new PacketTimedEvent(NBTUtil.storeBlockPos(tag.copy(), CHIMERA_POSITION_KEY, seated));
    }

    private EffectPacketRewriters() {
    }
}

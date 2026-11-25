package com.exoticworlds.compat.apprenticecodex;

import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.compat.apprenticecodex.mixin.DeepSensorObservationsPacketAccessor;
import com.exoticworlds.compat.apprenticecodex.mixin.SenseEvilHighlightsPacketAccessor;
import com.exoticworlds.compat.apprenticecodex.mixin.TerraResonanceHighlightsPacketAccessor;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import jp.aquafactory.apprenticecodex.network.packet.DeepSensorObservationsPacket;
import jp.aquafactory.apprenticecodex.network.packet.SenseEvilHighlightsPacket;
import jp.aquafactory.apprenticecodex.network.packet.TerraResonanceHighlightsPacket;
import jp.aquafactory.apprenticecodex.spell.deepsensor.DeepSensorObservationBuffer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

final class HighlightPacketRewriters {
    static void register() {
        PacketTranslator.registerClientboundPayloadRewriter(DeepSensorObservationsPacket.class,
                HighlightPacketRewriters::deepSensor);
        PacketTranslator.registerClientboundPayloadRewriter(SenseEvilHighlightsPacket.class,
                HighlightPacketRewriters::senseEvil);
        PacketTranslator.registerClientboundPayloadRewriter(TerraResonanceHighlightsPacket.class,
                HighlightPacketRewriters::terraResonance);
    }

    static CustomPacketPayload deepSensor(DeepSensorObservationsPacket packet, TranslationContext context) {
        DeepSensorObservationsPacketAccessor sensor = (DeepSensorObservationsPacketAccessor) (Object) packet;
        List<DeepSensorObservationsPacket.Observation> observations = sensor.exotic_worlds$getObservations();
        List<DeepSensorObservationBuffer.DisplayObservation> seated = new ArrayList<>(observations.size());
        boolean isMoved = false;
        for (DeepSensorObservationsPacket.Observation observation : observations) {
            BlockPos position = context.nearestCopy(observation.position());
            isMoved |= !position.equals(observation.position());
            seated.add(new DeepSensorObservationBuffer.DisplayObservation(position, observation.distance(),
                    observation.expiresAtGameTime()));
        }

        return isMoved ? new DeepSensorObservationsPacket(sensor.exotic_worlds$getDimension(), seated) : packet;
    }

    static CustomPacketPayload senseEvil(SenseEvilHighlightsPacket packet, TranslationContext context) {
        List<SenseEvilHighlightsPacket.TargetData> targets =
                ((SenseEvilHighlightsPacketAccessor) packet).exotic_worlds$getTargets();
        List<SenseEvilHighlightsPacket.TargetData> seated = new ArrayList<>(targets.size());
        boolean isMoved = false;
        for (SenseEvilHighlightsPacket.TargetData target : targets) {
            Vec3 position = context.nearestCopy(target.position());
            isMoved |= !position.equals(target.position());
            seated.add(new SenseEvilHighlightsPacket.TargetData(position, target.scale(), target.variant()));
        }

        return isMoved ? new SenseEvilHighlightsPacket(seated) : packet;
    }

    static CustomPacketPayload terraResonance(TerraResonanceHighlightsPacket packet, TranslationContext context) {
        List<BlockPos> targets = ((TerraResonanceHighlightsPacketAccessor) packet).exotic_worlds$getTargets();
        List<BlockPos> seated = new ArrayList<>(targets.size());
        boolean isMoved = false;
        for (BlockPos target : targets) {
            BlockPos position = context.nearestCopy(target);
            isMoved |= !position.equals(target);
            seated.add(position);
        }

        return isMoved ? new TerraResonanceHighlightsPacket(seated) : packet;
    }

    private HighlightPacketRewriters() {
    }
}

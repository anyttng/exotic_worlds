package com.exoticworlds.compat.supplementaries;

import java.util.Set;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.engine.fold.FoldedCopies;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import net.mehvahdjukaar.supplementaries.common.network.ClientBoundPistonCooperatorsPacket;
import net.mehvahdjukaar.supplementaries.common.network.ClientBoundPulleyAttemptPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

final class CooperationPacketRewriters {
    static void register() {
        PacketTranslator.registerClientboundPayloadRewriter(ClientBoundPistonCooperatorsPacket.class,
                CooperationPacketRewriters::pistons);
        PacketTranslator.registerClientboundPayloadRewriter(ClientBoundPulleyAttemptPacket.class,
                CooperationPacketRewriters::pulley);
    }

    static CustomPacketPayload pistons(ClientBoundPistonCooperatorsPacket packet, TranslationContext context) {
        BlockPos seated = context.toClient(packet.pos);
        if (seated.equals(packet.pos)) {
            return packet;
        }

        DeckTransformation move = context.transformer().nearestCopyTransformation(seated, packet.pos);
        Set<BlockPos> cooperators = FoldedCopies.of(packet.cooperators, move::apply);
        return new ClientBoundPistonCooperatorsPacket(seated, cooperators);
    }

    static CustomPacketPayload pulley(ClientBoundPulleyAttemptPacket packet, TranslationContext context) {
        BlockPos seated = context.toClient(packet.pos);
        return seated.equals(packet.pos)
                ? packet
                : new ClientBoundPulleyAttemptPacket(seated, packet.animationTicks, packet.pushDir, packet.tick);
    }

    private CooperationPacketRewriters() {
    }
}

package com.exoticworlds.compat.apprenticecodex;

import java.util.List;

import com.exoticworlds.compat.apprenticecodex.mixin.ClientBlockTargetCastPacketAccessor;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import jp.aquafactory.apprenticecodex.network.packet.ClientBlockTargetCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientEpicFightAttackcastRingTargetsPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientFullautoRapidcastSpellrifleCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientMultipurposeStaffrifleCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientQuickcastCartridgePacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientSpellgunCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientSwingMagicAttackPacket;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

final class BlockTargetPacketRewriters {
    static void register() {
        PacketTranslator.registerServerboundPayloadRewriter(ClientBlockTargetCastPacket.class,
                BlockTargetPacketRewriters::cast);
        PacketTranslator.registerServerboundPayloadRewriter(ClientEpicFightAttackcastRingTargetsPacket.class,
                BlockTargetPacketRewriters::epicFightRing);
        PacketTranslator.registerServerboundPayloadRewriter(ClientFullautoRapidcastSpellrifleCastPacket.class,
                BlockTargetPacketRewriters::rapidcastSpellrifle);
        PacketTranslator.registerServerboundPayloadRewriter(ClientMultipurposeStaffrifleCastPacket.class,
                BlockTargetPacketRewriters::multipurposeStaffrifle);
        PacketTranslator.registerServerboundPayloadRewriter(ClientQuickcastCartridgePacket.class,
                BlockTargetPacketRewriters::quickcastCartridge);
        PacketTranslator.registerServerboundPayloadRewriter(ClientSpellgunCastPacket.class,
                BlockTargetPacketRewriters::spellgun);
        PacketTranslator.registerServerboundPayloadRewriter(ClientSwingMagicAttackPacket.class,
                BlockTargetPacketRewriters::swingMagic);
    }

    static CustomPacketPayload cast(ClientBlockTargetCastPacket packet, TranslationContext context) {
        ClientBlockTargetCastPacketAccessor cast = (ClientBlockTargetCastPacketAccessor) packet;
        BlockTargetData target = cast.exotic_worlds$getTargetData();
        BlockTargetData folded = BlockTargetFold.toServer(target, context.fold());
        return folded == target
                ? packet
                : new ClientBlockTargetCastPacket(cast.exotic_worlds$getQuickCastSlot(), cast.exotic_worlds$getSpellId(),
                        folded, cast.exotic_worlds$shouldInitiateCast());
    }

    static CustomPacketPayload epicFightRing(ClientEpicFightAttackcastRingTargetsPacket packet,
            TranslationContext context) {
        List<BlockTargetData> folded = BlockTargetFold.eachToServer(packet.ringTargets(), context.fold());
        return folded == packet.ringTargets() ? packet : new ClientEpicFightAttackcastRingTargetsPacket(folded);
    }

    static CustomPacketPayload rapidcastSpellrifle(ClientFullautoRapidcastSpellrifleCastPacket packet,
            TranslationContext context) {
        BlockTargetData folded = BlockTargetFold.toServer(packet.targetData(), context.fold());
        return folded == packet.targetData()
                ? packet
                : new ClientFullautoRapidcastSpellrifleCastPacket(packet.adsFullAuto(), folded);
    }

    static CustomPacketPayload multipurposeStaffrifle(ClientMultipurposeStaffrifleCastPacket packet,
            TranslationContext context) {
        BlockTargetData folded = BlockTargetFold.toServer(packet.targetData(), context.fold());
        return folded == packet.targetData()
                ? packet
                : new ClientMultipurposeStaffrifleCastPacket(packet.aiming(), folded);
    }

    static CustomPacketPayload quickcastCartridge(ClientQuickcastCartridgePacket packet, TranslationContext context) {
        BlockTargetData folded = BlockTargetFold.toServer(packet.target(), context.fold());
        return folded == packet.target() ? packet : new ClientQuickcastCartridgePacket(packet.expectedSpell(), folded);
    }

    static CustomPacketPayload spellgun(ClientSpellgunCastPacket packet, TranslationContext context) {
        BlockTargetData folded = BlockTargetFold.toServer(packet.targetData(), context.fold());
        return folded == packet.targetData()
                ? packet
                : new ClientSpellgunCastPacket(folded, packet.deferToEpicFightAttack());
    }

    static CustomPacketPayload swingMagic(ClientSwingMagicAttackPacket packet, TranslationContext context) {
        List<BlockTargetData> folded = BlockTargetFold.eachToServer(packet.ringTargets(), context.fold());
        return folded == packet.ringTargets()
                ? packet
                : new ClientSwingMagicAttackPacket(packet.bypassChargeCheck(), packet.hand(),
                        packet.missEvaluationDelayTicks(), folded);
    }

    private BlockTargetPacketRewriters() {
    }
}

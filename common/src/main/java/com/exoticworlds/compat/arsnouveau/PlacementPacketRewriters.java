package com.exoticworlds.compat.arsnouveau;

import com.exoticworlds.compat.arsnouveau.mixin.ChangeBiomePacketAccessor;
import com.exoticworlds.compat.arsnouveau.mixin.PacketOneShotAnimationAccessor;
import com.exoticworlds.compat.arsnouveau.mixin.PacketWarpPositionAccessor;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;
import com.hollingsworth.arsnouveau.common.network.ChangeBiomePacket;
import com.hollingsworth.arsnouveau.common.network.PacketClientRequestDim;
import com.hollingsworth.arsnouveau.common.network.PacketOneShotAnimation;
import com.hollingsworth.arsnouveau.common.network.PacketUpdateDimTile;
import com.hollingsworth.arsnouveau.common.network.PacketWarpPosition;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

final class PlacementPacketRewriters {
    static void register() {
        PacketTranslator.registerClientboundPayloadRewriter(PacketWarpPosition.class, PlacementPacketRewriters::warp);
        PacketTranslator.registerClientboundPayloadRewriter(ChangeBiomePacket.class, PlacementPacketRewriters::biome);
        PacketTranslator.registerClientboundPayloadRewriter(PacketOneShotAnimation.class,
                PlacementPacketRewriters::animation);
        PacketTranslator.registerClientboundPayloadRewriter(PacketUpdateDimTile.class,
                PlacementPacketRewriters::planariumTemplate);
        PacketTranslator.registerServerboundPayloadRewriter(PacketClientRequestDim.class,
                PlacementPacketRewriters::planariumRequest);
    }

    static CustomPacketPayload warp(PacketWarpPosition packet, TranslationContext context) {
        PacketWarpPositionAccessor warp = (PacketWarpPositionAccessor) packet;
        Vec3 position = new Vec3(warp.exotic_worlds$x(), warp.exotic_worlds$y(), warp.exotic_worlds$z());
        Vec3 seated = context.nearestCopy(position);
        return seated.equals(position)
                ? packet
                : new PacketWarpPosition(warp.exotic_worlds$entityId(), seated.x, seated.y, seated.z,
                        warp.exotic_worlds$xRot(), warp.exotic_worlds$yRot());
    }

    static CustomPacketPayload biome(ChangeBiomePacket packet, TranslationContext context) {
        ChangeBiomePacketAccessor biome = (ChangeBiomePacketAccessor) packet;
        BlockPos seated = context.toClient(biome.exotic_worlds$pos());
        return seated.equals(biome.exotic_worlds$pos())
                ? packet
                : new ChangeBiomePacket(seated, biome.exotic_worlds$biomeId());
    }

    static CustomPacketPayload animation(PacketOneShotAnimation packet, TranslationContext context) {
        PacketOneShotAnimationAccessor animation = (PacketOneShotAnimationAccessor) packet;
        BlockPos pos = new BlockPos(animation.exotic_worlds$x(), animation.exotic_worlds$y(),
                animation.exotic_worlds$z());
        BlockPos seated = context.toClient(pos);
        return seated.equals(pos)
                ? packet
                : new PacketOneShotAnimation(seated.getX(), seated.getY(), seated.getZ(), animation.exotic_worlds$arg());
    }

    static CustomPacketPayload planariumTemplate(PacketUpdateDimTile packet, TranslationContext context) {
        BlockPos seated = context.toClient(packet.pos);
        return seated.equals(packet.pos) ? packet : new PacketUpdateDimTile(seated, packet.structureTemplate);
    }

    static CustomPacketPayload planariumRequest(PacketClientRequestDim packet, TranslationContext context) {
        BlockPos folded = context.toServer(packet.pos);
        return folded.equals(packet.pos) ? packet : new PacketClientRequestDim(folded);
    }

    private PlacementPacketRewriters() {
    }
}

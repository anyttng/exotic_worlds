package com.exoticworlds.engine.net;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.SeamHit;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetTestBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundTestInstanceBlockActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class ServerboundPacketRewriters {
    static ServerboundUseItemOnPacket useItemOn(ServerboundUseItemOnPacket packet, TranslationContext context) {
        BlockHitResult hit = packet.hitResult();
        BlockHitResult wrapped = SeamHit.reseat(hit, context.toServerOriented(hit.getBlockPos()));
        return wrapped == hit
                ? packet
                : new ServerboundUseItemOnPacket(packet.hand(), wrapped, packet.sequence());
    }

    static ServerboundPlayerActionPacket playerAction(ServerboundPlayerActionPacket packet, TranslationContext context) {
        WorldFold.Folded<BlockPos> folded = context.toServerOriented(packet.getPos());
        return new ServerboundPlayerActionPacket(packet.getAction(), folded.value(),
                folded.orientation().applyToFace(packet.getDirection()), packet.getSequence());
    }

    static ServerboundPickItemFromBlockPacket pickItemFromBlock(ServerboundPickItemFromBlockPacket packet, TranslationContext context) {
        return new ServerboundPickItemFromBlockPacket(
                context.toServer(packet.pos()), packet.includeData());
    }

    static ServerboundSignUpdatePacket signUpdate(ServerboundSignUpdatePacket packet, TranslationContext context) {
        return new ServerboundSignUpdatePacket(context.toServer(packet.pos()), packet.lines(), packet.slot());
    }

    static ServerboundBlockEntityTagQueryPacket blockEntityTagQuery(ServerboundBlockEntityTagQueryPacket packet, TranslationContext context) {
        return new ServerboundBlockEntityTagQueryPacket(
                packet.getTransactionId(), context.toServer(packet.getPos()));
    }

    static ServerboundInteractPacket interact(ServerboundInteractPacket packet, TranslationContext context) {
        Vec3 targetPosition = context.entityPosition().apply(packet.entityId());
        Vec3 location = packet.location();

        Vec3 serverLocation = targetPosition == null
                ? context.toServer(location)
                : context.transformer().nearestCopy(targetPosition, location);
        return new ServerboundInteractPacket(
                packet.entityId(), packet.hand(), serverLocation, packet.usingSecondaryAction());
    }

    static ServerboundJigsawGeneratePacket jigsawGenerate(ServerboundJigsawGeneratePacket packet, TranslationContext context) {
        return new ServerboundJigsawGeneratePacket(
                context.toServer(packet.getPos()), packet.levels(), packet.keepJigsaws());
    }

    static ServerboundSetCommandBlockPacket setCommandBlock(ServerboundSetCommandBlockPacket packet, TranslationContext context) {
        return new ServerboundSetCommandBlockPacket(
                context.toServer(packet.getPos()), packet.getCommand(), packet.getMode(),
                packet.isTrackOutput(), packet.isConditional(), packet.isAutomatic());
    }

    static ServerboundSetJigsawBlockPacket setJigsawBlock(ServerboundSetJigsawBlockPacket packet, TranslationContext context) {
        return new ServerboundSetJigsawBlockPacket(
                context.toServer(packet.getPos()), packet.getName(), packet.getTarget(),
                packet.getPool(), packet.getFinalState(), packet.getJoint(),
                packet.getSelectionPriority(), packet.getPlacementPriority());
    }

    static ServerboundSetStructureBlockPacket setStructureBlock(ServerboundSetStructureBlockPacket packet, TranslationContext context) {
        return new ServerboundSetStructureBlockPacket(
                context.toServer(packet.getPos()), packet.getUpdateType(), packet.getMode(),
                packet.getName(), packet.getOffset(), packet.getSize(), packet.getMirror(), packet.getRotation(),
                packet.getData(), packet.isIgnoreEntities(), packet.isStrict(), packet.isShowAir(),
                packet.isShowBoundingBox(), packet.getIntegrity(), packet.getSeed());
    }

    static ServerboundSetTestBlockPacket setTestBlock(ServerboundSetTestBlockPacket packet, TranslationContext context) {
        return new ServerboundSetTestBlockPacket(
                context.toServer(packet.position()), packet.mode(), packet.message());
    }

    static ServerboundTestInstanceBlockActionPacket testInstanceBlockAction(ServerboundTestInstanceBlockActionPacket packet, TranslationContext context) {
        return new ServerboundTestInstanceBlockActionPacket(
                context.toServer(packet.pos()), packet.action(), packet.data());
    }

    static ServerboundSetCreativeModeSlotPacket setCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet,
            TranslationContext context) {
        ItemStack canonical = ComponentPositions.canonical(packet.itemStack(), context);
        return canonical == packet.itemStack()
                ? packet
                : new ServerboundSetCreativeModeSlotPacket(packet.slotNum(), canonical);
    }

    private ServerboundPacketRewriters() {
    }
}

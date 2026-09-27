package com.toroidalworld.engine.net;

import com.toroidalworld.engine.fold.SeamHit;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.mixin.InteractPacketAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class ServerboundPacketRewriters {
    private static final StreamCodec<FriendlyByteBuf, Vec3> HIT_LOCATION_CODEC = StreamCodec.of(
            (buffer, location) -> {
                buffer.writeFloat((float) location.x);
                buffer.writeFloat((float) location.y);
                buffer.writeFloat((float) location.z);
            },
            buffer -> new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));

    private record InteractHeader(int entityId, int actionType) {
    }

    private static final StreamCodec<FriendlyByteBuf, InteractHeader> INTERACT_HEADER_CODEC = StreamCodec.of(
            (buffer, header) -> {
                buffer.writeVarInt(header.entityId());
                buffer.writeVarInt(header.actionType());
            },
            buffer -> new InteractHeader(buffer.readVarInt(), buffer.readVarInt()));

    static ServerboundUseItemOnPacket useItemOn(ServerboundUseItemOnPacket packet, TranslationContext context) {
        BlockHitResult hit = packet.getHitResult();
        BlockHitResult wrapped = SeamHit.reseat(hit, context.toServerOriented(hit.getBlockPos()));
        return wrapped == hit
                ? packet
                : new ServerboundUseItemOnPacket(packet.getHand(), wrapped, packet.getSequence());
    }

    static ServerboundPlayerActionPacket playerAction(ServerboundPlayerActionPacket packet, TranslationContext context) {
        WorldFold.Folded<BlockPos> folded = context.toServerOriented(packet.getPos());
        return new ServerboundPlayerActionPacket(packet.getAction(), folded.value(),
                folded.orientation().applyToFace(packet.getDirection()), packet.getSequence());
    }

    static ServerboundSignUpdatePacket signUpdate(ServerboundSignUpdatePacket packet, TranslationContext context) {
        String[] lines = packet.getLines();
        return new ServerboundSignUpdatePacket(context.toServer(packet.getPos()),
                packet.isFrontText(), lines[0], lines[1], lines[2], lines[3]);
    }

    static ServerboundBlockEntityTagQueryPacket blockEntityTagQuery(ServerboundBlockEntityTagQueryPacket packet, TranslationContext context) {
        return new ServerboundBlockEntityTagQueryPacket(
                packet.getTransactionId(), context.toServer(packet.getPos()));
    }

    static ServerboundInteractPacket interact(ServerboundInteractPacket packet, TranslationContext context) {
        if (!carriesLocation(packet)) {
            return packet;
        }

        return PacketTranslator.rewritePosition(
                (interactPacket, output) -> ((InteractPacketAccessor) interactPacket).toroidal$write(output),
                InteractPacketAccessor::toroidal$create,
                INTERACT_HEADER_CODEC, HIT_LOCATION_CODEC,
                packet, context, (header, location) -> toServerHitLocation(context, header.entityId(), location));
    }

    private static Vec3 toServerHitLocation(TranslationContext context, int entityId, Vec3 location) {
        Vec3 targetPosition = context.entityPosition().apply(entityId);
        return targetPosition == null
                ? context.toServer(location)
                : context.transformer().nearestCopy(targetPosition, location);
    }

    private static boolean carriesLocation(ServerboundInteractPacket packet) {
        boolean[] atLocation = new boolean[1];
        packet.dispatch(new ServerboundInteractPacket.Handler() {
            @Override
            public void onInteraction(InteractionHand hand) {
            }

            @Override
            public void onInteraction(InteractionHand hand, Vec3 location) {
                atLocation[0] = true;
            }

            @Override
            public void onAttack() {
            }
        });

        return atLocation[0];
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
                packet.getData(), packet.isIgnoreEntities(), packet.isShowAir(),
                packet.isShowBoundingBox(), packet.getIntegrity(), packet.getSeed());
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

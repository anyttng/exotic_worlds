package com.exoticworlds.compat.create;

import java.util.List;

import com.simibubi.create.content.equipment.symmetryWand.SymmetryEffectPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelEffectPacket;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.infrastructure.command.HighlightPacket;
import com.exoticworlds.client.engine.SyncedTagFold;
import com.exoticworlds.engine.fold.FoldedCopies;
import com.exoticworlds.engine.net.ComponentPositions;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TagPositions;
import com.exoticworlds.engine.net.TranslationContext;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

// Nothing on a CompoundTag key or a payload component says it holds a world position, so every list below is
// enumerated from Create 6.0.10's own read code and a bump of create_version means reading it again.
public final class CreateTranslation {
    public static final String CONTROLLER_KEY = "Controller";
    public static final String LAST_KNOWN_POS_KEY = "LastKnownPos";
    private static final String PRINTER_KEY = "Printer";
    private static final String ANCHOR_KEY = "Anchor";
    private static final String FLYING_BLOCKS_KEY = "FlyingBlocks";
    private static final String TARGET_KEY = "Target";
    private static final ResourceLocation TRACK_TARGETING_SELECTED_POS_ID =
            ResourceLocation.fromNamespaceAndPath("create", "track_targeting_item_selected_pos");

    public static void register() {
        if (!CreateMod.present()) {
            return;
        }

        registerSyncedTags();
        registerPayloads();
        registerComponents();
    }

    private static void registerComponents() {
        ComponentPositions.register(TRACK_TARGETING_SELECTED_POS_ID);
    }

    private static void registerSyncedTags() {
        SyncedTagFold.register(IMultiBlockEntityContainer.class, TagPositions.PositionShape.BLOCK_POS,
                CONTROLLER_KEY, LAST_KNOWN_POS_KEY);
        SyncedTagFold.registerIn(SchematicannonBlockEntity.class, PRINTER_KEY,
                TagPositions.PositionShape.BLOCK_POS, ANCHOR_KEY);
        SyncedTagFold.registerInEach(SchematicannonBlockEntity.class, FLYING_BLOCKS_KEY,
                TagPositions.PositionShape.BLOCK_POS, TARGET_KEY);
    }

    private static void registerPayloads() {
        // The record fold seats each placement around the player; these belong around the wand's mirror.
        PacketTranslator.registerClientboundPayloadRewriter(SymmetryEffectPacket.class, (payload, context) -> {
            BlockPos mirror = seat(context, payload.mirror());
            List<BlockPos> positions = FoldedCopies.of(payload.positions(),
                    placed -> context.transformer().nearestCopy(mirror, placed));
            return mirror == payload.mirror() && positions == payload.positions()
                    ? payload
                    : new SymmetryEffectPacket(mirror, positions);
        });

        // The command may name any loaded block, so it takes the plain door, not the record fold's guarded one.
        PacketTranslator.registerClientboundPayloadRewriter(HighlightPacket.class, (payload, context) -> {
            BlockPos outlined = seat(context, payload.pos());
            return outlined == payload.pos() ? payload : new HighlightPacket(outlined);
        });

        PacketTranslator.registerClientboundPayloadRewriter(FactoryPanelEffectPacket.class, (payload, context) -> payload);
    }

    private static BlockPos seat(TranslationContext context, BlockPos pos) {
        return context.nearestCopy(pos);
    }

    private CreateTranslation() {
    }
}

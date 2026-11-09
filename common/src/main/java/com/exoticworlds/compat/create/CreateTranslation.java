package com.exoticworlds.compat.create;

import java.util.List;

import com.zurrtum.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.zurrtum.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.zurrtum.create.infrastructure.packet.s2c.HighlightPacket;
import com.zurrtum.create.infrastructure.packet.s2c.SymmetryEffectPacket;
import com.exoticworlds.client.engine.SyncedTagFold;
import com.exoticworlds.engine.fold.FoldedCopies;
import com.exoticworlds.engine.net.ComponentPositions;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TagPositions;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public final class CreateTranslation {
    private static final String CONTROLLER_KEY = "Controller";
    private static final String LAST_KNOWN_POS_KEY = "LastKnownPos";
    private static final String PRINTER_KEY = "Printer";
    private static final String ANCHOR_KEY = "Anchor";
    private static final String FLYING_BLOCKS_KEY = "FlyingBlocks";
    private static final String TARGET_KEY = "Target";
    private static final Identifier TRACK_TARGETING_SELECTED_POS_ID =
            Identifier.fromNamespaceAndPath("create", "track_targeting_item_selected_pos");

    public static void register() {
        if (!CreateMixinPlugin.CREATE.admitted()) {
            return;
        }

        registerSyncedTags();
        registerPackets();
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

    private static void registerPackets() {
        PacketTranslator.registerClientboundPacketRewriter(SymmetryEffectPacket.class, (packet, context) -> {
            BlockPos mirror = context.nearestCopy(packet.mirror());
            List<BlockPos> positions = FoldedCopies.of(packet.positions(),
                    placed -> context.fold().nearestCopy(mirror, placed));
            return mirror == packet.mirror() && positions == packet.positions()
                    ? packet
                    : new SymmetryEffectPacket(mirror, positions);
        });

        PacketTranslator.registerClientboundPacketRewriter(HighlightPacket.class, (packet, context) -> {
            BlockPos outlined = context.nearestCopy(packet.pos());
            return outlined == packet.pos() ? packet : new HighlightPacket(outlined);
        });
    }

    private CreateTranslation() {
    }
}

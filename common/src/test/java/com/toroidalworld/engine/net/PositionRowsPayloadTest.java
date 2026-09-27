package com.toroidalworld.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.toroidalworld.engine.net.TagPositions.Nesting;
import com.toroidalworld.engine.net.TagPositions.PositionShape;
import com.toroidalworld.engine.net.TagPositions.TagPosition;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import net.minecraft.resources.ResourceLocation;

class PositionRowsPayloadTest {
    private static final ResourceLocation HOLDER_ID = ResourceLocation.fromNamespaceAndPath("cject", "holder");
    private static final ResourceLocation RELAY_ID = ResourceLocation.fromNamespaceAndPath("pack", "relay");
    private static final ResourceLocation ANCHOR_COMPONENT_ID = ResourceLocation.fromNamespaceAndPath("cject", "anchor");
    private static final ResourceLocation TARGET_COMPONENT_ID = ResourceLocation.fromNamespaceAndPath("pack", "target");

    private static final List<TagPosition> HOLDER_POSITIONS = List.of(
            new TagPosition("Goal", PositionShape.PACKED_LONG),
            new TagPosition("Partner", PositionShape.BLOCK_POS),
            new TagPosition(Nesting.COMPOUND, "Printer", "Anchor", PositionShape.BLOCK_POS),
            new TagPosition(Nesting.COMPOUND, "Printer", "Cursor", PositionShape.VEC3_LIST),
            new TagPosition(Nesting.EACH_OF_LIST, "FlyingBlocks", "Target", PositionShape.BLOCK_POS));

    private static final List<TagPosition> RELAY_POSITIONS =
            List.of(new TagPosition(Nesting.EACH_OF_LIST, "Queue", "Destination", PositionShape.VEC3_LIST));

    @Test
    void everyAddressFormAndComponentCrossesTheWireAndDrainsTheBuffer() {
        ByteBuf buffer = Unpooled.buffer();
        PositionRowsPayload.STREAM_CODEC.encode(buffer,
                new PositionRowsPayload(Map.of(HOLDER_ID, HOLDER_POSITIONS, RELAY_ID, RELAY_POSITIONS),
                        Set.of(ANCHOR_COMPONENT_ID, TARGET_COMPONENT_ID)));

        PositionRowsPayload decoded = PositionRowsPayload.STREAM_CODEC.decode(buffer);

        assertEquals(Set.of(HOLDER_ID, RELAY_ID), decoded.blockEntities().keySet());
        assertEquals(Set.copyOf(HOLDER_POSITIONS), Set.copyOf(decoded.blockEntities().get(HOLDER_ID)));
        assertEquals(Set.copyOf(RELAY_POSITIONS), Set.copyOf(decoded.blockEntities().get(RELAY_ID)));
        assertEquals(Set.of(ANCHOR_COMPONENT_ID, TARGET_COMPONENT_ID), decoded.components());
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void noRowsCrossTheWireAsNoRows() {
        ByteBuf buffer = Unpooled.buffer();
        PositionRowsPayload.STREAM_CODEC.encode(buffer, new PositionRowsPayload(Map.of(), Set.of()));

        PositionRowsPayload decoded = PositionRowsPayload.STREAM_CODEC.decode(buffer);
        assertEquals(Map.of(), decoded.blockEntities());
        assertEquals(Set.of(), decoded.components());
    }
}

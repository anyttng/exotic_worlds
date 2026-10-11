package com.exoticworlds.engine.net;

import static com.exoticworlds.engine.net.PacketTranslatorFixture.CLIENT_BLOCK;
import static com.exoticworlds.engine.net.PacketTranslatorFixture.CLIENT_CHUNK;
import static com.exoticworlds.engine.net.PacketTranslatorFixture.SERVER_BLOCK;
import static com.exoticworlds.engine.net.PacketTranslatorFixture.SERVER_CHUNK;
import static com.exoticworlds.engine.net.PacketTranslatorFixture.context;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.payload.AuxiliaryLightDataPayload;

class AuxiliaryLightTranslationTest {
    private static final byte TORCH_LIGHT = 14;
    private static final byte LANTERN_LIGHT = 10;

    private static final BlockPos SERVER_NEIGHBOUR = SERVER_BLOCK.offset(3, 2, 5);
    private static final BlockPos CLIENT_NEIGHBOUR = CLIENT_BLOCK.offset(3, 2, 5);

    @Test
    void eachLightOfAChunkHeldALapAwayLandsOnItsKeyInTheHeldCopy() {
        AuxiliaryLightDataPayload seated = (AuxiliaryLightDataPayload) AuxiliaryLightTranslation.seated(
                new AuxiliaryLightDataPayload(SERVER_CHUNK,
                        Map.of(SERVER_BLOCK, TORCH_LIGHT, SERVER_NEIGHBOUR, LANTERN_LIGHT)),
                context());

        assertEquals(CLIENT_CHUNK, seated.pos());
        assertEquals(Map.of(CLIENT_BLOCK, TORCH_LIGHT, CLIENT_NEIGHBOUR, LANTERN_LIGHT), seated.entries());
    }

    @Test
    void aChunkAlreadyInTheViewersFrameIsThePayloadBack() {
        AuxiliaryLightDataPayload payload =
                new AuxiliaryLightDataPayload(CLIENT_CHUNK, Map.of(CLIENT_BLOCK, TORCH_LIGHT));

        assertSame(payload, AuxiliaryLightTranslation.seated(payload, context()));
    }
}

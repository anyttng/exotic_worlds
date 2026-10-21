package com.exoticworlds.compat.ultimatemapatlases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.net.TranslationContext;
import com.exoticworlds.engine.seam.ClientPosition;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.nanaky.moonlight.api.platform.network.ChannelHandler;
import net.nanaky.moonlight.api.platform.network.Message;
import net.nanaky.ultimate_map_atlases.networking.C2S2COpenAtlasScreenPacket;
import net.nanaky.ultimate_map_atlases.networking.C2STakeAtlasPacket;
import net.nanaky.ultimate_map_atlases.networking.MapAtlasesNetworking;

class AtlasLecternTranslationTest {
    private static final int WIDTH_IN_CHUNKS = 32;
    private static final int WIDTH = WIDTH_IN_CHUNKS * 16;
    private static final int VIEW_DISTANCE = 12;

    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(WIDTH_IN_CHUNKS)));

    private static final double CLIENT_X = 240.0;
    private static final BlockPos LECTERN_ACROSS_THE_SEAM = new BlockPos(-250, 64, 10);
    private static final BlockPos LECTERN_BESIDE_THE_CLIENT = new BlockPos(230, 64, 10);

    private static TranslationContext context() {
        ClientPosition clientPosition = new ClientPosition();
        clientPosition.rebase(CLIENT_X, 0.0, Level.OVERWORLD, TORUS);
        return new TranslationContext(
                TORUS,
                clientPosition,
                size -> {
                    throw new UnsupportedOperationException();
                },
                Level.OVERWORLD,
                VIEW_DISTANCE,
                VIEW_DISTANCE,
                entityId -> false,
                entityId -> null,
                entityId -> null,
                () -> {
                });
    }

    private static CustomPacketPayload wrapped(Message message) throws ReflectiveOperationException {
        Field byClass = ChannelHandler.class.getDeclaredField("registrationsByClass");
        byClass.setAccessible(true);
        Object registration = ((Map<?, ?>) byClass.get(MapAtlasesNetworking.CHANNEL)).get(message.getClass());
        Method wrap = registration.getClass().getDeclaredMethod("wrap", Message.class);
        wrap.setAccessible(true);
        return (CustomPacketPayload) wrap.invoke(registration, message);
    }

    private static Object messageOf(CustomPacketPayload payload) throws ReflectiveOperationException {
        Method message = payload.getClass().getDeclaredMethod("message");
        message.setAccessible(true);
        return message.invoke(payload);
    }

    private static Object fieldOf(Object message, String name) throws ReflectiveOperationException {
        Field field = C2S2COpenAtlasScreenPacket.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(message);
    }

    private static CustomPacketPayload translated(CustomPacketPayload payload) throws ReflectiveOperationException {
        return new AtlasLecternTranslation.Rewriter(payload.getClass()).toClient(payload, context());
    }

    @Test
    void aLecternAcrossTheSeamIsSeatedOnTheClientsCopy() throws ReflectiveOperationException {
        CustomPacketPayload sent = wrapped(C2S2COpenAtlasScreenPacket.forLectern(LECTERN_ACROSS_THE_SEAM, true));

        CustomPacketPayload rewritten = translated(sent);

        Object open = messageOf(rewritten);
        assertSame(sent.getClass(), rewritten.getClass());
        assertSame(sent.type(), rewritten.type());
        assertEquals(LECTERN_ACROSS_THE_SEAM.offset(WIDTH, 0, 0), fieldOf(open, "lecternPos"));
        assertSame(C2S2COpenAtlasScreenPacket.OpenSource.LECTERN, fieldOf(open, "source"));
        assertNull(fieldOf(open, "hand"));
        assertEquals(true, fieldOf(open, "pinOnly"));
    }

    @Test
    void aLecternAlreadyInTheClientFrameKeepsThePayload() throws ReflectiveOperationException {
        CustomPacketPayload sent = wrapped(C2S2COpenAtlasScreenPacket.forLectern(LECTERN_BESIDE_THE_CLIENT, false));

        assertSame(sent, translated(sent));
    }

    @Test
    void anOpenScreenWithNoLecternKeepsThePayload() throws ReflectiveOperationException {
        CustomPacketPayload sent = wrapped(C2S2COpenAtlasScreenPacket.forHand(InteractionHand.MAIN_HAND));

        assertSame(sent, translated(sent));
    }

    @Test
    void anotherMessageKeepsThePayload() throws ReflectiveOperationException {
        CustomPacketPayload sent = wrapped(new C2STakeAtlasPacket(LECTERN_ACROSS_THE_SEAM));

        assertSame(sent, translated(sent));
    }
}

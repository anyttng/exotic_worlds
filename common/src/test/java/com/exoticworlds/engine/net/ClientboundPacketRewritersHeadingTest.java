package com.exoticworlds.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.SeamTransform;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.seam.ClientPosition;

import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class ClientboundPacketRewritersHeadingTest {
    private static final double TOLERANCE = 1.0e-6;
    private static final int FACE_WIDTH = 64;
    private static final int VIEW_DISTANCE = 12;
    private static final int TORUS_WIDTH_IN_CHUNKS = 32;
    private static final int TELEPORT_ID = 3;

    private static final SeamTransform QUARTER_TURN_TO_THE_CLIENT =
            new SeamTransform(-1, 1, FACE_WIDTH, -FACE_WIDTH, true);
    private static final SeamTransform MIRROR_TO_THE_CLIENT = new SeamTransform(-1, 1, FACE_WIDTH, 0);

    private static final WorldFold TORUS =
            WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(TORUS_WIDTH_IN_CHUNKS)));

    private static final double ANCHOR_X = 60.0;
    private static final Vec3 DESTINATION = new Vec3(58.5, 70.0, 2.5);
    private static final Vec3 STEP = new Vec3(3.0, 1.0, 0.0);
    private static final Vec3 MOVEMENT = new Vec3(1.0, 0.5, 2.0);
    private static final float YAW = 30.0F;
    private static final float PITCH = 12.0F;

    private static WorldFold standingOnACopy(SeamTransform toTheClient) {
        return (WorldFold) Proxy.newProxyInstance(
                WorldFold.class.getClassLoader(),
                new Class<?>[] {WorldFold.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "preservesLocalIndices" -> false;
                    case "nearestCopy" -> arguments[1];
                    case "foldTransformation" -> new DeckTransformation(toTheClient.inverse());
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static TranslationContext context(WorldFold fold) {
        ClientPosition clientPosition = new ClientPosition();
        clientPosition.rebase(ANCHOR_X, 0.0, Level.OVERWORLD, fold);
        return new TranslationContext(
                fold,
                clientPosition,
                size -> {
                    throw new UnsupportedOperationException();
                },
                Level.OVERWORLD,
                VIEW_DISTANCE,
                VIEW_DISTANCE,
                entityId -> null,
                entityId -> null,
                () -> {
                });
    }

    private static ClientboundPlayerPositionPacket teleport(Vec3 position, Set<Relative> relatives) {
        return new ClientboundPlayerPositionPacket(
                TELEPORT_ID, new PositionMoveRotation(position, MOVEMENT, YAW, PITCH), relatives);
    }

    private static void assertClose(Vec3 expected, Vec3 actual, String what) {
        assertEquals(expected.x, actual.x, TOLERANCE, what + " on x");
        assertEquals(expected.y, actual.y, TOLERANCE, what + " on y");
        assertEquals(expected.z, actual.z, TOLERANCE, what + " on z");
    }

    @Test
    void aTeleportToAQuarterTurnCopyCarriesTheHeadingAndTheMovementTurned() {
        ClientboundPlayerPositionPacket sent = ClientboundPacketRewriters.playerPosition(
                teleport(DESTINATION, Set.of()), context(standingOnACopy(QUARTER_TURN_TO_THE_CLIENT)));

        assertEquals(YAW + 90.0F, sent.change().yRot());
        assertEquals(PITCH, sent.change().xRot());
        assertClose(new Vec3(-MOVEMENT.z, MOVEMENT.y, MOVEMENT.x), sent.change().deltaMovement(), "the movement");
        assertEquals(Set.of(), sent.relatives());
    }

    @Test
    void aTeleportToAMirroredCopyCarriesTheHeadingAndTheMovementReflected() {
        ClientboundPlayerPositionPacket sent = ClientboundPacketRewriters.playerPosition(
                teleport(DESTINATION, Set.of()), context(standingOnACopy(MIRROR_TO_THE_CLIENT)));

        assertEquals(-YAW, sent.change().yRot());
        assertClose(new Vec3(-MOVEMENT.x, MOVEMENT.y, MOVEMENT.z), sent.change().deltaMovement(), "the movement");
    }

    @Test
    void aRelativeTurnIsReversedOnlyByAMirror() {
        Set<Relative> relatives = Set.of(Relative.Y_ROT);

        ClientboundPlayerPositionPacket turned = ClientboundPacketRewriters.playerPosition(
                teleport(DESTINATION, relatives), context(standingOnACopy(QUARTER_TURN_TO_THE_CLIENT)));
        ClientboundPlayerPositionPacket mirrored = ClientboundPacketRewriters.playerPosition(
                teleport(DESTINATION, relatives), context(standingOnACopy(MIRROR_TO_THE_CLIENT)));

        assertEquals(YAW, turned.change().yRot());
        assertEquals(-YAW, mirrored.change().yRot());
    }

    @Test
    void aRelativeStepOnAQuarterTurnCopyIsTakenAlongTheTurnedAxis() {
        TranslationContext context = context(standingOnACopy(QUARTER_TURN_TO_THE_CLIENT));

        ClientboundPlayerPositionPacket sent = ClientboundPacketRewriters.playerPosition(
                teleport(STEP, Set.of(Relative.X, Relative.Y, Relative.Z)), context);

        assertClose(new Vec3(0.0, STEP.y, STEP.x), sent.change().position(), "the step");
        assertEquals(ANCHOR_X, context.clientPosition().x(), TOLERANCE);
        assertEquals(STEP.x, context.clientPosition().z(), TOLERANCE);
    }

    @Test
    void aMovementKeptOnOneAxisIsKeptOnTheAxisItTurnsOnto() {
        ClientboundPlayerPositionPacket sent = ClientboundPacketRewriters.playerPosition(
                teleport(DESTINATION, Set.of(Relative.DELTA_X, Relative.DELTA_Y)),
                context(standingOnACopy(QUARTER_TURN_TO_THE_CLIENT)));

        assertEquals(Set.of(Relative.DELTA_Z, Relative.DELTA_Y), sent.relatives());
    }

    @Test
    void aMovementKeptOnOneAxisStaysOnItAcrossAMirror() {
        Set<Relative> relatives = Set.of(Relative.DELTA_X, Relative.DELTA_Y);

        ClientboundPlayerPositionPacket sent = ClientboundPacketRewriters.playerPosition(
                teleport(DESTINATION, relatives), context(standingOnACopy(MIRROR_TO_THE_CLIENT)));

        assertSame(relatives, sent.relatives());
    }

    @Test
    void aForcedRotationOnAQuarterTurnCopyIsTurned() {
        ClientboundPlayerRotationPacket sent = (ClientboundPlayerRotationPacket) ClientboundPacketRewriters
                .playerRotation(new ClientboundPlayerRotationPacket(YAW, false, PITCH, false),
                        context(standingOnACopy(QUARTER_TURN_TO_THE_CLIENT)));

        assertEquals(new ClientboundPlayerRotationPacket(YAW + 90.0F, false, PITCH, false), sent);
    }

    @Test
    void aRelativeForcedRotationOnAMirroredCopyIsReversed() {
        ClientboundPlayerRotationPacket sent = (ClientboundPlayerRotationPacket) ClientboundPacketRewriters
                .playerRotation(new ClientboundPlayerRotationPacket(YAW, true, PITCH, true),
                        context(standingOnACopy(MIRROR_TO_THE_CLIENT)));

        assertEquals(new ClientboundPlayerRotationPacket(-YAW, true, PITCH, true), sent);
    }

    @Test
    void aVehicleCorrectionOnAQuarterTurnCopyCarriesTheHeadingTurned() {
        ClientboundMoveVehiclePacket sent = (ClientboundMoveVehiclePacket) ClientboundPacketRewriters.moveVehicle(
                new ClientboundMoveVehiclePacket(DESTINATION, YAW, PITCH),
                context(standingOnACopy(QUARTER_TURN_TO_THE_CLIENT)));

        assertEquals(DESTINATION, sent.position());
        assertEquals(YAW + 90.0F, sent.yRot());
        assertEquals(PITCH, sent.xRot());
    }

    @Test
    void aTranslationSeamLeavesTheHeadingAndTheMovementAsSent() {
        TranslationContext context = context(TORUS);
        ClientboundPlayerRotationPacket rotation = new ClientboundPlayerRotationPacket(YAW, false, PITCH, false);

        ClientboundPlayerPositionPacket sent = ClientboundPacketRewriters.playerPosition(
                teleport(DESTINATION, Relative.DELTA), context);

        assertEquals(YAW, sent.change().yRot());
        assertSame(MOVEMENT, sent.change().deltaMovement());
        assertSame(Relative.DELTA, sent.relatives());
        assertSame(rotation, ClientboundPacketRewriters.playerRotation(rotation, context));
    }
}

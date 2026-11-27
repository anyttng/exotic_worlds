package com.exoticworlds.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.engine.seam.ClientPosition;

import net.minecraft.core.PositionAndRotation;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class ClientboundPacketRewritersHeadingTest {
    private static final double TOLERANCE = 1.0e-6;
    private static final int VIEW_DISTANCE_CHUNKS = 5;
    private static final int TORUS_WIDTH_IN_CHUNKS = 32;
    private static final int TELEPORT_ID = 3;

    private static final WorldFold TORUS =
            WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(TORUS_WIDTH_IN_CHUNKS)));

    private static final double ANCHOR_X = 60.0;
    private static final Vec3 DESTINATION = new Vec3(58.5, 70.0, 2.5);
    private static final Vec3 BESIDE_THE_MIRROR = new Vec3(-1.5, 70.0, 2.5);
    private static final Vec3 STEP = new Vec3(3.0, 1.0, 0.0);
    private static final Vec3 MOVEMENT = new Vec3(1.0, 0.5, 2.0);
    private static final float YAW = 30.0F;
    private static final float PITCH = 12.0F;

    private static TranslationContext context(WorldFold fold, ClientPosition clientPosition) {
        return new TranslationContext(
                fold,
                clientPosition,
                size -> {
                    throw new UnsupportedOperationException();
                },
                Level.OVERWORLD,
                VIEW_DISTANCE_CHUNKS,
                VIEW_DISTANCE_CHUNKS,
                entityId -> null,
                entityId -> null,
                () -> {
                });
    }

    private static TranslationContext context(WalkedMirrors.Walked walked) {
        return context(walked.fold(), walked.mirror());
    }

    private static Vec3 besideTheMirror(WalkedMirrors.Walked walked) {
        return walked.serverPoint().add(BESIDE_THE_MIRROR);
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
        WalkedMirrors.Walked walked = WalkedMirrors.acrossAQuarterTurn();
        ClientboundPlayerPositionPacket sent = ClientboundPacketRewriters.playerPosition(
                teleport(besideTheMirror(walked), Set.of()), context(walked));

        assertEquals(YAW + 90.0F, sent.change().yRot());
        assertEquals(PITCH, sent.change().xRot());
        assertClose(new Vec3(-MOVEMENT.z, MOVEMENT.y, MOVEMENT.x), sent.change().deltaMovement(), "the movement");
        assertEquals(Set.of(), sent.relatives());
    }

    @Test
    void aTeleportToAMirroredCopyCarriesTheHeadingAndTheMovementReflected() {
        WalkedMirrors.Walked walked = WalkedMirrors.acrossAMirror();
        ClientboundPlayerPositionPacket sent = ClientboundPacketRewriters.playerPosition(
                teleport(besideTheMirror(walked), Set.of()), context(walked));

        assertEquals(-YAW, sent.change().yRot());
        assertClose(new Vec3(-MOVEMENT.x, MOVEMENT.y, MOVEMENT.z), sent.change().deltaMovement(), "the movement");
    }

    @Test
    void aRelativeTurnIsReversedOnlyByAMirror() {
        Set<Relative> relatives = Set.of(Relative.Y_ROT);
        WalkedMirrors.Walked quarterTurn = WalkedMirrors.acrossAQuarterTurn();
        WalkedMirrors.Walked mirror = WalkedMirrors.acrossAMirror();

        ClientboundPlayerPositionPacket turned = ClientboundPacketRewriters.playerPosition(
                teleport(besideTheMirror(quarterTurn), relatives), context(quarterTurn));
        ClientboundPlayerPositionPacket mirrored = ClientboundPacketRewriters.playerPosition(
                teleport(besideTheMirror(mirror), relatives), context(mirror));

        assertEquals(YAW, turned.change().yRot());
        assertEquals(-YAW, mirrored.change().yRot());
    }

    @Test
    void aRelativeStepOnAQuarterTurnCopyIsTakenAlongTheTurnedAxis() {
        WalkedMirrors.Walked walked = WalkedMirrors.acrossAQuarterTurn();
        Vec3 before = walked.clientPoint();
        TranslationContext context = context(walked);

        ClientboundPlayerPositionPacket sent = ClientboundPacketRewriters.playerPosition(
                teleport(STEP, Set.of(Relative.X, Relative.Y, Relative.Z)), context);

        assertClose(new Vec3(0.0, STEP.y, STEP.x), sent.change().position(), "the step");
        assertEquals(before.x, context.clientPosition().getX(), TOLERANCE);
        assertEquals(before.z + STEP.x, context.clientPosition().getZ(), TOLERANCE);
    }

    @Test
    void aMovementKeptOnOneAxisIsKeptOnTheAxisItTurnsOnto() {
        WalkedMirrors.Walked walked = WalkedMirrors.acrossAQuarterTurn();
        ClientboundPlayerPositionPacket sent = ClientboundPacketRewriters.playerPosition(
                teleport(besideTheMirror(walked), Set.of(Relative.DELTA_X, Relative.DELTA_Y)), context(walked));

        assertEquals(Set.of(Relative.DELTA_Z, Relative.DELTA_Y), sent.relatives());
    }

    @Test
    void aMovementKeptOnOneAxisStaysOnItAcrossAMirror() {
        Set<Relative> relatives = Set.of(Relative.DELTA_X, Relative.DELTA_Y);
        WalkedMirrors.Walked walked = WalkedMirrors.acrossAMirror();

        ClientboundPlayerPositionPacket sent = ClientboundPacketRewriters.playerPosition(
                teleport(besideTheMirror(walked), relatives), context(walked));

        assertSame(relatives, sent.relatives());
    }

    @Test
    void aForcedRotationOnAQuarterTurnCopyIsTurned() {
        ClientboundPlayerRotationPacket sent = (ClientboundPlayerRotationPacket) ClientboundPacketRewriters
                .playerRotation(new ClientboundPlayerRotationPacket(YAW, false, PITCH, false),
                        context(WalkedMirrors.acrossAQuarterTurn()));

        assertEquals(new ClientboundPlayerRotationPacket(YAW + 90.0F, false, PITCH, false), sent);
    }

    @Test
    void aRelativeForcedRotationOnAMirroredCopyIsReversed() {
        ClientboundPlayerRotationPacket sent = (ClientboundPlayerRotationPacket) ClientboundPacketRewriters
                .playerRotation(new ClientboundPlayerRotationPacket(YAW, true, PITCH, true),
                        context(WalkedMirrors.acrossAMirror()));

        assertEquals(new ClientboundPlayerRotationPacket(-YAW, true, PITCH, true), sent);
    }

    @Test
    void aVehicleCorrectionOnAQuarterTurnCopyCarriesTheHeadingTurned() {
        WalkedMirrors.Walked walked = WalkedMirrors.acrossAQuarterTurn();
        Vec3 destination = besideTheMirror(walked);
        ClientboundMoveVehiclePacket sent = (ClientboundMoveVehiclePacket) ClientboundPacketRewriters.moveVehicle(
                new ClientboundMoveVehiclePacket(PositionAndRotation.of(destination, YAW, PITCH)), context(walked));

        assertClose(walked.seatOf(destination), sent.movingTo().position(), "the seat");
        assertEquals(YAW + 90.0F, sent.movingTo().yRot());
        assertEquals(PITCH, sent.movingTo().xRot());
    }

    @Test
    void aTranslationSeamLeavesTheHeadingAndTheMovementAsSent() {
        ClientPosition clientPosition = new ClientPosition();
        clientPosition.rebase(ANCHOR_X, 0.0, Level.OVERWORLD, TORUS);
        TranslationContext context = context(TORUS, clientPosition);
        ClientboundPlayerRotationPacket rotation = new ClientboundPlayerRotationPacket(YAW, false, PITCH, false);

        ClientboundPlayerPositionPacket sent = ClientboundPacketRewriters.playerPosition(
                teleport(DESTINATION, Relative.DELTA), context);

        assertEquals(YAW, sent.change().yRot());
        assertSame(MOVEMENT, sent.change().deltaMovement());
        assertSame(Relative.DELTA, sent.relatives());
        assertSame(rotation, ClientboundPacketRewriters.playerRotation(rotation, context));
    }
}

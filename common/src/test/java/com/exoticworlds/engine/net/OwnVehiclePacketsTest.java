package com.exoticworlds.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.seam.ClientPosition;

import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class OwnVehiclePacketsTest {
    private static final int WIDTH_IN_CHUNKS = 32;
    private static final int WIDTH = WIDTH_IN_CHUNKS * 16;
    private static final int VIEW_DISTANCE = 12;
    private static final int VEHICLE_ID = 7;

    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(WIDTH_IN_CHUNKS)));

    private static final Vec3 VEHICLE = new Vec3(240.5, 64.0, 10.5);
    private static final double ANCHOR_ACROSS_THE_SEAM = -240.0;

    private static TranslationContext contextRidingTheVehicle() {
        ClientPosition clientPosition = new ClientPosition();
        clientPosition.rebase(ANCHOR_ACROSS_THE_SEAM, 0.0, Level.OVERWORLD, TORUS);
        return new TranslationContext(
                TORUS,
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

    @Test
    void theOwnVehiclesSyncReachesTheClientSeatedInItsLappedCopy() {
        ClientboundEntityPositionSyncPacket sync = ClientboundPacketRewriters.entityPositionSync(
                new ClientboundEntityPositionSyncPacket(
                        VEHICLE_ID, new PositionMoveRotation(VEHICLE, Vec3.ZERO, 0.0F, 0.0F), true),
                contextRidingTheVehicle());

        assertEquals(VEHICLE_ID, sync.id());
        assertEquals(VEHICLE.subtract(WIDTH, 0.0, 0.0), sync.values().position());
    }

    @Test
    void theOwnVehiclesTeleportReachesTheClientSeatedInItsLappedCopy() {
        ClientboundTeleportEntityPacket teleport = ClientboundPacketRewriters.teleportEntity(
                new ClientboundTeleportEntityPacket(VEHICLE_ID,
                        new PositionMoveRotation(VEHICLE, Vec3.ZERO, 0.0F, 0.0F), Set.of(), true),
                contextRidingTheVehicle());

        assertEquals(VEHICLE_ID, teleport.id());
        assertEquals(VEHICLE.subtract(WIDTH, 0.0, 0.0), teleport.change().position());
    }
}

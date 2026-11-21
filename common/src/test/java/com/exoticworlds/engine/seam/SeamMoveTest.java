package com.exoticworlds.engine.seam;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.SeamTransform;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.world.phys.Vec3;

class SeamMoveTest {
    private static final int FACE_WIDTH = 64;
    private static final int TORUS_WIDTH_IN_CHUNKS = 32;
    private static final int TORUS_WIDTH = TORUS_WIDTH_IN_CHUNKS * 16;

    private static final SeamTransform QUARTER_TURN_TO_THE_SERVER =
            new SeamTransform(-1, 1, FACE_WIDTH, -FACE_WIDTH, true);
    private static final SeamTransform MIRROR_ON_Z_TO_THE_SERVER = new SeamTransform(1, -1, 0, FACE_WIDTH);

    private static final WorldFold TORUS =
            WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(TORUS_WIDTH_IN_CHUNKS)));

    private static final Vec3 SERVER_POSITION = new Vec3(60.5, 70.0, 2.5);
    private static final Vec3 CLIENT_POINT = new Vec3(66.5, 70.0, -3.5);
    private static final float YAW = 30.0F;

    private static WorldFold seatingThrough(SeamTransform toTheServer) {
        return (WorldFold) Proxy.newProxyInstance(
                WorldFold.class.getClassLoader(),
                new Class<?>[] {WorldFold.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "nearestCopyTransformation" -> new DeckTransformation(toTheServer);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    void aHeadingSentFromAQuarterTurnCopyIsTurnedIntoTheServersFrame() {
        assertEquals(YAW + 90.0F,
                SeamMove.serverYaw(seatingThrough(QUARTER_TURN_TO_THE_SERVER), SERVER_POSITION, CLIENT_POINT, YAW));
    }

    @Test
    void aHeadingSentFromAMirroredCopyIsReflectedIntoTheServersFrame() {
        assertEquals(180.0F - YAW,
                SeamMove.serverYaw(seatingThrough(MIRROR_ON_Z_TO_THE_SERVER), SERVER_POSITION, CLIENT_POINT, YAW));
    }

    @Test
    void aHeadingSentFromALappedCopyIsLeftAsSent() {
        assertEquals(YAW, SeamMove.serverYaw(TORUS, SERVER_POSITION, SERVER_POSITION.add(TORUS_WIDTH, 0.0, 0.0), YAW));
    }
}

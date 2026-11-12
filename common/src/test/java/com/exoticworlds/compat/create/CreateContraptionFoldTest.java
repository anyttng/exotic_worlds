package com.exoticworlds.compat.create;

import static com.exoticworlds.compat.CompatFoldFixture.DECK_TORUS;
import static com.exoticworlds.compat.CompatFoldFixture.MIRRORED;
import static com.exoticworlds.compat.CompatFoldFixture.PER_AXIS;
import static com.exoticworlds.compat.CompatFoldFixture.SKEWED;
import static com.exoticworlds.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

class CreateContraptionFoldTest {
    private static final double TOLERANCE = 1.0E-9;
    private static final double STEP = 2.0;

    private static final List<WorldFold> TRANSLATING = List.of(PER_AXIS, DECK_TORUS, SKEWED);
    private static final List<WorldFold> ALL = List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED);

    private static final Vec3 GANTRY = new Vec3(10.5, 64.0, 200.5);
    private static final List<Vec3> VIEWER_OFFSETS = List.of(Vec3.ZERO, new Vec3(WORLD_BLOCKS, 0.0, 0.0),
            new Vec3(0.0, 0.0, -WORLD_BLOCKS), new Vec3(WORLD_BLOCKS, 0.0, -WORLD_BLOCKS));

    private static Vec3 copyNear(WorldFold fold, Vec3 viewerOffset) {
        return fold.nearestCopy(GANTRY.add(viewerOffset), GANTRY);
    }

    private static double syncedCoord(Direction.Axis axis) {
        return GANTRY.get(axis) + STEP;
    }

    @Test
    void aSyncedCoordLandsOneStepFromTheEntityInEveryTranslatedCopy() {
        for (WorldFold fold : TRANSLATING) {
            for (Vec3 offset : VIEWER_OFFSETS) {
                Vec3 client = copyNear(fold, offset);
                for (Direction.Axis axis : Direction.Axis.values()) {
                    double folded = CreateContraptionFold.axisInFrameOf(fold, client, axis, syncedCoord(axis));

                    assertEquals(client.get(axis) + STEP, folded, TOLERANCE,
                            axis + " gantry seen from " + client + " in " + fold);
                }
            }
        }
    }

    @Test
    void anXGantryAcrossTheSkewedZSeamTakesTheSkewWithIt() {
        Vec3 client = copyNear(SKEWED, new Vec3(0.0, 0.0, -WORLD_BLOCKS));
        assertNotEquals(GANTRY.x, client.x, "the rig's copy is not skewed, so this says nothing about the lattice");

        double folded = CreateContraptionFold.axisInFrameOf(SKEWED, client, Direction.Axis.X,
                syncedCoord(Direction.Axis.X));

        assertEquals(client.x + STEP, folded, TOLERANCE);
    }

    @Test
    void aSyncedCoordKeepsItsDistanceFromTheEntityInEveryCopy() {
        for (WorldFold fold : ALL) {
            for (Vec3 offset : VIEWER_OFFSETS) {
                Vec3 client = copyNear(fold, offset);
                for (Direction.Axis axis : Direction.Axis.values()) {
                    double folded = CreateContraptionFold.axisInFrameOf(fold, client, axis, syncedCoord(axis));

                    assertEquals(STEP, Math.abs(folded - client.get(axis)), TOLERANCE,
                            axis + " gantry seen from " + client + " in " + fold);
                }
            }
        }
    }
}

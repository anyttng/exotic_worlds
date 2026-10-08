package com.exoticworlds.compat.wover;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.Direction;

class LapNoiseTest {
    private static final long SEED = 0x937L;

    private static final double[] CONDITION_SCALES = {0.03, 0.1, 0.2};

    private static final int BLOCK_STEP = 7;

    private static final int SURFACE_Y = 64;

    private static final double TOLERANCE = 1.0E-9;

    private static final int CYLINDER_WIDTH = 32;

    @Test
    void aConditionReadRepeatsOneLapOnOnATinyTorus() {
        assertRepeatsOnLoopingAxes(WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-16, 16, -16, 16))));
    }

    @Test
    void aConditionReadRepeatsOneLapOnOnUnevenAxes() {
        assertRepeatsOnLoopingAxes(WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-75, 75, -41, 41))));
    }

    @Test
    void aCylinderClosesTheLoopingAxisAndLeavesTheOtherOpen() {
        WorldFold cylinder = WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X,
                CYLINDER_WIDTH)));
        assertRepeatsOnLoopingAxes(cylinder);
        for (double scale : CONDITION_SCALES) {
            assertEquals(OpenSimplexStandIn.UNBOUNDED, LapNoise.period(cylinder, Direction.Axis.Z, scale));
        }
    }

    private static void assertRepeatsOnLoopingAxes(WorldFold fold) {
        OpenSimplexStandIn noise = new OpenSimplexStandIn(SEED);
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            WrapDomain domain = fold.blockDomain(axis);
            if (!domain.loops()) {
                continue;
            }

            for (double scale : CONDITION_SCALES) {
                for (int block = domain.lowerBound; block < domain.lowerBound + domain.domainLength;
                        block += BLOCK_STEP) {
                    int lapOn = block + domain.domainLength;
                    int other = block / 2;
                    int x = axis == Direction.Axis.X ? block : other;
                    int z = axis == Direction.Axis.X ? other : block;
                    int xLapOn = axis == Direction.Axis.X ? lapOn : other;
                    int zLapOn = axis == Direction.Axis.X ? other : lapOn;
                    String where = axis.getName() + " " + block + " at scale " + scale;
                    assertEquals(LapNoise.eval(noise, fold, x * scale, z * scale, scale, scale),
                            LapNoise.eval(noise, fold, xLapOn * scale, zLapOn * scale, scale, scale), TOLERANCE,
                            where);
                    assertEquals(LapNoise.eval(noise, fold, x * scale, SURFACE_Y * scale, z * scale, scale, scale),
                            LapNoise.eval(noise, fold, xLapOn * scale, SURFACE_Y * scale, zLapOn * scale, scale,
                                    scale),
                            TOLERANCE, where + " in three dimensions");
                }
            }
        }
    }
}

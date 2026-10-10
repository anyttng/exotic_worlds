package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;

class BlendedNoiseOverflowTest {
    private static final long SEED = 0x0153EL;
    private static final int SAMPLES = 64;
    private static final int LOWEST_Y = -64;
    private static final int WORLD_HEIGHT = 384;

    private static final int HALF_CHUNK_SPAN = 1 << 20;

    private static final WorldFold WIDE = WorldFolds.of(FlatShape.torus(
            new WorldLoopBounds(-HALF_CHUNK_SPAN, HALF_CHUNK_SPAN, -HALF_CHUNK_SPAN, HALF_CHUNK_SPAN)));

    @Test
    void vanillaBlendedNoiseOnAWideWorldStaysInRangeAndRepeatsOneLapAway() {
        BlendedNoiseFixture.Params params = BlendedNoiseFixture.Params.OVERWORLD;
        BlendedNoise noise = BlendedNoise.createUnseeded(params.xzScale(), params.yScale(), params.xzFactor(),
                params.yFactor(), params.smear());
        BlendedNoiseFixture.Replica replica = BlendedNoiseFixture.Replica.of(SEED);
        WrapDomain xDomain = WIDE.blockDomain(Direction.Axis.X);
        WrapDomain zDomain = WIDE.blockDomain(Direction.Axis.Z);
        Random random = new Random(SEED);
        for (int i = 0; i < SAMPLES; i++) {
            int x = blockIn(random, xDomain);
            int y = LOWEST_Y + random.nextInt(WORLD_HEIGHT);
            int z = blockIn(random, zDomain);
            double here = BlendedNoiseFixture.folded(replica, params, WIDE, x, y, z);
            String at = " at (" + x + ", " + y + ", " + z + ")";
            assertTrue(here >= noise.minValue() && here <= noise.maxValue(),
                    "blended noise left its range" + at + ": " + here);
            assertEquals(here, BlendedNoiseFixture.folded(replica, params, WIDE, x + xDomain.domainLength, y, z),
                    "one X lap away" + at);
            assertEquals(here, BlendedNoiseFixture.folded(replica, params, WIDE, x, y, z + zDomain.domainLength),
                    "one Z lap away" + at);
        }
    }

    private static int blockIn(Random random, WrapDomain domain) {
        return domain.lowerBound + random.nextInt(domain.domainLength);
    }
}

package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.fold.WrapDomain;

import net.minecraft.core.Direction;
import net.minecraft.util.Interval;

class BlendedNoiseOverflowTest {
    private static final long SEED = 0x0153EL;
    private static final int SAMPLES = 64;
    private static final int LOWEST_Y = -64;
    private static final int WORLD_HEIGHT_BLOCKS = 384;

    private static final int HALF_SPAN_CHUNKS = 1 << 20;

    private static final WorldFold WIDE = WorldFolds.of(ShapeFixtures.torus(
            ShapeFixtures.loopedBounds(-HALF_SPAN_CHUNKS, HALF_SPAN_CHUNKS, -HALF_SPAN_CHUNKS, HALF_SPAN_CHUNKS)));

    private static final BlendedNoiseFixture.Params EXTREME_XZ_SCALE =
            new BlendedNoiseFixture.Params(1000.0, 0.125, 80.0, 160.0, 8.0);

    @Test
    void vanillaBlendedNoiseOnAWideWorldStaysInRangeAndRepeatsOneLapAway() {
        BlendedNoiseFixture.Params params = BlendedNoiseFixture.Params.OVERWORLD;
        Interval range = params.noise().range();
        BlendedNoiseFixture.Replica replica = BlendedNoiseFixture.Replica.of(SEED);
        WrapDomain xDomain = WIDE.getBlockDomain(Direction.Axis.X);
        WrapDomain zDomain = WIDE.getBlockDomain(Direction.Axis.Z);
        Random random = new Random(SEED);
        for (int i = 0; i < SAMPLES; i++) {
            int x = blockIn(random, xDomain);
            int y = LOWEST_Y + random.nextInt(WORLD_HEIGHT_BLOCKS);
            int z = blockIn(random, zDomain);
            double here = BlendedNoiseFixture.folded(replica, params, WIDE, x, y, z);
            String at = " at (" + x + ", " + y + ", " + z + ")";
            assertTrue(range.contains((float) here), "blended noise left its range" + at + ": " + here);
            assertEquals(here, BlendedNoiseFixture.folded(replica, params, WIDE, x + xDomain.domainLength, y, z),
                    "one X lap away" + at);
            assertEquals(here, BlendedNoiseFixture.folded(replica, params, WIDE, x, y, z + zDomain.domainLength),
                    "one Z lap away" + at);
        }
    }

    @Test
    void anExtremeHorizontalScaleSamplesVanillaAtTheFoldedBlock() {
        BlendedNoiseFixture.Replica replica = BlendedNoiseFixture.Replica.of(SEED);
        TranslationLattice lattice = WIDE.getBlockLattice();
        WrapDomain xDomain = WIDE.getBlockDomain(Direction.Axis.X);
        WrapDomain zDomain = WIDE.getBlockDomain(Direction.Axis.Z);
        Random random = new Random(SEED);
        for (int i = 0; i < SAMPLES; i++) {
            int x = blockIn(random, xDomain);
            int y = LOWEST_Y + random.nextInt(WORLD_HEIGHT_BLOCKS);
            int z = blockIn(random, zDomain);
            for (int[] block : new int[][] {{x, z}, {x + xDomain.domainLength, z}, {x, z + zDomain.domainLength}}) {
                double expected = BlendedNoiseFixture.vanilla(replica, EXTREME_XZ_SCALE,
                        lattice.foldX(block[0], block[1]), y, lattice.foldZ(block[1]));
                assertEquals(expected, BlendedNoiseFixture.folded(replica, EXTREME_XZ_SCALE, WIDE, block[0], y,
                        block[1]), "vanilla at the folded block at (" + block[0] + ", " + y + ", " + block[1] + ")");
            }
        }
    }

    private static int blockIn(Random random, WrapDomain domain) {
        return domain.lowerBound + random.nextInt(domain.domainLength);
    }
}

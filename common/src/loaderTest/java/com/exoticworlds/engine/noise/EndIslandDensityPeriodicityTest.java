package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.DensityFunctionFixture.SEED;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.blockY;
import static com.exoticworlds.engine.noise.EndIslandFixture.CYLINDER;
import static com.exoticworlds.engine.noise.EndIslandFixture.END_ISLANDS;
import static com.exoticworlds.engine.noise.EndIslandFixture.NO_ISLAND_DENSITY;
import static com.exoticworlds.engine.noise.EndIslandFixture.TORUS;
import static com.exoticworlds.engine.noise.EndIslandFixture.acrossTheSeam;
import static com.exoticworlds.engine.noise.EndIslandFixture.anyBlock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;

class EndIslandDensityPeriodicityTest {
    private static final int SAMPLES = 256;

    @Test
    void endIslandDensityRepeatsOneWorldWidthAwayAcrossTheXSeam() {
        repeatsAcrossTheSeam(List.of(TORUS, CYLINDER), Direction.Axis.X);
    }

    @Test
    void endIslandDensityRepeatsOneWorldWidthAwayAcrossTheZSeam() {
        repeatsAcrossTheSeam(List.of(TORUS), Direction.Axis.Z);
    }

    private static void repeatsAcrossTheSeam(List<WorldFold> folds, Direction.Axis axis) {
        Random random = new Random(SEED);
        for (WorldFold fold : folds) {
            WrapDomain domain = fold.blockDomain(axis);
            WrapDomain other = fold.blockDomain(axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            int hits = 0;
            for (int i = 0; i < SAMPLES; i++) {
                int along = acrossTheSeam(random, domain);
                int y = blockY(random);
                int across = anyBlock(random, other);
                double base = sample(fold, axis, along, y, across);
                assertEquals(base, sample(fold, axis, along + domain.domainLength, y, across),
                        () -> "end_islands at " + axis + "=" + along + " vs " + axis + "="
                                + (along + domain.domainLength) + ", across " + across + " in " + fold);
                hits += base > NO_ISLAND_DENSITY ? 1 : 0;
            }

            int stood = hits;
            assertTrue(stood > 0, () -> "no sample across the " + axis + " seam of " + fold
                    + " stood on an island, so every comparison read the void");
        }
    }

    private static double sample(WorldFold fold, Direction.Axis axis, int along, int y, int across) {
        DensitySampler islands = DensityFunctionFixture.compile(END_ISLANDS, fold);
        return axis == Direction.Axis.X
                ? DensityFunctionFixture.sample(islands, along, y, across)
                : DensityFunctionFixture.sample(islands, across, y, along);
    }
}

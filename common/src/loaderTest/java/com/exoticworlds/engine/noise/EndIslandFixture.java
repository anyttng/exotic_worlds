package com.exoticworlds.engine.noise;

import java.util.Random;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public final class EndIslandFixture {
    private static final int SWEEP_REACH_BLOCKS = 13 * 16;
    private static final int UNBOUNDED_SPAN = 4096;
    private static final float NO_ISLAND_HEIGHT = -100.0F;

    public static final WorldFold TORUS = WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-128, 128, -96, 96)));

    public static final WorldFold CYLINDER = WorldFolds.of(FlatShape.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-128, 128), AxisBounds.Unbounded.INSTANCE)));

    public static final DensityFunction END_ISLANDS = DensityFunctions.endIslands(DensityFunctionFixture.SEED);

    public static final double NO_ISLAND_DENSITY = PeriodicEndIslands.density(NO_ISLAND_HEIGHT);

    public static int acrossTheSeam(Random random, WrapDomain domain) {
        return domain.lowerBound - SWEEP_REACH_BLOCKS + random.nextInt(2 * SWEEP_REACH_BLOCKS);
    }

    public static int anyBlock(Random random, WrapDomain domain) {
        if (!domain.loops()) {
            return random.nextInt(UNBOUNDED_SPAN) - UNBOUNDED_SPAN / 2;
        }

        return domain.lowerBound + random.nextInt(domain.domainLength);
    }

    private EndIslandFixture() {
    }
}

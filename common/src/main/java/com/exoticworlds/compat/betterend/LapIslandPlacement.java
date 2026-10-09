package com.exoticworlds.compat.betterend;

import java.util.List;
import java.util.function.IntBinaryOperator;

import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.generator.LayerOptions;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

public final class LapIslandPlacement {
    public record Island(BlockPos canonical, BlockPos seated) {
    }

    public record Centre(boolean island, int biomesSize, long innerVoidSquared) {
    }

    public record Grid(IslandLapAxis x, IslandLapAxis z) {
    }

    private static final int CENTRAL_ISLAND_Y = 64;

    private static final BlockPos CENTRAL_ISLAND = new BlockPos(0, CENTRAL_ISLAND_Y, 0);

    private static final double COVERAGE_FREQUENCY = 0.01;

    private static final int REACH = 1;

    public static void place(List<Island> into, Grid grid, int cellX, int cellZ, int maxHeight, LayerOptions options,
            IntBinaryOperator seedOf, OpenSimplexNoise coverage, Centre centre) {
        into.clear();
        for (int offsetX = -REACH; offsetX <= REACH; offsetX++) {
            int rawX = cellX + offsetX;
            int canonicalX = grid.x().wrap(rawX);
            long fromOriginX = grid.x().nearestToOrigin(canonicalX);
            for (int offsetZ = -REACH; offsetZ <= REACH; offsetZ++) {
                int rawZ = cellZ + offsetZ;
                int canonicalZ = grid.z().wrap(rawZ);
                long fromOriginZ = grid.z().nearestToOrigin(canonicalZ);
                if (fromOriginX * fromOriginX + fromOriginZ * fromOriginZ <= options.centerDist) {
                    continue;
                }

                RandomSource random = new LegacyRandomSource(seedOf.applyAsInt(canonicalX, canonicalZ));
                double blockX = (canonicalX + random.nextFloat()) * grid.x().cellBlocks();
                double blockY = MHelper.randRange(options.minY, options.maxY, random) * maxHeight;
                double blockZ = (canonicalZ + random.nextFloat()) * grid.z().cellBlocks();
                if (coverage.eval(blockX * COVERAGE_FREQUENCY, blockZ * COVERAGE_FREQUENCY) > options.coverage) {
                    BlockPos canonical = new BlockPos((int) blockX, (int) blockY, (int) blockZ);
                    into.add(new Island(canonical,
                            canonical.offset(grid.x().shift(rawX), 0, grid.z().shift(rawZ))));
                }
            }
        }

        clearCentre(into, grid, cellX, cellZ, options, centre);
    }

    private static void clearCentre(List<Island> into, Grid grid, int cellX, int cellZ, LayerOptions options,
            Centre centre) {
        if (!centre.island()
                || Math.abs(grid.x().nearestToOrigin(grid.x().wrap(cellX))) >= centre.biomesSize()
                || Math.abs(grid.z().nearestToOrigin(grid.z().wrap(cellZ))) >= centre.biomesSize()) {
            return;
        }

        into.removeIf(island -> {
            long x = (long) island.canonical().getX() - grid.x().originCopy(island.canonical().getX());
            long z = (long) island.canonical().getZ() - grid.z().originCopy(island.canonical().getZ());
            return x * x + z * z < centre.innerVoidSquared();
        });
        if (options.hasCentralIsland) {
            into.add(new Island(CENTRAL_ISLAND, new BlockPos(grid.x().originCopy(middle(grid.x(), cellX)),
                    CENTRAL_ISLAND_Y, grid.z().originCopy(middle(grid.z(), cellZ)))));
        }
    }

    private static double middle(IslandLapAxis axis, int cell) {
        return (cell + 0.5) * axis.cellBlocks();
    }

    private LapIslandPlacement() {
    }
}

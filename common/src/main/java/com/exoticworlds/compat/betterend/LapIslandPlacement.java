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

    public record Grid(IslandLapAxis x, IslandLapAxis z, LapFrame frame) {
        public int cellX(double blockX, double blockZ) {
            return this.x.cell(this.frame.frameX(blockX, blockZ));
        }

        int reachX() {
            double reach = Math.min(this.x.cellBlocks(), this.z.cellBlocks()) * (1.0 + Math.abs(this.frame.shear()));
            return (int) Math.ceil(reach / this.x.cellBlocks());
        }
    }

    private static final int CENTRAL_ISLAND_Y = 64;

    private static final BlockPos CENTRAL_ISLAND = new BlockPos(0, CENTRAL_ISLAND_Y, 0);

    private static final double COVERAGE_FREQUENCY = 0.01;

    private static final int REACH = 1;

    public static void place(List<Island> into, Grid grid, int cellX, int cellZ, int maxHeight, LayerOptions options,
            IntBinaryOperator seedOf, OpenSimplexNoise coverage, Centre centre) {
        into.clear();
        int reachX = grid.reachX();
        for (int offsetX = -reachX; offsetX <= reachX; offsetX++) {
            int rawX = cellX + offsetX;
            int canonicalX = grid.x().wrap(rawX);
            for (int offsetZ = -REACH; offsetZ <= REACH; offsetZ++) {
                int rawZ = cellZ + offsetZ;
                int canonicalZ = grid.z().wrap(rawZ);
                long[] fromOrigin = cellsFromOrigin(grid, canonicalX, canonicalZ);
                if (fromOrigin[0] * fromOrigin[0] + fromOrigin[1] * fromOrigin[1] <= options.centerDist) {
                    continue;
                }

                RandomSource random = new LegacyRandomSource(seedOf.applyAsInt(canonicalX, canonicalZ));
                double frameX = (canonicalX + random.nextFloat()) * grid.x().cellBlocks();
                double blockY = MHelper.randRange(options.minY, options.maxY, random) * maxHeight;
                double blockZ = (canonicalZ + random.nextFloat()) * grid.z().cellBlocks();
                double blockX = grid.frame().blockX(frameX, blockZ);
                if (coverage.eval(blockX * COVERAGE_FREQUENCY, blockZ * COVERAGE_FREQUENCY) > options.coverage) {
                    BlockPos canonical = new BlockPos((int) blockX, (int) blockY, (int) blockZ);
                    into.add(new Island(canonical, seated(canonical, grid, grid.x().laps(rawX), grid.z().laps(rawZ))));
                }
            }
        }

        clearCentre(into, grid, cellX, cellZ, options, centre);
    }

    private static void clearCentre(List<Island> into, Grid grid, int cellX, int cellZ, LayerOptions options,
            Centre centre) {
        if (!centre.island()) {
            return;
        }

        long[] fromOrigin = cellsFromOrigin(grid, cellX, cellZ);
        if (Math.abs(fromOrigin[0]) >= centre.biomesSize() || Math.abs(fromOrigin[1]) >= centre.biomesSize()) {
            return;
        }

        into.removeIf(island -> {
            BlockPos canonical = island.canonical();
            long[] laps = originLapsNearest(grid, canonical.getX(), canonical.getZ());
            long x = canonical.getX() - grid.frame().shiftX(laps[0], laps[1]);
            long z = canonical.getZ() - grid.frame().shiftZ(laps[1]);
            return x * x + z * z < centre.innerVoidSquared();
        });
        if (options.hasCentralIsland) {
            double middleZ = middle(grid.z(), cellZ);
            long[] laps = originLapsNearest(grid, grid.frame().blockX(middle(grid.x(), cellX), middleZ), middleZ);
            into.add(new Island(CENTRAL_ISLAND, seated(CENTRAL_ISLAND, grid, laps[0], laps[1])));
        }
    }

    private static BlockPos seated(BlockPos canonical, Grid grid, long lapsX, long lapsZ) {
        return canonical.offset(Math.toIntExact(grid.frame().shiftX(lapsX, lapsZ)), 0,
                Math.toIntExact(grid.frame().shiftZ(lapsZ)));
    }

    private static long[] cellsFromOrigin(Grid grid, int cellX, int cellZ) {
        double middleZ = middle(grid.z(), cellZ);
        long[] laps = grid.frame().nearestOriginLaps(grid.frame().blockX(middle(grid.x(), cellX), middleZ), middleZ,
                grid.x().originLapsOfCell(cellX), grid.z().originLapsOfCell(cellZ));
        return new long[] {cellX - laps[0] * grid.x().cells(), cellZ - laps[1] * grid.z().cells()};
    }

    private static long[] originLapsNearest(Grid grid, double blockX, double blockZ) {
        return grid.frame().nearestOriginLaps(blockX, blockZ,
                grid.x().originLaps(grid.frame().frameX(blockX, blockZ)), grid.z().originLaps(blockZ));
    }

    private static double middle(IslandLapAxis axis, int cell) {
        return (cell + 0.5) * axis.cellBlocks();
    }

    private LapIslandPlacement() {
    }
}

package com.exoticworlds.shape.torus;

import com.exoticworlds.accessors.CoastLiftCache;
import com.exoticworlds.core.gen.GenerationMoments;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.shape.noise.DensityNoises;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

public final class CoastFieldLift {
    static final long LAND_FLOOR_BLOCKS = 4096L;

    private static final double[] CANDIDATES = {0.0, 0.05, 0.1, 0.15, 0.2, 0.3, 0.4, 0.6, 0.8, 1.0, 1.3, 1.6};

    private static final int STRIDE_BLOCKS = 16;

    private static final int GRID_CAP = 64;

    private static final int[][] NEIGHBORS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public static void register() {
        GenerationMoments.atRandomState(GuaranteedLand.KEY,
                (randomState, fold, seaLevel) -> solve(randomState, fold, fold.getOptions(), seaLevel));
    }

    private static void solve(RandomState randomState, WorldFold dimensionFold, ShapeOptions options, int seaLevel) {
        if (!options.get(GuaranteedLand.OPTION)) {
            return;
        }

        if (!dimensionFold.isLooped(Direction.Axis.X) || !dimensionFold.isLooped(Direction.Axis.Z)) {
            return;
        }

        WorldFold fold = GenerationFoldContext.getContext().getRouterBuildFold();
        if (fold == null) {
            return;
        }

        NoiseRouter router = randomState.router;
        if (DensityNoises.matching(router.continents(), CoastFields::isCoast).isEmpty()) {
            return;
        }

        searchLift(randomState, router, fold, dimensionFold, seaLevel);
    }

    private static void searchLift(RandomState randomState, NoiseRouter router, WorldFold fold, WorldFold dimensionFold,
            int seaLevel) {
        int xLengthBlocks = dimensionFold.getWidthBlocks(Direction.Axis.X);
        int zLengthBlocks = dimensionFold.getWidthBlocks(Direction.Axis.Z);
        CoastLiftCache lift = (CoastLiftCache) (Object) randomState;
        DensitySampler density = randomState.getSampler(router.finalDensity());
        int strideBlocks = Math.max(STRIDE_BLOCKS, Math.max(xLengthBlocks, zLengthBlocks) / GRID_CAP);
        int xGrid = Math.max(1, xLengthBlocks / strideBlocks);
        int zGrid = Math.max(1, zLengthBlocks / strideBlocks);
        long cellBlocks = (long) strideBlocks * strideBlocks;
        int skewCells = (int) Math.round((double) fold.getBlockLattice().getSkew() / strideBlocks);

        for (double candidate : CANDIDATES) {
            lift.exotic_worlds$setCoastLift(candidate);
            long patch = largestPatch(density, seaLevel, strideBlocks, xGrid, zGrid, skewCells) * cellBlocks;
            if (patch >= LAND_FLOOR_BLOCKS) {
                return;
            }
        }

        lift.exotic_worlds$setCoastLift(CANDIDATES[CANDIDATES.length - 1]);
    }

    private static int largestPatch(DensitySampler density, int seaLevel, int strideBlocks, int xGrid, int zGrid,
            int skewCells) {
        boolean[] land = new boolean[xGrid * zGrid];
        for (int ix = 0; ix < xGrid; ix++) {
            for (int iz = 0; iz < zGrid; iz++) {
                land[ix * zGrid + iz] = density.sampleValue(
                        SamplerContext.EMPTY_UNCACHED, ix * strideBlocks, seaLevel, iz * strideBlocks) > 0.0F;
            }
        }

        return largestComponent(land, xGrid, zGrid, skewCells);
    }

    static int largestComponent(boolean[] land, int xGrid, int zGrid, int skewCells) {
        boolean[] seen = new boolean[land.length];
        int[] queue = new int[land.length];
        int largest = 0;

        for (int start = 0; start < land.length; start++) {
            if (!land[start] || seen[start]) {
                continue;
            }

            largest = Math.max(largest, componentSize(land, seen, queue, start, xGrid, zGrid, skewCells));
        }

        return largest;
    }

    private static int componentSize(boolean[] land, boolean[] seen, int[] queue, int start, int xGrid, int zGrid,
            int skewCells) {
        seen[start] = true;
        queue[0] = start;
        int head = 0;
        int tail = 1;

        while (head < tail) {
            int cell = queue[head++];
            int x = cell / zGrid;
            int z = cell % zGrid;

            for (int[] step : NEIGHBORS) {
                int zLaps = Math.floorDiv(z + step[1], zGrid);
                int next = Math.floorMod(x + step[0] - zLaps * skewCells, xGrid) * zGrid
                        + Math.floorMod(z + step[1], zGrid);
                if (land[next] && !seen[next]) {
                    seen[next] = true;
                    queue[tail++] = next;
                }
            }
        }

        return tail;
    }

    private CoastFieldLift() {
    }
}

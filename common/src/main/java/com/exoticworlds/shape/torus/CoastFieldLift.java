package com.exoticworlds.shape.torus;

import com.exoticworlds.accessors.CoastLiftCache;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.gen.GenerationHooks;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
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

    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public static void register() {
        GenerationHooks.atRandomState(GuaranteedLand.KEY, CoastFieldLift::solve);
    }

    private static void solve(RandomState randomState, ToroidalShape shape, GenerationOptions options, int seaLevel) {
        if (!options.get(GuaranteedLand.OPTION)) {
            return;
        }

        if (!shape.loops(Direction.Axis.X) || !shape.loops(Direction.Axis.Z)) {
            return;
        }

        WorldFold fold = GenerationTransformerContext.context().routerBuildTransformer();
        if (fold == null) {
            return;
        }

        int xLength = shape.widthBlocks(Direction.Axis.X);
        int zLength = shape.widthBlocks(Direction.Axis.Z);

        NoiseRouter router = randomState.router;
        if (DensityNoises.matching(router.continents(), CoastFields::isCoast).isEmpty()) {
            return;
        }

        CoastLiftCache lift = (CoastLiftCache) (Object) randomState;
        DensitySampler density = randomState.getSampler(router.finalDensity());
        int stride = Math.max(STRIDE_BLOCKS, Math.max(xLength, zLength) / GRID_CAP);
        int xGrid = Math.max(1, xLength / stride);
        int zGrid = Math.max(1, zLength / stride);
        long cellBlocks = (long) stride * stride;
        int skewCells = (int) Math.round((double) fold.blockLattice().skew() / stride);

        for (double candidate : CANDIDATES) {
            lift.toroidal$coastLift(candidate);
            long patch = largestPatch(density, seaLevel, stride, xGrid, zGrid, skewCells) * cellBlocks;
            if (patch >= LAND_FLOOR_BLOCKS) {
                return;
            }
        }

        lift.toroidal$coastLift(CANDIDATES[CANDIDATES.length - 1]);
    }

    private static int largestPatch(DensitySampler density, int seaLevel, int stride, int xGrid, int zGrid,
            int skewCells) {
        boolean[] land = new boolean[xGrid * zGrid];
        for (int ix = 0; ix < xGrid; ix++) {
            for (int iz = 0; iz < zGrid; iz++) {
                land[ix * zGrid + iz] = density.sampleValue(
                        SamplerContext.EMPTY_UNCACHED, ix * stride, seaLevel, iz * stride) > 0.0F;
            }
        }

        return largestComponent(land, xGrid, zGrid, skewCells);
    }

    private static int largestComponent(boolean[] land, int xGrid, int zGrid, int skewCells) {
        boolean[] seen = new boolean[land.length];
        int[] queue = new int[land.length];
        int largest = 0;

        for (int start = 0; start < land.length; start++) {
            if (!land[start] || seen[start]) {
                continue;
            }

            seen[start] = true;
            queue[0] = start;
            int head = 0;
            int tail = 1;

            while (head < tail) {
                int cell = queue[head++];
                int x = cell / zGrid;
                int z = cell % zGrid;

                for (int[] step : NEIGHBOURS) {
                    int zLaps = Math.floorDiv(z + step[1], zGrid);
                    int next = Math.floorMod(x + step[0] - zLaps * skewCells, xGrid) * zGrid
                            + Math.floorMod(z + step[1], zGrid);
                    if (land[next] && !seen[next]) {
                        seen[next] = true;
                        queue[tail++] = next;
                    }
                }
            }

            largest = Math.max(largest, tail);
        }

        return largest;
    }

    private CoastFieldLift() {
    }
}

package com.exoticworlds.engine.noise;

import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.noise.BlendedNoiseFixture.Octave;
import com.exoticworlds.engine.noise.LayeredNoise.CorrectedGrids;
import static com.exoticworlds.engine.noise.BlendedNoiseFixture.mix;
import static com.exoticworlds.engine.noise.BlendedNoiseFixture.sample;
import static com.exoticworlds.engine.noise.LayeredNoise.Stats.variance;
import static com.exoticworlds.engine.noise.LayeredNoise.WORLD;
import static com.exoticworlds.engine.noise.LayeredNoise.WORLD_BLOCKS;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

final class OctaveCalibration {
    private static final int CYLINDER_GRID_X = 16;
    private static final int CYLINDER_GRID_Z = 256;
    private static final double CYLINDER_Z_SPAN = 8192.0;
    private static final int VERTICAL_GRID_XZ = 16;
    private static final int VERTICAL_GRID_Y = 8;

    private record Draw(Octave octave, double windowX, double windowZ) {
        static Draw next(int seedIndex, java.util.Random windows) {
            Octave octave = Octave.of(mix(0xCA11B7A7EL + seedIndex * 7919L));
            return new Draw(octave, windows.nextDouble() * 1.0E6, windows.nextDouble() * 1.0E6);
        }
    }

    @SuppressWarnings("ArrayRecordComponent")
    private record PairedGrids(double[] vanilla, double[] wrapped) {
    }

    private record DegenerateSums(double vanilla, double wrapped, double corrected, double vanillaStep,
            double wrappedStep, double correctedStep) {
    }

    private record VerticalRatios(double uncorrected, double corrected) {
    }

    private record WindowMeans(double vanilla, double fold) {
    }

    @SuppressWarnings("ArrayRecordComponent")
    private record MeanSpreads(double[] vanillaMeans, double[] foldMeans, double[] anchors) {
    }

    private final List<double[]> octaveGateRows = new ArrayList<>();
    private final List<double[]> cylinderGateRows = new ArrayList<>();
    private final List<Double> livenessGateRatios = new ArrayList<>();

    List<double[]> octaveGateRows() {
        return this.octaveGateRows;
    }

    List<double[]> cylinderGateRows() {
        return this.cylinderGateRows;
    }

    List<Double> livenessGateRatios() {
        return this.livenessGateRatios;
    }

    void appendDegenerateCalibration(StringBuilder report, LapFloor floor, boolean gates) {
        double[] fractions = {0.125, 0.1875, 0.25, 0.3125, 0.375, 0.4375, 0.5, 0.625, 0.75, 0.875, 1.0, 1.125, 1.25,
                1.375, 1.4375, 1.5, 1.75, 2.0, 2.25, 2.5, 3.0, 4.0, 6.0, 8.0};
        int seeds = 2048;
        int grid = 32;
        java.util.Random windows = new java.util.Random(0x0153E);
        report.append("degenerate-octave calibration, floor ").append(floor.period)
                .append(" cells (single octave, 512-block lap, ")
                .append(seeds).append(" seeds x ").append(grid).append("x").append(grid).append(" grid):\n");
        for (double fraction : fractions) {
            DegenerateSums sums = degenerateSums(fraction / WORLD_BLOCKS, floor, windows, seeds, grid);
            double uncorrectedRatio = Math.sqrt(sums.vanilla() / sums.wrapped());
            double correctedRatio = Math.sqrt(sums.vanilla() / sums.corrected());
            if (gates) {
                this.octaveGateRows.add(new double[] {fraction, uncorrectedRatio, correctedRatio});
            }

            report.append(String.format(
                    "  f=%.3f cells/lap: vanilla rms std %.4f, floored rms std %.4f, ratio k=%.3f, corrected %.3f%n",
                    fraction, Math.sqrt(sums.vanilla() / seeds), Math.sqrt(sums.wrapped() / seeds),
                    uncorrectedRatio, correctedRatio));
            report.append(String.format(
                    "      largest step over the lap: vanilla %.4f, floored %.4f, corrected %.4f,"
                            + " corrected/vanilla %.3f%n",
                    sums.vanillaStep() / seeds, sums.wrappedStep() / seeds, sums.correctedStep() / seeds,
                    sums.correctedStep() / sums.vanillaStep()));
        }
        report.append('\n');
    }

    private DegenerateSums degenerateSums(double scale, LapFloor floor, java.util.Random windows, int seeds,
            int grid) {
        double vanillaSum = 0.0;
        double wrappedSum = 0.0;
        double correctedSum = 0.0;
        double vanillaStepSum = 0.0;
        double wrappedStepSum = 0.0;
        double correctedStepSum = 0.0;
        for (int s = 0; s < seeds; s++) {
            CorrectedGrids grids = squareGrids(Draw.next(s, windows), scale, floor, grid);
            vanillaSum += variance(grids.vanilla());
            wrappedSum += variance(grids.wrapped());
            correctedSum += variance(grids.corrected());
            vanillaStepSum += maxStep(grids.vanilla(), grid);
            wrappedStepSum += maxStep(grids.wrapped(), grid);
            correctedStepSum += maxStep(grids.corrected(), grid);
        }

        return new DegenerateSums(vanillaSum, wrappedSum, correctedSum, vanillaStepSum, wrappedStepSum,
                correctedStepSum);
    }

    private static CorrectedGrids squareGrids(Draw draw, double scale, LapFloor floor, int grid) {
        Octave octave = draw.octave();
        double[] vanillaGrid = new double[grid * grid];
        double[] wrappedGrid = new double[grid * grid];
        double[] correctedGrid = new double[grid * grid];
        int k = 0;
        for (int i = 0; i < grid; i++) {
            double x = i * (WORLD_BLOCKS / (double) grid);
            for (int j = 0; j < grid; j++) {
                double z = j * (WORLD_BLOCKS / (double) grid);
                vanillaGrid[k] = octave.vanilla().noise(PerlinNoise.wrap((draw.windowX() + x) * scale), 0.0,
                        PerlinNoise.wrap((draw.windowZ() + z) * scale), 0.0, 0.0);
                wrappedGrid[k] = sample(octave.permutations(), octave.xo(), octave.yo(),
                        octave.zo(), WORLD, scale, x - 256.0, 0.0, z - 256.0, 0.0, 0.0, -1.0, floor);
                correctedGrid[k] = sample(octave.permutations(), octave.xo(),
                        octave.yo(), octave.zo(), WORLD, scale, x - 256.0, 0.0, z - 256.0, 0.0, 0.0, 0.0,
                        floor);
                k++;
            }
        }

        return new CorrectedGrids(vanillaGrid, wrappedGrid, correctedGrid);
    }

    private static double maxStep(double[] values, int grid) {
        double largest = 0.0;
        for (int i = 0; i < grid; i++) {
            for (int j = 0; j < grid; j++) {
                double here = values[i * grid + j];
                if (i + 1 < grid) {
                    largest = Math.max(largest, Math.abs(values[(i + 1) * grid + j] - here));
                }
                if (j + 1 < grid) {
                    largest = Math.max(largest, Math.abs(values[i * grid + j + 1] - here));
                }
            }
        }
        return largest;
    }

    private double k1d(double f) {
        return f >= 1.5 ? 1.0 : OctaveVarianceCorrection.flat(f);
    }

    void appendRectangularCalibration(StringBuilder report) {
        double[] baseFractions = {0.25, 0.5, 0.75, 1.0, 1.25};
        int[] zChunkSizes = {48, 64, 96, 128};
        int seeds = 1024;
        int grid = 32;
        java.util.Random windows = new java.util.Random(0x0153E);
        report.append("rectangular-world calibration (X lap 512 blocks fixed, ").append(seeds)
                .append(" seeds x ").append(grid).append("x").append(grid).append(" grid):\n");
        for (double fx : baseFractions) {
            for (int zChunks : zChunkSizes) {
                double zBlocks = zChunks * 16.0;
                double scale = fx / WORLD_BLOCKS;
                double fz = zBlocks * scale;
                double measured = rectangularRatio(zChunks, scale, windows, seeds, grid);
                report.append(String.format(
                        "  fx=%.3f fz=%.3f (Z %d chunks): k2D=%.3f | geo-f %.3f, min-f %.3f, geo-k %.3f%n",
                        fx, fz, zChunks, measured,
                        k1d(Math.sqrt(fx * fz)), k1d(Math.min(fx, fz)), Math.sqrt(k1d(fx) * k1d(fz))));
            }
        }
        report.append('\n');
    }

    private double rectangularRatio(int zChunks, double scale, java.util.Random windows, int seeds, int grid) {
        double zBlocks = zChunks * 16.0;
        WorldFold world = WorldFolds.of(
                ShapeFixtures.torus(ShapeFixtures.loopedBounds(-16, 16, -zChunks / 2, zChunks / 2)));
        double vanillaSum = 0.0;
        double wrappedSum = 0.0;
        for (int s = 0; s < seeds; s++) {
            PairedGrids grids = rectangularGrids(Draw.next(s, windows), world, scale, zBlocks, grid);
            vanillaSum += variance(grids.vanilla());
            wrappedSum += variance(grids.wrapped());
        }

        return Math.sqrt(vanillaSum / wrappedSum);
    }

    private static PairedGrids rectangularGrids(Draw draw, WorldFold world, double scale, double zBlocks, int grid) {
        Octave octave = draw.octave();
        double[] vanillaGrid = new double[grid * grid];
        double[] wrappedGrid = new double[grid * grid];
        int k = 0;
        for (int i = 0; i < grid; i++) {
            double x = i * (WORLD_BLOCKS / (double) grid);
            for (int j = 0; j < grid; j++) {
                double z = j * (zBlocks / grid);
                vanillaGrid[k] = octave.vanilla().noise(PerlinNoise.wrap((draw.windowX() + x) * scale), 0.0,
                        PerlinNoise.wrap((draw.windowZ() + z) * scale), 0.0, 0.0);
                wrappedGrid[k] = sample(octave.permutations(), octave.xo(),
                        octave.yo(), octave.zo(), world, scale, x - 256.0, 0.0, z - zBlocks / 2.0, 0.0, 0.0, -1.0);
                k++;
            }
        }

        return new PairedGrids(vanillaGrid, wrappedGrid);
    }

    void appendCylinderCalibration(StringBuilder report) {
        double[] fractions = {0.125, 0.25, 0.375, 0.5, 0.75, 1.0, 1.25, 1.4375};
        int seeds = 1024;
        WorldFold cylinder = WorldFolds.of(ShapeFixtures.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 32)));
        LapFloor[] floors = {LapFloor.FOUR_CELLS, LapFloor.of(cylinder)};
        java.util.Random windows = new java.util.Random(0x0153E);
        report.append("cylinder calibration (X lap 512 blocks, Z unbounded, window ").append((int) CYLINDER_Z_SPAN)
                .append(" blocks of Z, ").append(seeds).append(" seeds x ").append(CYLINDER_GRID_X).append("x")
                .append(CYLINDER_GRID_Z)
                .append(" grid; rms of strip / line / X-step; the last column is the fold's own rule):\n");
        for (double fraction : fractions) {
            double[][] sums = cylinderSums(fraction / WORLD_BLOCKS, cylinder, floors, windows, seeds);
            double[] fold = sums[floors.length];
            this.cylinderGateRows.add(new double[] {
                    fraction, Math.sqrt(sums[0][0] / seeds), Math.sqrt(fold[0] / seeds), Math.sqrt(fold[2] / seeds)});
            StringBuilder line = new StringBuilder(String.format("  f=%.4f cells/lap:", fraction));
            appendStripReadings(line, "vanilla", sums[0], seeds);
            for (int f = 0; f < floors.length; f++) {
                appendStripReadings(line, floors[f].name().toLowerCase(java.util.Locale.ROOT), sums[f + 1], seeds);
            }
            report.append(line).append('\n');
        }
        report.append('\n');
    }

    private double[][] cylinderSums(double scale, WorldFold cylinder, LapFloor[] floors, java.util.Random windows,
            int seeds) {
        double[][] sums = new double[floors.length + 1][3];
        for (int s = 0; s < seeds; s++) {
            double[][][] grids = cylinderGrids(Draw.next(s, windows), scale, cylinder, floors);
            for (int v = 0; v < grids.length; v++) {
                accumulateStrip(grids[v], sums[v]);
            }
        }

        return sums;
    }

    private static double[][][] cylinderGrids(Draw draw, double scale, WorldFold cylinder, LapFloor[] floors) {
        Octave octave = draw.octave();
        double[][][] grids = new double[floors.length + 1][CYLINDER_GRID_X][CYLINDER_GRID_Z];
        for (int i = 0; i < CYLINDER_GRID_X; i++) {
            double x = i * (WORLD_BLOCKS / (double) CYLINDER_GRID_X);
            for (int j = 0; j < CYLINDER_GRID_Z; j++) {
                double z = j * (CYLINDER_Z_SPAN / CYLINDER_GRID_Z);
                grids[0][i][j] = octave.vanilla().noise(PerlinNoise.wrap((draw.windowX() + x) * scale), 0.0,
                        PerlinNoise.wrap((draw.windowZ() + z) * scale), 0.0, 0.0);
                for (int f = 0; f < floors.length; f++) {
                    grids[f + 1][i][j] = sample(octave.permutations(), octave.xo(), octave.yo(),
                            octave.zo(), cylinder, scale, x - 256.0, 0.0, draw.windowZ() + z, 0.0, 0.0, -1.0,
                            floors[f]);
                }
            }
        }

        return grids;
    }

    private void accumulateStrip(double[][] grid, double[] sums) {
        int gridX = grid.length;
        int gridZ = grid[0].length;
        double[] flat = new double[gridX * gridZ];
        double lineVariance = 0.0;
        double stepSum = 0.0;
        int k = 0;
        for (int i = 0; i < gridX; i++) {
            lineVariance += variance(grid[i]);
            for (int j = 0; j < gridZ; j++) {
                flat[k++] = grid[i][j];
                if (i + 1 < gridX) {
                    double step = grid[i + 1][j] - grid[i][j];
                    stepSum += step * step;
                }
            }
        }
        sums[0] += variance(flat);
        sums[1] += lineVariance / gridX;
        sums[2] += stepSum / ((gridX - 1) * gridZ);
    }

    private void appendStripReadings(StringBuilder line, String label, double[] sums, int seeds) {
        line.append(String.format("  %s %.4f/%.4f/%.5f", label,
                Math.sqrt(sums[0] / seeds), Math.sqrt(sums[1] / seeds), Math.sqrt(sums[2] / seeds)));
    }

    void appendVerticalCalibration(StringBuilder report, LapFloor floor, boolean gates) {
        double[] fractions = {0.125, 0.25, 0.5, 0.75, 1.0, 1.25};
        double[] verticalCells = {0.0, 0.25, 0.5, 1.0, 2.0, 4.0, 8.0, 16.0};
        int seeds = 512;
        java.util.Random windows = new java.util.Random(0x0153E);
        report.append("vertical-liveness calibration, floor ").append(floor.period)
                .append(" cells (512-block square lap, ").append(seeds)
                .append(" seeds x ").append(VERTICAL_GRID_XZ).append("x").append(VERTICAL_GRID_XZ).append("x")
                .append(VERTICAL_GRID_Y)
                .append(" grid, nu = vertical cells in window):\n");
        for (double fraction : fractions) {
            double scale = fraction / WORLD_BLOCKS;
            StringBuilder line = new StringBuilder(String.format("  f=%.3f cells/lap:", fraction));
            for (double nu : verticalCells) {
                VerticalRatios ratios = verticalRatios(scale, nu, floor, windows, seeds);
                if (gates) {
                    this.livenessGateRatios.add(ratios.corrected());
                }
                line.append(String.format("  nu=%.2f k=%.3f c=%.3f", nu, ratios.uncorrected(),
                        ratios.corrected()));
            }
            report.append(line).append('\n');
        }
        report.append('\n');
    }

    private VerticalRatios verticalRatios(double scale, double nu, LapFloor floor, java.util.Random windows,
            int seeds) {
        double vanillaSum = 0.0;
        double wrappedSum = 0.0;
        double correctedSum = 0.0;
        for (int s = 0; s < seeds; s++) {
            Draw draw = Draw.next(s, windows);
            double windowY = windows.nextDouble() * 256.0;
            CorrectedGrids grids = verticalGrids(draw, windowY, scale, nu, floor);
            vanillaSum += variance(grids.vanilla());
            wrappedSum += variance(grids.wrapped());
            correctedSum += variance(grids.corrected());
        }

        return new VerticalRatios(Math.sqrt(vanillaSum / wrappedSum), Math.sqrt(vanillaSum / correctedSum));
    }

    private static CorrectedGrids verticalGrids(Draw draw, double windowY, double scale, double nu, LapFloor floor) {
        Octave octave = draw.octave();
        int ySteps = nu == 0.0 ? 1 : VERTICAL_GRID_Y;
        double verticalShare = nu / (384.0 * scale);
        double[] vanillaGrid = new double[VERTICAL_GRID_XZ * VERTICAL_GRID_XZ * ySteps];
        double[] wrappedGrid = new double[VERTICAL_GRID_XZ * VERTICAL_GRID_XZ * ySteps];
        double[] correctedGrid = new double[VERTICAL_GRID_XZ * VERTICAL_GRID_XZ * ySteps];
        int k = 0;
        for (int i = 0; i < VERTICAL_GRID_XZ; i++) {
            double x = i * (WORLD_BLOCKS / (double) VERTICAL_GRID_XZ);
            for (int j = 0; j < VERTICAL_GRID_XZ; j++) {
                double z = j * (WORLD_BLOCKS / (double) VERTICAL_GRID_XZ);
                for (int t = 0; t < ySteps; t++) {
                    double y = windowY + t * (nu / VERTICAL_GRID_Y);
                    vanillaGrid[k] = octave.vanilla().noise(PerlinNoise.wrap((draw.windowX() + x) * scale),
                            PerlinNoise.wrap(y), PerlinNoise.wrap((draw.windowZ() + z) * scale), 0.0, 0.0);
                    wrappedGrid[k] = sample(octave.permutations(), octave.xo(),
                            octave.yo(), octave.zo(), WORLD, scale, x - 256.0, y, z - 256.0, 0.0, 0.0,
                            -1.0, floor);
                    correctedGrid[k] = sample(octave.permutations(), octave.xo(),
                            octave.yo(), octave.zo(), WORLD, scale, x - 256.0, y, z - 256.0, 0.0, 0.0,
                            verticalShare, floor);
                    k++;
                }
            }
        }

        return new CorrectedGrids(vanillaGrid, wrappedGrid, correctedGrid);
    }

    void appendMeanSpreadCalibration(StringBuilder report, LapFloor floor) {
        double[] fractions = {0.125, 0.1875, 0.25, 0.3125, 0.375, 0.4375, 0.5, 0.625, 0.75, 0.875, 1.0, 1.125, 1.25,
                1.375, 1.4375, 1.5, 2.0};
        int seeds = 2048;
        int grid = 32;
        java.util.Random windows = new java.util.Random(0x0153E);
        report.append("mean-spread calibration, floor ").append(floor.period)
                .append(" cells (std across ").append(seeds)
                .append(" seeds of window/world mean, ").append(grid).append("x").append(grid).append(" grid):\n");
        for (double fraction : fractions) {
            MeanSpreads spreads = meanSpreads(fraction / WORLD_BLOCKS, floor, windows, seeds, grid);
            report.append(String.format(
                    "  f=%.3f cells/lap: vanilla mean-spread %.4f, fold mean-spread %.4f, anchor spread %.4f%n",
                    fraction, Math.sqrt(variance(spreads.vanillaMeans())), Math.sqrt(variance(spreads.foldMeans())),
                    Math.sqrt(variance(spreads.anchors()))));
        }
        report.append('\n');
    }

    private static MeanSpreads meanSpreads(double scale, LapFloor floor, java.util.Random windows, int seeds,
            int grid) {
        double[] vanillaMeans = new double[seeds];
        double[] foldMeans = new double[seeds];
        double[] anchors = new double[seeds];
        for (int s = 0; s < seeds; s++) {
            Draw draw = Draw.next(s, windows);
            WindowMeans means = windowMeans(draw, scale, floor, grid);
            vanillaMeans[s] = means.vanilla();
            foldMeans[s] = means.fold();
            Octave octave = draw.octave();
            anchors[s] = sample(octave.permutations(), octave.xo(), octave.yo(),
                    octave.zo(), WORLD, scale, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, floor);
        }

        return new MeanSpreads(vanillaMeans, foldMeans, anchors);
    }

    private static WindowMeans windowMeans(Draw draw, double scale, LapFloor floor, int grid) {
        Octave octave = draw.octave();
        double vanillaSum = 0.0;
        double foldSum = 0.0;
        for (int i = 0; i < grid; i++) {
            double x = i * (WORLD_BLOCKS / (double) grid);
            for (int j = 0; j < grid; j++) {
                double z = j * (WORLD_BLOCKS / (double) grid);
                vanillaSum += octave.vanilla().noise(PerlinNoise.wrap((draw.windowX() + x) * scale), 0.0,
                        PerlinNoise.wrap((draw.windowZ() + z) * scale), 0.0, 0.0);
                foldSum += sample(octave.permutations(), octave.xo(), octave.yo(),
                        octave.zo(), WORLD, scale, x - 256.0, 0.0, z - 256.0, 0.0, 0.0, -1.0, floor);
            }
        }

        return new WindowMeans(vanillaSum / (grid * grid), foldSum / (grid * grid));
    }
}

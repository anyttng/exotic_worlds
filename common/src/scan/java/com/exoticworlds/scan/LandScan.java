package com.exoticworlds.scan;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.noise.ClimateScanFixture;
import com.exoticworlds.engine.noise.ClimateScanFixture.WorldType;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.scan.ClimateScan.Width;
import static com.exoticworlds.engine.noise.ClimateScanFixture.SEED_BASE;
import static com.exoticworlds.engine.noise.ClimateScanFixture.TYPES;
import static com.exoticworlds.engine.noise.ClimateScanFixture.guaranteedTorusOfWidth;
import static com.exoticworlds.engine.noise.ClimateScanFixture.randomState;
import static com.exoticworlds.engine.noise.ClimateScanFixture.settingsOf;
import static com.exoticworlds.engine.noise.ClimateScanFixture.torusOfWidth;
import static com.exoticworlds.scan.ClimateScan.GRID;
import static com.exoticworlds.scan.ClimateScan.SEEDS;
import static com.exoticworlds.scan.ClimateScan.SEED_STEP;
import static com.exoticworlds.scan.ClimateScan.WIDTHS;
import static com.exoticworlds.scan.ClimateScan.isLand;
import static com.exoticworlds.scan.ClimateScan.mean;
import static com.exoticworlds.scan.ClimateScan.standardDeviation;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;

class LandScan {
    private static final String SCAN = "land";

    private static final int LAND_GRID = 32;

    static final int LAND_SEEDS = 32;

    private static final int LAP_SEEDS = 16;

    private static final double USABLE_LAND_SHARE = 0.20;

    private static final int PATCH_STRIDE_BLOCKS = 16;

    private static final int PATCH_GRID_CAP = 128;

    private static final int PATCH_MAX_WIDTH_BLOCKS = 4096;

    private static final long LAND_PATCH_FLOOR_BLOCKS = 4096L;

    private static final double OCEAN_CONTINENTALNESS = -0.19;

    private static final int[][] PATCH_NEIGHBORS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private static final Path LAND_REPORT = ScanReports.DIRECTORY.resolve("land-scan.txt");

    private static final Path PATCH_REPORT = ScanReports.DIRECTORY.resolve("land-patch-scan.txt");

    @BeforeAll
    static void bootstrapVanilla() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void reportsHowOftenALapComesOutWithNowhereToStand() {
        StringBuilder report = new StringBuilder();
        report.append("Land scan - vanilla final density at the world type's sea level through the real router,")
                .append(" no chunk generation.").append(System.lineSeparator())
                .append("Grid ").append(LAND_GRID).append("x").append(LAND_GRID).append(" points over one lap, ")
                .append(LAND_SEEDS).append(" seeds per row.").append(System.lineSeparator())
                .append("mean64 = the climate scan's own land reading of the same rows - the mean over ")
                .append(SEEDS).append(" seeds on its ").append(GRID).append("x").append(GRID)
                .append(" grid, against this scan's median over ").append(LAND_SEEDS).append(" on ")
                .append(LAND_GRID).append("x").append(LAND_GRID).append(". gap is the distance between them,")
                .append(" and a gap wider than this row's own spread (med - min) is listed under the table.")
                .append(" Same criterion, same sea level, two populations and two resolutions: the listed rows")
                .append(" are where one of the two must not be quoted alone. Reported, never gated - a scan")
                .append(" measures.").append(System.lineSeparator())
                .append("land = area fraction standing above the water; a cave at sea level reads as water, so")
                .append(" the share is the low estimate.").append(System.lineSeparator())
                .append("The control binds WorldFolds.NOOP, so it measures the same window of an unbounded")
                .append(" vanilla world - the nether has no sea level to stand on and is left out.")
                .append(System.lineSeparator())
                .append("under = seeds whose land share falls below ")
                .append(String.format(Locale.ROOT, "%.0f%%", USABLE_LAND_SHARE * 100))
                .append("; best seed is the one to hand a round that needs land.")
                .append(System.lineSeparator())
                .append("Criterion: the distribution clause. The only assertion is a blindness guard - no")
                .append(" median, minimum or under-count here is a gate.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        List<String> blind = new ArrayList<>();
        List<String> apart = new ArrayList<>();

        for (WorldType type : TYPES) {
            if (type.nether()) {
                continue;
            }

            report.append("  ").append(type.name()).append(System.lineSeparator());
            report.append(String.format(Locale.ROOT, "    %-8s %-14s %17s %17s%n", "", "", "torus", "control"));
            report.append(String.format(Locale.ROOT, "    %-8s %-14s %5s %5s %5s %5s %5s %5s %6s %5s %20s%n",
                    "width", "blocks", "med", "min", "under", "med", "min", "under", "mean64", "gap",
                    "best seed"));

            for (Width width : WIDTHS) {
                int widthBlocks = width.widthBlocks(type);
                Land torus = landDistribution(type, widthBlocks, torusOfWidth(widthBlocks));
                Land control = landDistribution(type, widthBlocks, WorldFolds.NOOP);

                double mean = meanLandShare(type, torusOfWidth(widthBlocks), widthBlocks);
                double spread = torus.median() - torus.min();
                double gap = Math.abs(mean - torus.median());

                report.append(String.format(Locale.ROOT, "    %-8s %-14s %5.2f %5.2f %5d %5.2f %5.2f %5d %6.2f %5.2f %20d%n",
                        width.id(), widthBlocks + " blocks",
                        torus.median(), torus.min(), torus.under(),
                        control.median(), control.min(), control.under(),
                        mean, gap, torus.bestSeed()));

                if (control.under() == LAND_SEEDS) {
                    blind.add(type.name() + " " + width.id());
                }

                if (gap > spread) {
                    apart.add(String.format(Locale.ROOT, "%s %s: mean64 %.2f against median %.2f, spread %.2f",
                            type.name(), width.id(), mean, torus.median(), spread));
                }
            }

            report.append(System.lineSeparator());
        }

        report.append("Cross-check: ").append(apart.isEmpty()
                ? "every row's two readings agree inside the torus spread."
                : "rows whose two readings fall further apart than the torus spread - "
                        + String.join("; ", apart))
                .append(System.lineSeparator());

        ScanReports.write(LAND_REPORT, ScanReports.population(LAND_SEEDS, SEED_BASE, SEED_STEP,
                "the cheapest reading per seed in the set, so it carries the largest sample"),
                report.toString());

        assertTrue(blind.isEmpty(),
                "no control seed clears the land floor, so the scan measures nothing: " + blind);
    }

    private record Land(double median, double min, int under, long bestSeed) {
    }

    private static Land landDistribution(WorldType type, int widthBlocks, WorldFold fold) {
        double[] shares = ScanSeeds.inOrder(LAND_SEEDS, s -> {
            long seed = SEED_BASE + s * SEED_STEP;
            double share = landShare(type, fold, widthBlocks, seed, LAND_GRID);
            ScanReports.note(SCAN, "land", "type=" + type.name() + " width=" + widthBlocks + " seed=" + seed
                    + " share=" + ScanReports.value(share));
            return share;
        }).stream().mapToDouble(Double::doubleValue).toArray();
        long bestSeed = SEED_BASE;
        double best = -1.0;
        int under = 0;

        for (int s = 0; s < LAND_SEEDS; s++) {
            long seed = SEED_BASE + s * SEED_STEP;
            if (shares[s] > best) {
                best = shares[s];
                bestSeed = seed;
            }

            if (shares[s] < USABLE_LAND_SHARE) {
                under++;
            }
        }

        double[] sorted = shares.clone();
        Arrays.sort(sorted);
        double median = (sorted[(sorted.length - 1) / 2] + sorted[sorted.length / 2]) / 2.0;
        return new Land(median, sorted[0], under, bestSeed);
    }

    private static double meanLandShare(WorldType type, WorldFold fold, int widthBlocks) {
        List<Double> shares = ScanSeeds.inOrder(SEEDS, s -> {
            long seed = SEED_BASE + s * SEED_STEP;
            double share = landShare(type, fold, widthBlocks, seed, GRID);
            ScanReports.note(SCAN, "cross", "type=" + type.name() + " width=" + widthBlocks + " seed=" + seed
                    + " share=" + ScanReports.value(share));
            return share;
        });

        double total = 0.0;
        for (double share : shares) {
            total += share;
        }

        return total / SEEDS;
    }

    private static double landShare(WorldType type, WorldFold fold, int widthBlocks, long seed, int grid) {
        RandomState randomState = randomState(type, fold, seed);
        DensityFunction density = randomState.router().finalDensity();
        int seaLevel = settingsOf(type).seaLevel();
        double step = widthBlocks / (double) grid;
        int[] land = new int[1];

        GenerationFoldContext.runWithFold(fold, () -> {
            for (int ix = 0; ix < grid; ix++) {
                for (int iz = 0; iz < grid; iz++) {
                    if (isLand(density, (int) Math.round(ix * step), seaLevel, (int) Math.round(iz * step))) {
                        land[0]++;
                    }
                }
            }
        });

        return land[0] / (double) (grid * grid);
    }

    @Test
    void reportsTheLargestPatchOfLandOnALap() {
        StringBuilder report = new StringBuilder();
        report.append("Largest land patch - the biggest run of land connected across the lap, seam included,")
                .append(" off the same final density at sea level as the land scan.")
                .append(System.lineSeparator())
                .append("Sampled every ").append(PATCH_STRIDE_BLOCKS).append(" blocks up to a ")
                .append(PATCH_GRID_CAP).append("x").append(PATCH_GRID_CAP)
                .append(" grid, so the step is one chunk while it fits and coarser above; ")
                .append(LAP_SEEDS).append(" seeds per row, torus only.").append(System.lineSeparator())
                .append("Areas are blocks squared; empty = seeds whose lap carries no land at all.")
                .append(System.lineSeparator())
                .append("Widths past ").append(PATCH_MAX_WIDTH_BLOCKS)
                .append(" blocks are left out - no seed there falls under the land floor.")
                .append(System.lineSeparator())
                .append("corr = Pearson correlation between the lap's mean continentalness and its patch area.")
                .append(" fail/pass are the highest mean still under the ")
                .append(LAND_PATCH_FLOOR_BLOCKS).append(" blocks² floor and the lowest mean clearing it, so")
                .append(" they bracket the mean a lift has to reach; vanilla puts ocean under ")
                .append(OCEAN_CONTINENTALNESS).append(".").append(System.lineSeparator())
                .append("spread = mean of the per-seed standard deviation of continentalness over the lap.")
                .append(System.lineSeparator())
                .append("Criterion: the distribution clause. The empty-lap check is a blindness guard and the")
                .append(" floor check a feature gate on the land guarantee; the medians, minima and")
                .append(" correlation are readings.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        List<String> blind = new ArrayList<>();
        List<String> unguarded = new ArrayList<>();

        for (WorldType type : TYPES) {
            if (type.nether()) {
                continue;
            }

            report.append("  ").append(type.name()).append(System.lineSeparator());
            report.append(String.format(Locale.ROOT, "    %-8s %-14s %25s %25s %7s%n", "", "", "off", "on", ""));
            report.append(String.format(Locale.ROOT, "    %-8s %-14s %12s %10s %4s %12s %10s %4s %7s%n",
                    "width", "blocks", "med", "min", "bad", "med", "min", "bad", "corr"));

            for (Width width : WIDTHS) {
                int widthBlocks = width.widthBlocks(type);
                if (widthBlocks > PATCH_MAX_WIDTH_BLOCKS) {
                    continue;
                }

                Patch off = patchOf(probeLaps(type, widthBlocks, torusOfWidth(widthBlocks)));
                Patch on = patchOf(probeLaps(type, widthBlocks, guaranteedTorusOfWidth(widthBlocks)));

                report.append(String.format(Locale.ROOT, "    %-8s %-14s %12.0f %10d %4d %12.0f %10d %4d %7s%n",
                        width.id(), widthBlocks + " blocks",
                        off.median(), off.min(), off.bad(),
                        on.median(), on.min(), on.bad(),
                        cell(off.correlation(), "%.2f")));

                if (off.max() == 0L) {
                    blind.add(type.name() + " " + width.id());
                }

                if (on.bad() > 0) {
                    unguarded.add(String.format(Locale.ROOT, "%s %s %d/%d", type.name(), width.id(), on.bad(), LAP_SEEDS));
                }
            }

            report.append(System.lineSeparator());
        }

        ScanReports.write(PATCH_REPORT, ScanReports.population(LAP_SEEDS, SEED_BASE, SEED_STEP,
                "the most expensive reading per seed in this scan, so it carries the smallest sample"),
                report.toString());

        assertTrue(blind.isEmpty(), "no seed of a row carries any land, so the scan measures nothing: " + blind);
        assertTrue(unguarded.isEmpty(),
                "the guarantee left seeds under " + LAND_PATCH_FLOOR_BLOCKS + " blocks squared: " + unguarded);
    }

    private record Patch(double median, long min, long max, int bad, double correlation) {
    }

    private static Patch patchOf(LapProbe[] probes) {
        long[] sorted = new long[probes.length];
        double[] means = new double[probes.length];
        double[] areas = new double[probes.length];
        int bad = 0;

        for (int i = 0; i < probes.length; i++) {
            sorted[i] = probes[i].patchBlocks();
            means[i] = probes[i].continentalnessMean();
            areas[i] = probes[i].patchBlocks();
            if (probes[i].patchBlocks() < LAND_PATCH_FLOOR_BLOCKS) {
                bad++;
            }
        }

        Arrays.sort(sorted);
        double median = (sorted[(sorted.length - 1) / 2] + sorted[sorted.length / 2]) / 2.0;
        return new Patch(median, sorted[0], sorted[sorted.length - 1], bad, correlation(means, areas));
    }

    private record LapProbe(long patchBlocks, double continentalnessMean, double continentalnessSpread) {
    }

    private static LapProbe[] probeLaps(WorldType type, int widthBlocks, WorldFold fold) {
        return ScanSeeds.inOrder(LAP_SEEDS, s -> {
            long seed = SEED_BASE + s * SEED_STEP;
            LapProbe probe = probeLap(type, fold, widthBlocks, seed);
            ScanReports.note(SCAN, "lap", "type=" + type.name() + " width=" + widthBlocks + " seed=" + seed
                    + " continentalness=" + ScanReports.value(probe.continentalnessMean()));
            return probe;
        }).toArray(LapProbe[]::new);
    }

    private static LapProbe probeLap(WorldType type, WorldFold fold, int widthBlocks, long seed) {
        int stride = Math.max(PATCH_STRIDE_BLOCKS, widthBlocks / PATCH_GRID_CAP);
        int grid = widthBlocks / stride;
        RandomState randomState = randomState(type, fold, seed);
        DensityFunction density = randomState.router().finalDensity();
        DensityFunction continents = randomState.router().continents();
        int seaLevel = settingsOf(type).seaLevel();
        boolean[] land = new boolean[grid * grid];
        double[] field = new double[grid * grid];

        GenerationFoldContext.runWithFold(fold, () -> {
            for (int ix = 0; ix < grid; ix++) {
                for (int iz = 0; iz < grid; iz++) {
                    int blockX = ix * stride;
                    int blockZ = iz * stride;
                    land[ix * grid + iz] = isLand(density, blockX, seaLevel, blockZ);
                    field[ix * grid + iz] =
                            continents.compute(new DensityFunction.SinglePointContext(blockX, seaLevel, blockZ));
                }
            }
        });

        return new LapProbe((long) largestComponent(land, grid) * stride * stride, mean(field),
                standardDeviation(field));
    }

    private static String cell(double value, String format) {
        return Double.isNaN(value) ? "n/a" : String.format(Locale.ROOT, format, value);
    }

    private static double correlation(double[] xs, double[] ys) {
        double meanX = mean(xs);
        double meanY = mean(ys);
        double covariance = 0.0;
        double varianceX = 0.0;
        double varianceY = 0.0;

        for (int i = 0; i < xs.length; i++) {
            double dx = xs[i] - meanX;
            double dy = ys[i] - meanY;
            covariance += dx * dy;
            varianceX += dx * dx;
            varianceY += dy * dy;
        }

        return varianceX == 0.0 || varianceY == 0.0 ? Double.NaN : covariance / Math.sqrt(varianceX * varianceY);
    }

    private static int largestComponent(boolean[] land, int grid) {
        boolean[] seen = new boolean[land.length];
        int[] queue = new int[land.length];
        int largest = 0;

        for (int start = 0; start < land.length; start++) {
            if (land[start] && !seen[start]) {
                largest = Math.max(largest, componentSize(land, grid, start, seen, queue));
            }
        }

        return largest;
    }

    private static int componentSize(boolean[] land, int grid, int start, boolean[] seen, int[] queue) {
        seen[start] = true;
        queue[0] = start;
        int head = 0;
        int tail = 1;

        while (head < tail) {
            int cell = queue[head++];
            int x = cell / grid;
            int z = cell % grid;

            for (int[] step : PATCH_NEIGHBORS) {
                int next = Math.floorMod(x + step[0], grid) * grid + Math.floorMod(z + step[1], grid);
                if (land[next] && !seen[next]) {
                    seen[next] = true;
                    queue[tail++] = next;
                }
            }
        }

        return tail;
    }
}

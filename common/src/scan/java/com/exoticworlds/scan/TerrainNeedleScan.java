package com.exoticworlds.scan;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.noise.ClimateScanFixture;
import com.exoticworlds.engine.noise.ClimateScanFixture.WorldType;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.shape.WorldLoopPresets;
import static com.exoticworlds.engine.noise.ClimateScanFixture.SEED_BASE;
import static com.exoticworlds.engine.noise.ClimateScanFixture.TYPES;
import static com.exoticworlds.engine.noise.ClimateScanFixture.randomState;
import static com.exoticworlds.engine.noise.ClimateScanFixture.settingsOf;
import static com.exoticworlds.engine.noise.ClimateScanFixture.torusOfWidth;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

class TerrainNeedleScan {
    private static final String SCAN = "terrain-needle";

    private static final int WORST_SITES = 12;

    private static final int NEEDLE_WINDOW_BLOCKS = 48;

    private static final int NEEDLE_SEEDS = 10;

    private static final long NEEDLE_SEED_STEP = 0x9E3779B97F4A7C15L;

    private static final int NEEDLE_DROP_BLOCKS = 16;

    private static final int COARSE_STEP_BLOCKS = 8;

    private static final int NO_COLUMN = Integer.MIN_VALUE;

    private static final int[][] NEEDLE_NEIGHBORS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private static final String NEEDLE_KIND = "needle";

    private static final String PIT_KIND = "pit";

    private static final Path NEEDLE_REPORT = ScanReports.DIRECTORY.resolve("terrain-needle-scan.txt");

    private record Needles(int needles, int worstDrop, int pits) {
        Needles plus(Needles other) {
            return new Needles(this.needles + other.needles, Math.max(this.worstDrop, other.worstDrop),
                    this.pits + other.pits);
        }
    }

    private record NeedleRow(int seedsWithNeedle, int needles, int worstDrop, int pits, Jumps jumps) {
    }

    private record SeedNeedles(Needles needles, List<Spike> sites, IntArrayList steps) {
    }

    private record NeedleWindow(RandomState state, NoiseSettings noiseSettings, int originX, int originZ,
            String lap, long seed) {
        Spike spike(int dx, int blockY, int dz, String kind, int drop) {
            return TerrainNeedleScan.spike(this.state, this.lap, this.seed, this.originX + dx, blockY,
                    this.originZ + dz, kind, drop);
        }
    }

    private record Spike(String lap, long seed, int blockX, int blockY, int blockZ, String kind, int drop,
            double continents, double erosion, double ridges, double depth) {
    }

    private record Jumps(int p50, int p90, int p99, int p999, int max) {
        static Jumps of(IntArrayList steps) {
            int[] sorted = steps.toIntArray();
            Arrays.sort(sorted);
            return new Jumps(at(sorted, 0.50), at(sorted, 0.90), at(sorted, 0.99), at(sorted, 0.999),
                    sorted[sorted.length - 1]);
        }

        private static int at(int[] sorted, double fraction) {
            return sorted[Math.min(sorted.length - 1, (int) (sorted.length * fraction))];
        }
    }

    @BeforeAll
    static void bootstrapVanilla() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void countsTheNeedlesEveryPresetLeavesOnItsLap() {
        WorldType type = TYPES.getFirst();
        NoiseSettings noiseSettings = settingsOf(type).noiseSettings();

        StringBuilder report = new StringBuilder();
        report.append("Needle scan - the highest solid block of every column, walked down the real final density,")
                .append(" no chunk generation.").append(System.lineSeparator())
                .append("A needle is a column standing at least ").append(NEEDLE_DROP_BLOCKS)
                .append(" blocks above all four of its neighbors; the blade metric of the wall scan reads the")
                .append(" generator's ").append(TerrainWallScan.CELL_WIDTH)
                .append("-block corner lattice and cannot see one.")
                .append(System.lineSeparator())
                .append("Per preset: ").append(NEEDLE_SEEDS).append(" seeds, one ").append(NEEDLE_WINDOW_BLOCKS)
                .append("x").append(NEEDLE_WINDOW_BLOCKS).append("-block window in each quadrant of the lap.")
                .append(System.lineSeparator())
                .append("The column is walked from the top of the dimension down in ").append(COARSE_STEP_BLOCKS)
                .append("-block steps and refined to the block, so a solid layer thinner than one step is")
                .append(" reported only where it is the highest one.").append(System.lineSeparator())
                .append("pits = the same test downward: columns sitting that far below all four neighbors.")
                .append(" Reported, not gated - ordinary seabed relief clears the same threshold.")
                .append(System.lineSeparator())
                .append("The control binds WorldFolds.NOOP, so it reads the same windows of an unbounded vanilla")
                .append(" world - the same seed, the same coordinates, no fold.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        report.append("The gate is the distribution: every adjacent-column height step over the windows, torus")
                .append(" against control. No percentile up to p999 may run more than one needle (")
                .append(NEEDLE_DROP_BLOCKS).append(" blocks) past the control's; max is one column and swings")
                .append(" both ways, so it is printed and not gated. The needle and pit counts under it are")
                .append(" outliers of the same numbers, reported so a row can be followed to a place.")
                .append(System.lineSeparator())
                .append("Criterion: the distribution clause on the percentiles, which are a distribution gate")
                .append(" against the unbounded control, and the per-site-count clause on the needle and pit")
                .append(" counts, which are readings.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        report.append(String.format(Locale.ROOT, "    %-8s %-14s %29s %29s%n", "", "", "torus", "control"));
        report.append(String.format(Locale.ROOT, "    %-8s %-14s %5s %5s %5s %5s %5s %5s %5s %5s %5s %5s%n",
                "preset", "blocks", "p50", "p90", "p99", "p999", "max", "p50", "p90", "p99", "p999", "max"));

        List<Spike> sites = new ArrayList<>();
        List<NeedleRow> torusRows = new ArrayList<>();
        List<NeedleRow> controlRows = new ArrayList<>();
        List<String> widened = new ArrayList<>();

        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            int widthBlocks = preset.blockWidth();
            NeedleRow torus = needleRow(type, noiseSettings, widthBlocks, true, preset.id(), sites);
            NeedleRow control = needleRow(type, noiseSettings, widthBlocks, false, preset.id(), sites);
            torusRows.add(torus);
            controlRows.add(control);
            widened.addAll(overrun(preset.id(), torus.jumps(), control.jumps()));

            report.append(String.format(Locale.ROOT, "    %-8s %-14s %5d %5d %5d %5d %5d %5d %5d %5d %5d %5d%n",
                    preset.id(), widthBlocks + " blocks",
                    torus.jumps().p50(), torus.jumps().p90(), torus.jumps().p99(), torus.jumps().p999(),
                    torus.jumps().max(),
                    control.jumps().p50(), control.jumps().p90(), control.jumps().p99(),
                    control.jumps().p999(), control.jumps().max()));
        }

        report.append(System.lineSeparator())
                .append("Outliers of the same windows, ").append(NEEDLE_SEEDS).append(" seeds per preset:")
                .append(System.lineSeparator());
        report.append(String.format(Locale.ROOT, "    %-8s %-14s %23s %23s%n", "", "", "torus", "control"));
        report.append(String.format(Locale.ROOT, "    %-8s %-14s %5s %7s %5s %6s %5s %7s %5s %6s%n",
                "preset", "blocks", "seeds", "needles", "worst", "pits", "seeds", "needles", "worst", "pits"));

        int row = 0;
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            NeedleRow torus = torusRows.get(row);
            NeedleRow control = controlRows.get(row++);
            report.append(String.format(Locale.ROOT, "    %-8s %-14s %5d %7d %5d %6d %5d %7d %5d %6d%n",
                    preset.id(), preset.blockWidth() + " blocks",
                    torus.seedsWithNeedle(), torus.needles(), torus.worstDrop(), torus.pits(),
                    control.seedsWithNeedle(), control.needles(), control.worstDrop(), control.pits()));
        }

        report.append(System.lineSeparator())
                .append("seeds = seeds carrying at least one needle; needles = the count over every window;")
                .append(" worst = the largest drop to a neighbor.")
                .append(System.lineSeparator());

        appendSites(report, sites);
        ScanReports.write(NEEDLE_REPORT, ScanReports.population(NEEDLE_SEEDS, SEED_BASE, NEEDLE_SEED_STEP,
                "seconds per seed, so ten is the ceiling the set affords over ten preset rows"),
                report.toString());

        assertTrue(widened.isEmpty(), "a folded lap steps higher between neighbors than its control: " + widened);
    }

    private static List<String> overrun(String presetId, Jumps torus, Jumps control) {
        List<String> over = new ArrayList<>();
        int[][] pairs = {
                {torus.p50(), control.p50()}, {torus.p90(), control.p90()},
                {torus.p99(), control.p99()}, {torus.p999(), control.p999()}};
        String[] names = {"p50", "p90", "p99", "p999"};

        for (int i = 0; i < pairs.length; i++) {
            if (pairs[i][0] - pairs[i][1] > NEEDLE_DROP_BLOCKS) {
                over.add(presetId + " " + names[i] + ": torus " + pairs[i][0] + " blocks, control "
                        + pairs[i][1]);
            }
        }

        return over;
    }

    private static void appendSites(StringBuilder report, List<Spike> sites) {
        appendSites(report, sites, NEEDLE_KIND, "Needles, largest drop first:");
        appendSites(report, sites, PIT_KIND, "Pits, largest drop first:");
    }

    private static void appendSites(StringBuilder report, List<Spike> sites, String kind, String title) {
        List<Spike> ofKind = sites.stream()
                .filter(spike -> spike.kind().equals(kind))
                .sorted(Comparator.comparingInt(Spike::drop).reversed())
                .limit(WORST_SITES)
                .toList();
        if (ofKind.isEmpty()) {
            return;
        }

        report.append(System.lineSeparator()).append(title).append(System.lineSeparator());
        report.append(String.format(Locale.ROOT, "    %-16s %-18s %5s %10s %9s %9s %9s %22s%n",
                "lap", "x y z", "drop", "continents", "erosion", "ridges", "depth", "seed"));

        for (Spike spike : ofKind) {
            report.append(String.format(Locale.ROOT, "    %-16s %-18s %5d %10.4f %9.4f %9.4f %9.4f %22d%n",
                    spike.lap(), spike.blockX() + " " + spike.blockY() + " " + spike.blockZ(),
                    spike.drop(), spike.continents(), spike.erosion(), spike.ridges(), spike.depth(),
                    spike.seed()));
        }
    }

    private static NeedleRow needleRow(WorldType type, NoiseSettings noiseSettings, int widthBlocks,
            boolean folded, String presetId, List<Spike> sites) {
        String lap = presetId + (folded ? " torus" : " control");
        List<SeedNeedles> seeds = ScanSeeds.inOrder(NEEDLE_SEEDS, s -> {
            long seed = SEED_BASE + s * NEEDLE_SEED_STEP;
            WorldFold fold = folded ? torusOfWidth(widthBlocks) : WorldFolds.NOOP;
            SeedNeedles found = quadrants(type, noiseSettings, fold, widthBlocks, seed, lap);
            ScanReports.note(SCAN, "needle", "preset=" + presetId + " folded=" + folded + " width=" + widthBlocks
                    + " seed=" + seed + " needles=" + found.needles().needles());
            return found;
        });

        return rowOf(seeds, sites);
    }

    private static NeedleRow rowOf(List<SeedNeedles> seeds, List<Spike> sites) {
        int seedsWithNeedle = 0;
        int needles = 0;
        int worstDrop = 0;
        int sunkenColumns = 0;
        IntArrayList steps = new IntArrayList();

        for (SeedNeedles seed : seeds) {
            Needles found = seed.needles();
            sites.addAll(seed.sites());
            steps.addAll(seed.steps());
            if (found.needles() > 0) {
                seedsWithNeedle++;
            }

            needles += found.needles();
            worstDrop = Math.max(worstDrop, found.worstDrop());
            sunkenColumns += found.pits();
        }

        return new NeedleRow(seedsWithNeedle, needles, worstDrop, sunkenColumns, Jumps.of(steps));
    }

    private static SeedNeedles quadrants(WorldType type, NoiseSettings noiseSettings, WorldFold fold,
            int widthBlocks, long seed, String lap) {
        RandomState state = randomState(type, fold, seed);
        List<Spike> sites = new ArrayList<>();
        IntArrayList steps = new IntArrayList();
        Needles[] total = {new Needles(0, 0, 0)};

        GenerationFoldContext.runWithFold(fold, () -> {
            for (int quadrantX = 0; quadrantX < 2; quadrantX++) {
                for (int quadrantZ = 0; quadrantZ < 2; quadrantZ++) {
                    total[0] = total[0].plus(window(new NeedleWindow(state, noiseSettings,
                            quadrantX * widthBlocks / 2, quadrantZ * widthBlocks / 2, lap, seed), sites, steps));
                }
            }
        });

        return new SeedNeedles(total[0], sites, steps);
    }

    private static Needles window(NeedleWindow window, List<Spike> sites, IntArrayList steps) {
        return classify(window, surfaceHeights(window), sites, steps);
    }

    private static int[] surfaceHeights(NeedleWindow window) {
        DensitySampler density = window.state().getSampler(window.state().router.finalDensity());
        int span = NEEDLE_WINDOW_BLOCKS + 2;
        int[] heights = new int[span * span];

        for (int dx = 0; dx < span; dx++) {
            for (int dz = 0; dz < span; dz++) {
                heights[dx * span + dz] = surfaceHeight(density, window.noiseSettings(),
                        window.originX() + dx - 1, window.originZ() + dz - 1);
            }
        }

        return heights;
    }

    private static Needles classify(NeedleWindow window, int[] heights, List<Spike> sites, IntArrayList steps) {
        NoiseSettings noiseSettings = window.noiseSettings();
        int span = NEEDLE_WINDOW_BLOCKS + 2;
        int needles = 0;
        int worstDrop = 0;
        int pits = 0;

        for (int dx = 1; dx <= NEEDLE_WINDOW_BLOCKS; dx++) {
            for (int dz = 1; dz <= NEEDLE_WINDOW_BLOCKS; dz++) {
                int here = height(heights[dx * span + dz], noiseSettings);
                steps.add(Math.abs(here - height(heights[(dx + 1) * span + dz], noiseSettings)));
                steps.add(Math.abs(here - height(heights[dx * span + dz + 1], noiseSettings)));
                int rise = Integer.MAX_VALUE;
                int fall = Integer.MAX_VALUE;

                for (int[] step : NEEDLE_NEIGHBORS) {
                    int neighbor = height(heights[(dx + step[0]) * span + dz + step[1]], noiseSettings);
                    rise = Math.min(rise, here - neighbor);
                    fall = Math.min(fall, neighbor - here);
                }

                if (rise >= NEEDLE_DROP_BLOCKS) {
                    needles++;
                    worstDrop = Math.max(worstDrop, rise);
                    sites.add(window.spike(dx - 1, here, dz - 1, NEEDLE_KIND, rise));
                }

                if (fall >= NEEDLE_DROP_BLOCKS) {
                    pits++;
                    sites.add(window.spike(dx - 1, here, dz - 1, PIT_KIND, fall));
                }
            }
        }

        return new Needles(needles, worstDrop, pits);
    }

    private static Spike spike(RandomState state, String lap, long seed, int blockX, int blockY, int blockZ,
            String kind, int drop) {
        NoiseRouter router = state.router;
        return new Spike(lap, seed, blockX, blockY, blockZ, kind, drop,
                TerrainWallScan.valueAt(state, router.continents(), blockX, blockY, blockZ),
                TerrainWallScan.valueAt(state, router.erosion(), blockX, blockY, blockZ),
                TerrainWallScan.valueAt(state, router.ridges(), blockX, blockY, blockZ),
                TerrainWallScan.valueAt(state, router.depth(), blockX, blockY, blockZ));
    }

    private static int height(int found, NoiseSettings noiseSettings) {
        return found == NO_COLUMN ? noiseSettings.minY() : found;
    }

    private static int surfaceHeight(DensitySampler density, NoiseSettings noiseSettings, int blockX, int blockZ) {
        int bottom = noiseSettings.minY();
        int top = bottom + noiseSettings.height() - 1;

        for (int y = top; y >= bottom; y -= COARSE_STEP_BLOCKS) {
            if (density.sampleValue(SamplerContext.EMPTY_UNCACHED, blockX, y, blockZ) <= 0.0F) {
                continue;
            }

            for (int fine = Math.min(top, y + COARSE_STEP_BLOCKS - 1); fine > y; fine--) {
                if (density.sampleValue(SamplerContext.EMPTY_UNCACHED, blockX, fine, blockZ) > 0.0F) {
                    return fine;
                }
            }

            return y;
        }

        return NO_COLUMN;
    }
}

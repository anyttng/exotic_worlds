package com.exoticworlds.scan;

import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.engine.noise.ClimateScanFixture;
import com.exoticworlds.engine.noise.ClimateScanFixture.WorldType;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.shape.WorldLoopPresets;
import static com.exoticworlds.engine.noise.ClimateScanFixture.SCAN_Y_BLOCKS;
import static com.exoticworlds.engine.noise.ClimateScanFixture.SEED_BASE;
import static com.exoticworlds.engine.noise.ClimateScanFixture.TYPES;
import static com.exoticworlds.engine.noise.ClimateScanFixture.biomeSource;
import static com.exoticworlds.engine.noise.ClimateScanFixture.uncompressedCylinderOfWidth;
import static com.exoticworlds.engine.noise.ClimateScanFixture.randomState;
import static com.exoticworlds.engine.noise.ClimateScanFixture.settingsOf;
import static com.exoticworlds.engine.noise.ClimateScanFixture.torusOfWidth;
import static com.exoticworlds.scan.LandScan.LAND_SEEDS;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

class ClimateScan {
    static final int GRID = 64;
    private static final String SCAN = "climate";

    static final int SEEDS = 10;
    private static final int AXIS_SEEDS = 16;

    static final long SEED_STEP = 0x9E3779B97F4A7C15L;

    private static final double MAX_TOP_SHARE = 0.45;

    private static final int UNBOUNDED_SPAN_BLOCKS = 8192;

    private static final int CONTROL_LINE_SPREAD_BLOCKS = 65536;

    private static final double MAX_ZONE_DRIFT = 0.20;

    private static final double MAX_SPREAD_DRIFT = 0.10;

    private static final Path REPORT = ScanReports.DIRECTORY.resolve("climate-scan.txt");

    private static final Path AXIS_REPORT = ScanReports.DIRECTORY.resolve("climate-cylinder-axis.txt");

    record Width(String id, int chunkWidth, int netherScale) {
        int widthBlocks(WorldType type) {
            int blocks = this.chunkWidth * CoordinateConstants.CHUNK_WIDTH;
            return type.nether()
                    ? blocks / NetherScales.normalize(this.netherScale, this.chunkWidth, this.chunkWidth,
                            FlatShape.NO_SKEW)
                    : blocks;
        }
    }

    static final List<Width> WIDTHS = widths();

    private static List<Width> widths() {
        List<Width> widths = new ArrayList<>();
        widths.add(new Width("min", WorldLoopSizes.MIN_CHUNK_WIDTH, NetherScales.DEFAULT));
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            widths.add(new Width(preset.id(), preset.chunkWidth(), preset.netherScale()));
        }

        return List.copyOf(widths);
    }

    private record Shape(String name, IntFunction<WorldFold> foldOfWidth, boolean compressed) {
    }

    private static final List<Shape> SHAPES = List.of(
            new Shape("torus", ClimateScanFixture::torusOfWidth, true),
            new Shape("torus, strong", ClimateScanFixture::strongTorusOfWidth, true),
            new Shape("torus, uncompressed", ClimateScanFixture::uncompressedTorusOfWidth, false),
            new Shape("cylinder", ClimateScanFixture::cylinderOfWidth, true),
            new Shape("cylinder, uncompressed", ClimateScanFixture::uncompressedCylinderOfWidth, false));

    private record Scan(double distinctBiomes, double topShare, double temperatureSpread, double landShare) {
    }

    private record AxisScan(double distinctBiomes, double temperatureSpread) {
    }

    @BeforeAll
    static void bootstrapVanilla() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void measuresTheBiomeSpreadOfEveryPresetThroughTheRealClimateSampler() {
        StringBuilder report = new StringBuilder();
        report.append("Climate scan - vanilla Climate.Sampler through the real router, biomes from")
                .append(" MultiNoiseBiomeSource, no chunk generation.").append(System.lineSeparator())
                .append("Grid ").append(GRID).append("x").append(GRID).append(" points over one lap at y=")
                .append(SCAN_Y_BLOCKS).append(" blocks, ").append(SEEDS).append(" seeds, mean.")
                .append(System.lineSeparator())
                .append("Folded and control runs are the same code - the control binds WorldFolds.NOOP, so it")
                .append(" measures the same window of an unbounded vanilla world.").append(System.lineSeparator())
                .append("top share = area fraction of the most common biome; 1.00 is a one-biome world.")
                .append(" spread = standard deviation of the temperature field.").append(System.lineSeparator())
                .append("land = area fraction whose final density is solid at the world type's sea level, so the")
                .append(" surface there stands above the water rather than under it.")
                .append(System.lineSeparator())
                .append("That column is a sanity reading over these ").append(SEEDS)
                .append(" seeds, not a measurement of land: a single seed runs from 0.00 to 0.85, so a mean")
                .append(" this size lands anywhere. The land measure is the land scan, over ").append(LAND_SEEDS)
                .append(" seeds, and the two are put side by side in its own cross-check row.")
                .append(System.lineSeparator())
                .append("min is the narrowest world the game will create; the nether is the overworld width")
                .append(" divided by the nether scale that width allows, and carries five biomes in all, so it is")
                .append(" reported and not gated.").append(System.lineSeparator())
                .append("A torus or a cylinder that declined compression is never compressed, so one lap of")
                .append(" either is vanilla's own window of that size; the one-biome gate applies to the")
                .append(" compressed shapes alone.")
                .append(System.lineSeparator())
                .append("Criterion: the distribution clause. The empty-control check is a blindness guard and")
                .append(" the one-biome check a feature gate on compression; every number in the table is a")
                .append(" reading.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        List<String> thin = new ArrayList<>();
        List<String> dominated = new ArrayList<>();

        for (WorldType type : TYPES) {
            MultiNoiseBiomeSource source = biomeSource(type);
            Map<Integer, Scan> controls = new HashMap<>();

            for (Shape shape : SHAPES) {
                report.append("  ").append(type.name()).append(", ").append(shape.name())
                        .append(System.lineSeparator());
                report.append(String.format(Locale.ROOT, "    %-8s %-14s %33s %33s%n", "width", "blocks", "folded", "control"));

                for (Width width : WIDTHS) {
                    int widthBlocks = width.widthBlocks(type);
                    Scan folded = meanScan(type, source, widthBlocks, shape.foldOfWidth().apply(widthBlocks));
                    Scan control = controls.computeIfAbsent(widthBlocks,
                            blocks -> meanScan(type, source, blocks, WorldFolds.NOOP));

                    report.append(String.format(Locale.ROOT, 
                            "    %-8s %-14s %6.1f biomes %5.2f %6.3f %6.2f %6.1f biomes %5.2f %6.3f %6.2f%n",
                            width.id(), widthBlocks + " blocks",
                            folded.distinctBiomes(), folded.topShare(), folded.temperatureSpread(),
                            folded.landShare(),
                            control.distinctBiomes(), control.topShare(), control.temperatureSpread(),
                            control.landShare()));

                    if (control.distinctBiomes() <= 1.0) {
                        thin.add(type.name() + " " + width.id());
                    }

                    if (type.gated() && shape.compressed() && folded.topShare() > MAX_TOP_SHARE) {
                        dominated.add(String.format(Locale.ROOT, "%s %s %s at %.2f",
                                type.name(), shape.name(), width.id(), folded.topShare()));
                    }
                }

                report.append(System.lineSeparator());
            }
        }

        ScanReports.write(REPORT, ScanReports.population(SEEDS, SEED_BASE, SEED_STEP,
                "a biome lookup per grid point is the cost here, so ten is what the set affords beside the"
                        + " land scan"), report.toString());

        assertTrue(thin.isEmpty(),
                "the control window itself carries no biome spread, so the scan measures nothing: " + thin);
        assertTrue(dominated.isEmpty(), "a folded preset is dominated by one biome: " + dominated);
    }

    @Test
    void theCylinderCarriesVanillaClimateAlongItsUnboundedAxis() {
        StringBuilder report = new StringBuilder();
        report.append("Cylinder that declined compression, climate along the unbounded axis - the axis that")
                .append(" carries no lap and so is")
                .append(" starved of nothing.").append(System.lineSeparator())
                .append(GRID).append(" lines, spread across the ring and across ")
                .append(CONTROL_LINE_SPREAD_BLOCKS).append(" blocks for the control, which has no ring; each is ")
                .append(GRID).append(" points over ").append(UNBOUNDED_SPAN_BLOCKS).append(" blocks of Z at y=")
                .append(SCAN_Y_BLOCKS).append(" blocks, ").append(AXIS_SEEDS).append(" seeds, mean per line.")
                .append(System.lineSeparator())
                .append("The ring's own variation never enters a line, so this is the unbounded axis alone,")
                .append(" against the same measure taken on an unbounded vanilla world.")
                .append(System.lineSeparator())
                .append("Zones per line is the scale measure and is gated at ")
                .append(String.format(Locale.ROOT, "%.0f%%", MAX_ZONE_DRIFT * 100))
                .append("; the spread is gated at ").append(String.format(Locale.ROOT, "%.0f%%", MAX_SPREAD_DRIFT * 100))
                .append(" - the ring's rule cannot move a line, so this is the estimator's own floor and")
                .append(" catches a compression-class regression, not the ring.")
                .append(System.lineSeparator())
                .append("Criterion: the distribution clause. Both drift checks are distribution gates - the")
                .append(" folded reading against the unbounded control.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        List<String> off = new ArrayList<>();

        for (WorldType type : TYPES) {
            MultiNoiseBiomeSource source = biomeSource(type);
            report.append("  ").append(type.name()).append(System.lineSeparator());
            report.append(String.format(Locale.ROOT, "    %-8s %-14s %22s %22s%n", "width", "blocks", "cylinder", "control"));

            for (Width width : WIDTHS) {
                int widthBlocks = width.widthBlocks(type);
                AxisScan folded = meanAlongZ(type, source, widthBlocks, uncompressedCylinderOfWidth(widthBlocks));
                AxisScan control = meanAlongZ(type, source, CONTROL_LINE_SPREAD_BLOCKS, WorldFolds.NOOP);

                report.append(String.format(Locale.ROOT, "    %-8s %-14s %6.2f biomes %8.4f %6.2f biomes %8.4f%n",
                        width.id(), widthBlocks + " blocks",
                        folded.distinctBiomes(), folded.temperatureSpread(),
                        control.distinctBiomes(), control.temperatureSpread()));

                double drift = Math.abs(folded.distinctBiomes() - control.distinctBiomes())
                        / control.distinctBiomes();
                if (drift > MAX_ZONE_DRIFT) {
                    off.add(String.format(Locale.ROOT, "%s %s zones off by %.0f%%", type.name(), width.id(), drift * 100));
                }

                double spreadDrift = Math.abs(folded.temperatureSpread() - control.temperatureSpread())
                        / control.temperatureSpread();
                if (spreadDrift > MAX_SPREAD_DRIFT) {
                    off.add(String.format(Locale.ROOT, "%s %s spread off by %.0f%%", type.name(), width.id(), spreadDrift * 100));
                }
            }

            report.append(System.lineSeparator());
        }

        ScanReports.write(AXIS_REPORT, ScanReports.population(AXIS_SEEDS, SEED_BASE, SEED_STEP,
                "cheap per seed, but the axis is walked once per width and per world type"),
                report.toString());

        assertTrue(off.isEmpty(), "the unbounded axis does not carry vanilla's zone size or spread: " + off);
    }

    private static AxisScan meanAlongZ(WorldType type, MultiNoiseBiomeSource source, int lineSpreadBlocks,
            WorldFold fold) {
        List<AxisScan> scans = ScanSeeds.inOrder(AXIS_SEEDS, s -> {
            long seed = SEED_BASE + s * SEED_STEP;
            AxisScan scan = alongZ(type, source, lineSpreadBlocks, fold, seed);
            ScanReports.note(SCAN, "axis", "type=" + type.name() + " spread=" + lineSpreadBlocks + " seed=" + seed
                    + " biomes=" + ScanReports.value(scan.distinctBiomes()));
            return scan;
        });

        double distinct = 0.0;
        double spread = 0.0;
        for (AxisScan scan : scans) {
            distinct += scan.distinctBiomes();
            spread += scan.temperatureSpread();
        }

        return new AxisScan(distinct / AXIS_SEEDS, spread / AXIS_SEEDS);
    }

    private static AxisScan alongZ(WorldType type, MultiNoiseBiomeSource source, int lineSpreadBlocks,
            WorldFold fold, long seed) {
        Climate.Sampler sampler = randomState(type, fold, seed).createClimateSampler(SamplerContext.EMPTY_UNCACHED);
        int quartY = QuartPos.fromBlock(SCAN_Y_BLOCKS);
        double xStep = lineSpreadBlocks / (double) GRID;
        double zStep = UNBOUNDED_SPAN_BLOCKS / (double) GRID;
        double[] totals = new double[2];

        GenerationFoldContext.runWithFold(fold, () -> {
            double[] temperatures = new double[GRID];

            for (int ix = 0; ix < GRID; ix++) {
                int quartX = QuartPos.fromBlock((int) Math.round(ix * xStep));
                Set<Holder<Biome>> biomes = new HashSet<>();

                for (int iz = 0; iz < GRID; iz++) {
                    int quartZ = QuartPos.fromBlock((int) Math.round(iz * zStep));
                    biomes.add(source.getNoiseBiome(sampler.sample(quartX, quartY, quartZ)));
                    temperatures[iz] = Climate.unquantizeCoord(sampler.sample(quartX, quartY, quartZ).temperature());
                }

                totals[0] += biomes.size();
                totals[1] += standardDeviation(temperatures);
            }
        });

        return new AxisScan(totals[0] / GRID, totals[1] / GRID);
    }

    private static Scan meanScan(WorldType type, MultiNoiseBiomeSource source, int widthBlocks, WorldFold fold) {
        List<Scan> scans = ScanSeeds.inOrder(SEEDS, s -> {
            long seed = SEED_BASE + s * SEED_STEP;
            Scan scan = scan(type, source, widthBlocks, fold, seed);
            ScanReports.note(SCAN, "mean", "type=" + type.name() + " width=" + widthBlocks + " seed=" + seed
                    + " land=" + ScanReports.value(scan.landShare()));
            return scan;
        });

        double distinct = 0.0;
        double topShare = 0.0;
        double temperatureSpread = 0.0;
        double landShare = 0.0;
        for (Scan scan : scans) {
            distinct += scan.distinctBiomes();
            topShare += scan.topShare();
            temperatureSpread += scan.temperatureSpread();
            landShare += scan.landShare();
        }

        return new Scan(distinct / SEEDS, topShare / SEEDS, temperatureSpread / SEEDS, landShare / SEEDS);
    }

    @SuppressWarnings("ArrayRecordComponent")
    private record GridSample(Map<Holder<Biome>, Integer> counts, double[] temperatures, int land) {
    }

    private static Scan scan(WorldType type, MultiNoiseBiomeSource source, int widthBlocks, WorldFold fold,
            long seed) {
        RandomState randomState = randomState(type, fold, seed);
        Climate.Sampler sampler = randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
        DensitySampler density = randomState.getSampler(randomState.router.finalDensity());
        int seaLevel = settingsOf(type).seaLevel();
        GridSample[] sampled = new GridSample[1];
        GenerationFoldContext.runWithFold(fold,
                () -> sampled[0] = sampleGrid(sampler, density, source, seaLevel, widthBlocks));
        return summary(sampled[0]);
    }

    private static GridSample sampleGrid(Climate.Sampler sampler, DensitySampler density,
            MultiNoiseBiomeSource source, int seaLevel, int widthBlocks) {
        int quartY = QuartPos.fromBlock(SCAN_Y_BLOCKS);
        double step = widthBlocks / (double) GRID;
        Map<Holder<Biome>, Integer> counts = new HashMap<>();
        double[] temperatures = new double[GRID * GRID];
        int land = 0;

        for (int ix = 0; ix < GRID; ix++) {
            for (int iz = 0; iz < GRID; iz++) {
                int blockX = (int) Math.round(ix * step);
                int blockZ = (int) Math.round(iz * step);
                int quartX = QuartPos.fromBlock(blockX);
                int quartZ = QuartPos.fromBlock(blockZ);
                counts.merge(source.getNoiseBiome(sampler.sample(quartX, quartY, quartZ)), 1, Integer::sum);
                temperatures[ix * GRID + iz] =
                        Climate.unquantizeCoord(sampler.sample(quartX, quartY, quartZ).temperature());
                if (isLand(density, blockX, seaLevel, blockZ)) {
                    land++;
                }
            }
        }

        return new GridSample(counts, temperatures, land);
    }

    private static Scan summary(GridSample sampled) {
        int samples = GRID * GRID;
        int top = sampled.counts().values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return new Scan(sampled.counts().size(), top / (double) samples, standardDeviation(sampled.temperatures()),
                sampled.land() / (double) samples);
    }

    static boolean isLand(DensitySampler density, int blockX, int seaLevel, int blockZ) {
        return density.sampleValue(SamplerContext.EMPTY_UNCACHED, blockX, seaLevel, blockZ) > 0.0F;
    }

    static double mean(double[] values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }

        return sum / values.length;
    }

    static double standardDeviation(double[] values) {
        double mean = mean(values);
        double sum = 0.0;
        for (double value : values) {
            sum += (value - mean) * (value - mean);
        }

        return Math.sqrt(sum / values.length);
    }
}

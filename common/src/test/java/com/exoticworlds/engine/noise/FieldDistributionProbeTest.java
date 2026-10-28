package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertTrue;
import com.exoticworlds.engine.noise.LayeredNoise.CorrectedGrids;
import com.exoticworlds.engine.noise.LayeredNoise.NoiseParams;
import com.exoticworlds.engine.noise.LayeredNoise.Stats;
import com.exoticworlds.engine.noise.LayeredNoise.WalkMode;
import static com.exoticworlds.engine.noise.BlendedNoiseFixture.mix;
import static com.exoticworlds.engine.noise.LayeredNoise.Stats.average;
import static com.exoticworlds.engine.noise.LayeredNoise.Stats.variance;
import static com.exoticworlds.engine.noise.LayeredNoise.WORLD;
import static com.exoticworlds.engine.noise.LayeredNoise.WORLD_BLOCKS;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;



import net.minecraft.core.Direction;

// Measurement harness and regression gate of the floating-terrain fix: compares the value distribution of the
// biome-parameter fields between vanilla sampling and the periodic fold, on the standard 32-chunk (512-block) world,
// at quart (4-block) resolution. Five variants per field: vanilla with the domain warp, vanilla without it, the
// wrapped sampling with no correction (the defect), a wrapped+periodic-warp prototype, and the wrapped sampling with
// the octave variance correction switched on (the fix). Writes a plain-text report under build/reports; the
// assertions pin the replica against vanilla and finiteness, and gate the damp-only fix: damped octaves (table
// k < 1) land on the vanilla window rms, every other octave is bit-exactly untouched, the ridge peaks-and-valleys
// spike-zone share stays in vanilla's neighborhood (the amplifying v1 doubled it and failed in-game), and a pooled
// field-variance band checks the composition.
class FieldDistributionProbeTest {
    // Verified against data/minecraft/worldgen/noise/*.json in the 1.21.1 client jar and NoiseData in the 26.2
    // sources (identical).
    private static final NoiseParams CONTINENTALNESS =
            new NoiseParams("continentalness", -9, new double[] {1, 1, 2, 2, 2, 1, 1, 1, 1});
    private static final NoiseParams EROSION = new NoiseParams("erosion", -9, new double[] {1, 1, 0, 1, 1});
    private static final NoiseParams OFFSET = new NoiseParams("offset", -3, new double[] {1, 1, 1, 0});
    private static final NoiseParams RIDGE = new NoiseParams("ridge", -7, new double[] {1, 2, 1, 0, 0, 0});

    private static final double XZ_SCALE = 0.25;
    private static final int QUART = 4;
    private static final int GRID = WORLD_BLOCKS / QUART;
    private static final int MIN_BLOCK = -256;
    private static final Path REPORT =
            Path.of(System.getProperty("toroidal.reports", "build/reports")).resolve("field-distribution-probe.txt");

    private static final long[] WORLD_SEEDS = {
            0x0153EL, 0xC0FFEEL, -1234567890123456789L, 42L, 8180061971476006269L, -8011452369450108867L,
            0xDEADBEEFL, 31337L, -777L, 0x1CEB00DAL, 987654321987654321L, -424242424242L
    };



    @SuppressWarnings("ArrayRecordComponent")
    private record Extremes(double[] vanilla, double[] uncorrected, double[] corrected, double[] vanillaWorldMeans,
            double[] correctedWorldMeans, long[] peakZone, long pooled) {
    }

    @SuppressWarnings("ArrayRecordComponent")
    private record FieldGrids(double[][] vanWarp, double[][] vanFlat, double[][] modCur, double[][] modWarp,
            double[][] modFix) {
    }

    private final List<Double> meanSpreadGateRatios = new ArrayList<>();
    private double pooledVanillaVariance;
    private double pooledUncorrectedVariance;
    private double pooledCorrectedVariance;
    private double ridgePeakShareVanilla;
    private double ridgePeakShareCorrected;

    @Test
    void measuresFieldDistributions() {
        StringBuilder report = new StringBuilder();
        report.append("Field distribution probe — 32-chunk (512-block) world, quart grid ")
                .append(GRID).append("x").append(GRID).append(", y=0\n");
        report.append("Variants: van_warp (vanilla, domain warp), van_flat (vanilla, no warp), ")
                .append("mod_cur (periodic, uncorrected), mod_warp (periodic + periodic warp prototype), ")
                .append("mod_fix (periodic + octave variance correction)\n\n");

        appendOctaveTable(report, CONTINENTALNESS);
        appendOctaveTable(report, EROSION);
        report.append('\n');

        OctaveCalibration octaves = new OctaveCalibration();
        octaves.appendDegenerateCalibration(report, LapFloor.FOUR_CELLS, false);
        octaves.appendDegenerateCalibration(report, LapFloor.TWO_CELLS, true);
        octaves.appendMeanSpreadCalibration(report, LapFloor.FOUR_CELLS);
        octaves.appendMeanSpreadCalibration(report, LapFloor.TWO_CELLS);
        octaves.appendRectangularCalibration(report);
        octaves.appendCylinderCalibration(report);
        octaves.appendVerticalCalibration(report, LapFloor.FOUR_CELLS, false);
        octaves.appendVerticalCalibration(report, LapFloor.TWO_CELLS, true);
        BlendedFoldCalibration.append(report);
        appendExtremeStatistics(report);

        for (long worldSeed : WORLD_SEEDS) {
            report.append("world seed ").append(worldSeed).append('\n');
            LayeredNoise offset = new LayeredNoise(worldSeed, 0, OFFSET);
            measureField(report, worldSeed, 1, CONTINENTALNESS, offset);
            measureField(report, worldSeed, 2, EROSION, offset);
        }

        double pooledCorrectedRatio = Math.sqrt(this.pooledCorrectedVariance / this.pooledVanillaVariance);
        report.append(String.format("pooled variance ratio vs vanilla over %d fields: uncorrected %.3f, corrected %.3f%n",
                WORLD_SEEDS.length * 2,
                Math.sqrt(this.pooledUncorrectedVariance / this.pooledVanillaVariance),
                pooledCorrectedRatio));

        try {
            Files.createDirectories(REPORT.getParent());
            Files.writeString(REPORT, report.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // The regression gate, asserted after the report is on disk so a failing run still leaves the numbers to
        // read. Everything is deterministic — fixed seeds, fixed windows — so the bands cannot flake. Three teeth:
        // damped octaves (table k < 1) must land on the vanilla window rms; every other octave must be untouched
        // (re-introducing the amplification that doubled the ridge spike zone shows up here); and the ridge
        // peaks-and-valleys spike-zone share must stay in vanilla's neighborhood (the artifact-facing metric — the
        // amplifying v1 measured 13.8% against vanilla's 7.6% and failed in-game). The pooled band is a composition
        // check for gross mis-application.
        for (double[] gateRow : octaves.octaveGateRows()) {
            double fraction = gateRow[0];
            double uncorrectedRatio = gateRow[1];
            double correctedRatio = gateRow[2];
            if (OctaveVarianceCorrection.flat(fraction) < 1.0) {
                assertTrue(Math.abs(correctedRatio - 1.0) <= 0.06,
                        "damped octave rms drifted off the vanilla window at f=" + fraction + ": " + correctedRatio);
            } else {
                assertTrue(Math.abs(correctedRatio - uncorrectedRatio) <= 1.0E-9,
                        "correction is not a no-op at f=" + fraction + ": " + correctedRatio);
            }
        }
        assertTrue(Math.abs(this.ridgePeakShareCorrected - this.ridgePeakShareVanilla) <= 3.0,
                "ridge pv>0.7 share drifted off vanilla: " + this.ridgePeakShareCorrected
                        + "% vs " + this.ridgePeakShareVanilla + "%");
        for (double[] cylinderRow : octaves.cylinderGateRows()) {
            double stripRatio = cylinderRow[2] / cylinderRow[1];
            assertTrue(Math.abs(stripRatio - 1.0) <= 0.06,
                    "cylinder strip rms drifted off vanilla at f=" + cylinderRow[0] + ": " + stripRatio);
            assertTrue(cylinderRow[3] == 0.0,
                    "a starved octave still varies around the ring at f=" + cylinderRow[0] + ": " + cylinderRow[3]);
        }
        for (double livenessRatio : octaves.livenessGateRatios()) {
            assertTrue(Math.abs(livenessRatio - 1.0) <= 0.08,
                    "liveness-corrected octave rms drifted off the vanilla window: " + livenessRatio);
        }
        for (double meanSpreadRatio : this.meanSpreadGateRatios) {
            assertTrue(meanSpreadRatio > 0.7 && meanSpreadRatio < 1.3,
                    "corrected world-mean spread drifted off vanilla: " + meanSpreadRatio);
        }
        assertTrue(pooledCorrectedRatio > 0.9 && pooledCorrectedRatio < 1.1,
                "corrected field composition drifted, pooled variance ratio " + pooledCorrectedRatio);
    }

    private void appendOctaveTable(StringBuilder report, NoiseParams params) {
        report.append("octaves of ").append(params.name()).append(" at xzScale ").append(XZ_SCALE)
                .append(" on ").append(WORLD_BLOCKS).append(" blocks:\n");
        for (int i = 0; i < params.amplitudes().length; i++) {
            if (params.amplitudes()[i] == 0.0) {
                continue;
            }
            double octaveScale = XZ_SCALE * Math.pow(2.0, params.firstOctave() + i);
            double trueCells = WORLD_BLOCKS * octaveScale;
            long period = PeriodicNoiseSampler.period(WORLD.blockDomain(Direction.Axis.X), octaveScale,
                    LapFloor.of(WORLD));
            report.append(String.format("  oct %d amp %.1f: true %.2f cells/lap -> period %d%s%n",
                    params.firstOctave() + i, params.amplitudes()[i], trueCells, period,
                    period != Math.round(trueCells * 1e9) / 1e9 && trueCells < 1.0 ? "  (degenerate)" : ""));
        }
    }

    // Artifacts live in the distribution shape, not the ensemble rms: the v1 round proved that amplifying the
    // near-cell closed waves doubles the ridge pv spike zone even while every extreme quantile stays under vanilla.
    // This measures both — per-realization max |field − world mean| quantiles across seeds, and for ridge the pooled
    // share of the world inside the peaks-and-valleys spike zone — for vanilla, the uncorrected fold and the
    // damp-only correction.
    private void appendExtremeStatistics(StringBuilder report) {
        int seeds = 128;
        int grid = 64;
        report.append("field extreme statistics (max |field - world mean|, ").append(seeds)
                .append(" seeds x ").append(grid).append("x").append(grid).append(" grid, 512-block world):\n");
        for (NoiseParams params : new NoiseParams[] {CONTINENTALNESS, EROSION, RIDGE}) {
            Extremes extremes = extremes(params, seeds, grid);
            double vanillaMeanSpread = Math.sqrt(variance(extremes.vanillaWorldMeans()));
            double correctedMeanSpread = Math.sqrt(variance(extremes.correctedWorldMeans()));
            this.meanSpreadGateRatios.add(correctedMeanSpread / vanillaMeanSpread);
            report.append(' ').append(params.name()).append(":\n");
            report.append(quantileLine("vanilla", extremes.vanilla()));
            report.append(quantileLine("uncorrected", extremes.uncorrected()));
            report.append(quantileLine("corrected", extremes.corrected()));
            report.append(String.format("  world-mean spread: vanilla %.4f, corrected %.4f (ratio %.3f)%n",
                    vanillaMeanSpread, correctedMeanSpread, correctedMeanSpread / vanillaMeanSpread));
            if (params == RIDGE) {
                long[] peakZone = extremes.peakZone();
                long pooled = extremes.pooled();
                this.ridgePeakShareVanilla = 100.0 * peakZone[0] / pooled;
                this.ridgePeakShareCorrected = 100.0 * peakZone[2] / pooled;
                report.append(String.format(
                        "  pv>0.7 share (jagged peak zone): vanilla %.2f%%, uncorrected %.2f%%, corrected %.2f%%%n",
                        this.ridgePeakShareVanilla, 100.0 * peakZone[1] / pooled, this.ridgePeakShareCorrected));
            }
        }
        report.append('\n');
    }

    private Extremes extremes(NoiseParams params, int seeds, int grid) {
        double[] vanilla = new double[seeds];
        double[] uncorrected = new double[seeds];
        double[] corrected = new double[seeds];
        double[] vanillaWorldMeans = new double[seeds];
        double[] correctedWorldMeans = new double[seeds];
        long[] peakZone = new long[3];
        long pooled = 0;
        for (int s = 0; s < seeds; s++) {
            long worldSeed = mix(0x5EED5EED5L + s * 0x9E3779B97F4A7C15L);
            CorrectedGrids grids = worldGrids(new LayeredNoise(worldSeed, 1, params), grid);
            vanilla[s] = maxAbsDeviation(grids.vanilla());
            uncorrected[s] = maxAbsDeviation(grids.wrapped());
            corrected[s] = maxAbsDeviation(grids.corrected());
            vanillaWorldMeans[s] = average(grids.vanilla());
            correctedWorldMeans[s] = average(grids.corrected());
            if (params == RIDGE) {
                peakZone[0] += countPeakZone(grids.vanilla());
                peakZone[1] += countPeakZone(grids.wrapped());
                peakZone[2] += countPeakZone(grids.corrected());
                pooled += grids.vanilla().length;
            }
        }

        return new Extremes(vanilla, uncorrected, corrected, vanillaWorldMeans, correctedWorldMeans, peakZone,
                pooled);
    }

    private static CorrectedGrids worldGrids(LayeredNoise field, int grid) {
        double[] vanillaGrid = new double[grid * grid];
        double[] uncorrectedGrid = new double[grid * grid];
        double[] correctedGrid = new double[grid * grid];
        int k = 0;
        for (int i = 0; i < grid; i++) {
            double x = MIN_BLOCK + i * (WORLD_BLOCKS / (double) grid);
            for (int j = 0; j < grid; j++) {
                double z = MIN_BLOCK + j * (WORLD_BLOCKS / (double) grid);
                vanillaGrid[k] = field.vanillaValue(x * XZ_SCALE, z * XZ_SCALE);
                uncorrectedGrid[k] = field.wrappedValue(x, z, XZ_SCALE, WalkMode.CURRENT);
                correctedGrid[k] = field.wrappedValue(x, z, XZ_SCALE, WalkMode.CORRECTED);
                k++;
            }
        }

        return new CorrectedGrids(vanillaGrid, uncorrectedGrid, correctedGrid);
    }

    // Vanilla peaks-and-valleys fold of the ridges field (TerrainProvider.peaksAndValleys): pv = +1 at |r| = 2/3 —
    // the zone where the jaggedness spline injects the 16-octave jagged noise into the terrain.
    private long countPeakZone(double[] ridgeValues) {
        long count = 0;
        for (double ridge : ridgeValues) {
            double pv = -(Math.abs(Math.abs(ridge) - 0.6666666666666666) - 0.3333333333333333) * 3.0;
            if (pv > 0.7) {
                count++;
            }
        }
        return count;
    }

    private double maxAbsDeviation(double[] values) {
        double mean = 0.0;
        for (double value : values) {
            mean += value;
        }
        mean /= values.length;
        double max = 0.0;
        for (double value : values) {
            max = Math.max(max, Math.abs(value - mean));
        }
        return max;
    }

    private String quantileLine(String label, double[] extremes) {
        double[] sorted = extremes.clone();
        Arrays.sort(sorted);
        double mean = 0.0;
        for (double extreme : sorted) {
            mean += extreme;
        }
        mean /= sorted.length;
        return String.format("  %-12s mean=%.4f p50=%.4f p90=%.4f p99=%.4f max=%.4f%n",
                label, mean, sorted[sorted.length / 2], sorted[(int) (sorted.length * 0.9)],
                sorted[(int) (sorted.length * 0.99)], sorted[sorted.length - 1]);
    }

    private void measureField(StringBuilder report, long worldSeed, int salt, NoiseParams params,
            LayeredNoise offset) {
        FieldGrids grids = fieldGrids(new LayeredNoise(worldSeed, salt, params), offset);

        report.append(' ').append(params.name()).append(" (valueFactor ")
                .append(String.format("%.4f", params.valueFactor())).append("):\n");
        Stats envelope = Stats.of(grids.vanWarp());
        Stats fixStats = Stats.of(grids.modFix());
        report.append(envelope.line("van_warp")).append('\n');
        report.append(Stats.of(grids.vanFlat()).line("van_flat")).append('\n');
        report.append(Stats.of(grids.modCur()).line("mod_cur")).append('\n');
        report.append(Stats.of(grids.modWarp()).line("mod_warp")).append('\n');
        report.append(fixStats.line("mod_fix")).append('\n');

        double stdRatio = fixStats.std() / envelope.std();
        this.pooledVanillaVariance += envelope.std() * envelope.std();
        this.pooledUncorrectedVariance += Stats.of(grids.modCur()).std() * Stats.of(grids.modCur()).std();
        this.pooledCorrectedVariance += fixStats.std() * fixStats.std();
        report.append(String.format("  outside van_warp [min,max]: mod_cur %.2f%%, mod_warp %.2f%%, mod_fix %.2f%%; "
                        + "mod_fix/van_warp std ratio %.3f%n%n",
                shareOutside(grids.modCur(), envelope), shareOutside(grids.modWarp(), envelope),
                shareOutside(grids.modFix(), envelope), stdRatio));
    }

    private FieldGrids fieldGrids(LayeredNoise field, LayeredNoise offset) {
        double[][] vanWarp = new double[GRID][GRID];
        double[][] vanFlat = new double[GRID][GRID];
        double[][] modCur = new double[GRID][GRID];
        double[][] modWarp = new double[GRID][GRID];
        double[][] modFix = new double[GRID][GRID];

        for (int i = 0; i < GRID; i++) {
            double x = MIN_BLOCK + i * QUART;
            for (int j = 0; j < GRID; j++) {
                double z = MIN_BLOCK + j * QUART;

                // Vanilla warp: ShiftA feeds (x, 0, z), ShiftB feeds (z, x, 0) — world X rides the noise's Y axis.
                double shiftXVan = offset.vanillaValue(x * XZ_SCALE, z * XZ_SCALE) * 4.0;
                double shiftZVan = shiftBVanilla(offset, x, z) * 4.0;
                vanWarp[i][j] = field.vanillaValue(x * XZ_SCALE + shiftXVan, z * XZ_SCALE + shiftZVan);
                vanFlat[i][j] = field.vanillaValue(x * XZ_SCALE, z * XZ_SCALE);
                modCur[i][j] = field.wrappedValue(x, z, XZ_SCALE);

                // Periodic warp prototype: offsets sampled periodically, then applied pre-scale (x + s/scale keeps
                // closure because the shift itself is periodic). ShiftB's Y-fed world X is folded so the lap closes.
                double shiftXPer = offset.wrappedValue(x, z, XZ_SCALE) * 4.0;
                double shiftZPer = shiftBPeriodic(offset, x, z) * 4.0;
                modWarp[i][j] = field.wrappedValue(x + shiftXPer / XZ_SCALE, z + shiftZPer / XZ_SCALE, XZ_SCALE);
                modFix[i][j] = field.correctedValue(x, z, XZ_SCALE);

                assertTrue(Double.isFinite(vanWarp[i][j]) && Double.isFinite(modCur[i][j])
                        && Double.isFinite(modWarp[i][j]));
            }
        }

        return new FieldGrids(vanWarp, vanFlat, modCur, modWarp, modFix);
    }

    // Vanilla ShiftB: offsetNoise.getValue(z·0.25, x·0.25, 0) — the second argument is the noise's vertical axis.
    // The replica ignores Y (worldgen samples these fields at y=0 anyway), so vanilla-side we fold the pair into the
    // horizontal plane the same way both sides: (z, x) as (X, Z) of the lattice. This keeps van/mod comparable even
    // though it is not bit-identical to vanilla's Y-fed variant; the distribution is what is being measured.
    private double shiftBVanilla(LayeredNoise offset, double x, double z) {
        return offset.vanillaValue(z * XZ_SCALE, x * XZ_SCALE);
    }

    private double shiftBPeriodic(LayeredNoise offset, double x, double z) {
        return offset.wrappedValue(z, x, XZ_SCALE);
    }

    private double shareOutside(double[][] field, Stats envelope) {
        int outside = 0;
        int total = 0;
        for (double[] row : field) {
            for (double value : row) {
                if (value < envelope.min() || value > envelope.max()) {
                    outside++;
                }
                total++;
            }
        }
        return 100.0 * outside / total;
    }
}

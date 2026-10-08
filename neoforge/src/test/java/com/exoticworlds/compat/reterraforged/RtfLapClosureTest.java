package com.exoticworlds.compat.reterraforged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;

// Every lattice input is moved by whole laps before use, so a value and its copy one lap away agree by construction;
// what closing the lattice buys is the seam joining like any other step, and that is what is measured.
class RtfLapClosureTest {
    private static final int NARROW_CHUNKS = 16;

    private static final int WIDE_CHUNKS = 256;

    private static final int SEED = 1337;

    private static final int FEATURE_SCALE = 1500;

    private static final int CELL_SCALE = 700;

    private static final int OCTAVES = 4;

    private static final float LACUNARITY = 2.2F;

    private static final int LINES = 64;

    private static final int STRIP = 16;

    private static final float STEP_FACTOR = 4.0F;

    private static final float STEP_SLACK = 1.0E-5F;

    private static final int CELLS_PAST_LAP = 3;

    private static final int CELL_ROW = 5;

    private static final String WORLDGEN = "raccoonman.reterraforged.world.worldgen.";

    private static final String[] TARGETS = {
            WORLDGEN + "noise.NoiseUtil",
            WORLDGEN + "noise.module.Perlin",
            WORLDGEN + "noise.module.Perlin2",
            WORLDGEN + "noise.module.PerlinRidge",
            WORLDGEN + "noise.module.Billow",
            WORLDGEN + "noise.module.Cubic",
            WORLDGEN + "noise.module.Simplex",
            WORLDGEN + "noise.module.Simplex2",
            WORLDGEN + "noise.module.SimplexRidge",
            WORLDGEN + "noise.module.White",
            WORLDGEN + "noise.module.Sin",
            WORLDGEN + "noise.module.LegacyTemperature",
            WORLDGEN + "noise.module.Worley",
            WORLDGEN + "noise.module.WorleyEdge",
            WORLDGEN + "noise.module.Erosion",
            WORLDGEN + "noise.module.Frequency",
            WORLDGEN + "noise.module.Warp",
            WORLDGEN + "cell.heightmap.Heightmap",
            WORLDGEN + "cell.continent.advanced.AbstractContinent",
            WORLDGEN + "cell.continent.advanced.AdvancedContinentGenerator",
            WORLDGEN + "cell.continent.uplift.UpliftContinentGenerator",
            WORLDGEN + "cell.continent.simple.ContinentGenerator",
            WORLDGEN + "cell.continent.simple.SingleContinentGenerator",
            WORLDGEN + "cell.continent.simple.SimpleRiverGenerator",
            WORLDGEN + "cell.terrain.region.RegionModule",
            WORLDGEN + "cell.climate.ClimateModule",
            WORLDGEN + "cell.terrain.populator.ArchipelagoPopulator",
            WORLDGEN + "cell.rivermap.Rivermap",
            WORLDGEN + "densityfunction.CellSampler",
            WORLDGEN + "densityfunction.CellSampler$CacheChunk",
            WORLDGEN + "densityfunction.NoiseFunction",
            WORLDGEN + "surface.rule.StrataRule$Source",
            WORLDGEN + "feature.placement.poisson.FastPoisson",
            "net.minecraft.server.level.ChunkMap"};

    private static final Map<String, Noise> CONTINUOUS = Map.ofEntries(
            Map.entry("perlin", Noises.perlin(SEED, FEATURE_SCALE, OCTAVES, LACUNARITY)),
            Map.entry("perlin2", Noises.perlin2(SEED, FEATURE_SCALE, OCTAVES, LACUNARITY)),
            Map.entry("perlinRidge", Noises.perlinRidge(SEED, FEATURE_SCALE, OCTAVES, LACUNARITY)),
            Map.entry("billow", Noises.billow(SEED, FEATURE_SCALE, OCTAVES, LACUNARITY)),
            Map.entry("simplex", Noises.simplex(SEED, FEATURE_SCALE, OCTAVES, LACUNARITY)),
            Map.entry("simplex2", Noises.simplex2(SEED, FEATURE_SCALE, OCTAVES, LACUNARITY)),
            Map.entry("simplexRidge", Noises.simplexRidge(SEED, FEATURE_SCALE, OCTAVES, LACUNARITY)),
            Map.entry("cubic", Noises.cubic(SEED, FEATURE_SCALE, OCTAVES, LACUNARITY)),
            Map.entry("worleyEdge", Noises.worleyEdge(SEED, CELL_SCALE)),
            Map.entry("frequency", Noises.frequency(Noises.perlin(SEED, FEATURE_SCALE, OCTAVES), 0.5F, 1.0F)),
            Map.entry("warp", Noises.warpPerlin(Noises.simplex(SEED, FEATURE_SCALE, 2), SEED, CELL_SCALE, 2,
                    CELL_SCALE)));

    @BeforeAll
    static void activate() {
        RtfLap.activate();
    }

    @Test
    void everyContinuousNoiseJoinsAcrossTheSeamOfANarrowTorus() {
        assertJoined(torus(NARROW_CHUNKS));
    }

    @Test
    void everyContinuousNoiseJoinsAcrossTheSeamOfAWideTorus() {
        assertJoined(torus(WIDE_CHUNKS));
    }

    @Test
    void everyContinuousNoiseJoinsAcrossTheSeamOfACylinder() {
        assertJoined(cylinder(NARROW_CHUNKS));
        assertJoined(cylinder(WIDE_CHUNKS));
    }

    // The control: with no lap bound ReTerraForged's own lattice runs, and its seam is a cut the measure must see.
    @Test
    void theSeamJumpsWhereNoLapIsBound() {
        WorldFold fold = torus(NARROW_CHUNKS);
        Noise noise = CONTINUOUS.get("perlin");
        int seam = fold.blockDomain(Direction.Axis.X).lowerBound;
        int span = fold.blockDomain(Direction.Axis.X).domainLength;
        int jumps = 0;
        for (int line = 0; line < LINES; line++) {
            float cross = seam + line * span / (float) LINES;
            float before = noise.compute(seam + span - 1, cross, 0);
            float after = noise.compute(seam, cross, 0);
            float beside = Math.abs(noise.compute(seam + 1, cross, 0) - after);
            if (Math.abs(after - before) > STEP_FACTOR * beside + STEP_SLACK) {
                jumps++;
            }
        }

        assertTrue(jumps > LINES / 2, "the unclosed seam jumped on " + jumps + " of " + LINES + " lines");
    }

    @Test
    void aLatticeCellOneLapOnHashesAsTheCellItCopies() {
        try (RtfLap.Frame.Scope lap = RtfLap.frame().bind(torus(NARROW_CHUNKS));
                RtfLap.Frame.Scope lattice = RtfLap.frame().octave(1.0 / CELL_SCALE)) {
            int cells = RtfLap.frame().xPeriod();
            assertTrue(cells > 0, "the lattice has a period on the lap");
            assertEquals(NoiseUtil.valCoord2D(SEED, CELLS_PAST_LAP, CELL_ROW),
                    NoiseUtil.valCoord2D(SEED, cells + CELLS_PAST_LAP, CELL_ROW));
            assertEquals(NoiseUtil.cell(SEED, CELLS_PAST_LAP, CELL_ROW),
                    NoiseUtil.cell(SEED, CELLS_PAST_LAP - cells, CELL_ROW));
        }
    }

    // A mixin applies when its target loads, and a refused injector throws then; loading every target here finds it
    // in the suite rather than at server boot.
    @Test
    void everyTargetTakesItsMixins() throws ClassNotFoundException {
        ClassLoader loader = RtfLapClosureTest.class.getClassLoader();
        for (String target : TARGETS) {
            Class.forName(target, true, loader);
        }
    }

    @Test
    void theFrameIsLeftAsItWasFound() {
        RtfLap.Frame frame = RtfLap.frame();
        try (RtfLap.Frame.Scope lap = frame.bind(torus(NARROW_CHUNKS))) {
            CONTINUOUS.get("frequency").compute(0.0F, 0.0F, 0);
        }

        assertEquals(RtfLap.OPEN, frame.lap(Direction.Axis.X));
        assertEquals(RtfLap.NO_PERIOD, frame.xPeriod());
    }

    private static void assertJoined(WorldFold fold) {
        try (RtfLap.Frame.Scope lap = RtfLap.frame().bind(fold)) {
            for (Map.Entry<String, Noise> entry : CONTINUOUS.entrySet()) {
                boolean varies = false;
                for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                    if (!fold.blockDomain(axis).loops()) {
                        continue;
                    }

                    varies |= assertJoinedAlong(fold, axis, entry.getKey(), entry.getValue());
                }

                assertTrue(varies, entry.getKey() + " never changes beside the seam");
            }
        }
    }

    private static boolean assertJoinedAlong(WorldFold fold, Direction.Axis axis, String name, Noise noise) {
        Direction.Axis crossAxis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        int min = fold.blockDomain(axis).lowerBound;
        int span = fold.blockDomain(axis).domainLength;
        int seam = min + span;
        boolean crossLoops = fold.blockDomain(crossAxis).loops();
        int crossMin = crossLoops ? fold.blockDomain(crossAxis).lowerBound : -span / 2;
        int crossSpan = crossLoops ? fold.blockDomain(crossAxis).domainLength : span;
        boolean varies = false;
        float[] values = new float[2 * STRIP + 1];
        for (int line = 0; line < LINES; line++) {
            int cross = crossMin + line * crossSpan / LINES;
            // The last canonical block stands beside the first one, so the strip is read from both ends of the bounds.
            for (int i = 0; i <= 2 * STRIP; i++) {
                float along = i < STRIP ? seam - STRIP + i : min + i - STRIP;
                values[i] = axis == Direction.Axis.X ? noise.compute(along, cross, 0) : noise.compute(cross, along, 0);
            }

            float seamStep = Math.abs(values[STRIP] - values[STRIP - 1]);
            float besideStep = 0.0F;
            for (int i = 0; i < 2 * STRIP; i++) {
                if (i != STRIP - 1) {
                    besideStep = Math.max(besideStep, Math.abs(values[i + 1] - values[i]));
                }
            }

            varies |= besideStep > 0.0F;
            assertTrue(seamStep <= STEP_FACTOR * besideStep + STEP_SLACK, name + " jumps across the " + axis
                    + " seam at " + cross + ": " + seamStep + " against " + besideStep + " beside it");
        }

        return varies;
    }

    private static WorldFold torus(int chunks) {
        return WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-chunks, chunks, -chunks, chunks)));
    }

    private static WorldFold cylinder(int chunks) {
        return WorldFolds.of(FlatShape.cylinder(new WorldLoopBounds(new AxisBounds.Looped(-chunks, chunks),
                AxisBounds.Unbounded.INSTANCE)));
    }
}

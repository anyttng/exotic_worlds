package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.BlendedNoiseFixture.mix;
import static com.exoticworlds.engine.noise.LayeredNoise.Stats.variance;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.engine.noise.BlendedNoiseFixture.Octave;
import com.exoticworlds.engine.noise.cuboid.CuboidOctaves;

import net.minecraft.world.level.levelgen.synth.NoiseStack;

class CuboidOctaveProbeTest {
    private static final double[] CELLS_PER_EDGE = {
            0.125, 0.1875, 0.25, 0.3125, 0.375, 0.4375, 0.5, 0.625, 0.75, 0.875, 1.0, 1.125, 1.25, 1.375, 1.4375
    };

    private static final int CHUNK_BLOCKS = 16;
    private static final int SHORT_EDGE_CHUNKS = 32;
    private static final int LONG_EDGE_CHUNKS = 384;
    private static final int SHORT_EDGE_BLOCKS = SHORT_EDGE_CHUNKS * CHUNK_BLOCKS;
    private static final int STEP_BLOCKS = 16;
    private static final double FIRST_POINT_BLOCKS = STEP_BLOCKS / 2.0;
    private static final double FIELD_Y = 0.0;
    private static final int CUBE_SEEDS = 2048;
    private static final int BOX_SEEDS = 256;
    private static final long OCTAVE_SEED = 0xC0B01DL;
    private static final long SEED_STRIDE = 7919L;
    private static final long WINDOW_SEED = 0x0153EL;
    private static final double WINDOW_SPAN_BLOCKS = 1.0E6;
    private static final Path REPORT =
            Path.of(System.getProperty("toroidal.reports", "build/reports")).resolve("cuboid-octave-probe.txt");

    private enum Group {
        WORLD("whole surface"),
        SQUARE_FACES("faces short x short"),
        LONG_FACES("faces short x long");

        private final String label;

        Group(String label) {
            this.label = label;
        }

        static Group of(CuboidFace face) {
            return face.widthBlocks() == face.depthBlocks() ? SQUARE_FACES : LONG_FACES;
        }
    }

    private record Geometry(String label, CuboidAtlas atlas, int seeds, List<Group> groups) {
    }

    private interface Field {
        double at(int atlasFace, CuboidFace face, double localX, double localZ);
    }

    private static final class Tally {
        private double sum;

        private double squares;

        private long points;

        private double largestStep;

        void add(double value) {
            this.sum += value;
            this.squares += value * value;
            this.points++;
        }

        void step(double delta) {
            this.largestStep = Math.max(this.largestStep, Math.abs(delta));
        }

        double mean() {
            return this.sum / this.points;
        }

        double variance() {
            double mean = mean();
            return this.squares / this.points - mean * mean;
        }
    }

    private static final class Reading {
        private final double[] means;

        private double varianceSum;

        private double stepSum;

        private int worlds;

        Reading(int seeds) {
            this.means = new double[seeds];
        }

        void take(Tally tally) {
            this.means[this.worlds++] = tally.mean();
            this.varianceSum += tally.variance();
            this.stepSum += tally.largestStep;
        }

        double rms() {
            return Math.sqrt(this.varianceSum / this.worlds);
        }

        double meanSpread() {
            return Math.sqrt(variance(this.means));
        }

        double largestStep() {
            return this.stepSum / this.worlds;
        }
    }

    @Test
    void measuresStarvedOctaves() {
        StringBuilder report = new StringBuilder();
        report.append("Cuboid octave probe: one Perlin octave at y=").append(FIELD_Y).append(", a point every ")
                .append(STEP_BLOCKS).append(" blocks of every face; vanilla reads the same points of the unfolded")
                .append(" atlas in an unbounded window.\n")
                .append("rms: root of the seed-averaged variance; mean spread: deviation of the mean across seeds;")
                .append(" largest step: seed-averaged largest difference between two points ").append(STEP_BLOCKS)
                .append(" blocks apart inside one face.\n\n");
        appendGeometry(report, new Geometry("cube", CuboidAtlas.of(SHORT_EDGE_CHUNKS, SHORT_EDGE_CHUNKS,
                SHORT_EDGE_CHUNKS, 0, 0), CUBE_SEEDS, List.of(Group.WORLD)));
        appendGeometry(report, new Geometry("box", CuboidAtlas.of(SHORT_EDGE_CHUNKS, SHORT_EDGE_CHUNKS,
                LONG_EDGE_CHUNKS, 0, 0), BOX_SEEDS, List.of(Group.WORLD, Group.SQUARE_FACES, Group.LONG_FACES)));

        try {
            Files.createDirectories(REPORT.getParent());
            Files.writeString(REPORT, report.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void appendFaces(StringBuilder report, Geometry geometry) {
        report.append(geometry.label()).append(':');
        for (CuboidFace face : geometry.atlas().faces()) {
            report.append(' ').append(face.side()).append(' ').append(face.widthBlocks()).append('x')
                    .append(face.depthBlocks());
        }

        report.append(" blocks, ").append(geometry.seeds()).append(" seeds\n");
    }

    private static void appendGeometry(StringBuilder report, Geometry geometry) {
        appendFaces(report, geometry);
        for (double cells : CELLS_PER_EDGE) {
            double scale = cells / SHORT_EDGE_BLOCKS;
            Map<Group, Reading> vanilla = readings(geometry.seeds());
            Map<Group, Reading> cuboid = readings(geometry.seeds());
            Random windows = new Random(WINDOW_SEED);
            for (int seed = 0; seed < geometry.seeds(); seed++) {
                measureWorld(geometry.atlas(), scale, seed, windows, vanilla, cuboid);
            }

            for (Group group : geometry.groups()) {
                report.append(row(cells, group, vanilla.get(group), cuboid.get(group)));
            }
        }

        report.append('\n');
    }

    private static void measureWorld(CuboidAtlas atlas, double scale, int seed, Random windows,
            Map<Group, Reading> vanilla, Map<Group, Reading> cuboid) {
        Octave octave = Octave.of(mix(OCTAVE_SEED + seed * SEED_STRIDE));
        double windowX = windows.nextDouble() * WINDOW_SPAN_BLOCKS;
        double windowZ = windows.nextDouble() * WINDOW_SPAN_BLOCKS;
        CuboidOctaves octaves = CuboidOctaves.compile(atlas, scale, BoxDivisors.UNDIVIDED, null,
                NoiseStack.builder().add(octave.vanilla(), 1.0, 1.0F).build());
        take(vanilla, walk(atlas, (atlasFace, face, localX, localZ) -> octave.vanilla().get(
                (windowX + face.minBlockX() + localX) * scale, FIELD_Y,
                (windowZ + face.minBlockZ() + localZ) * scale)));
        take(cuboid, walk(atlas, (atlasFace, face, localX, localZ) -> octaves.sampleOnFace(atlasFace, localX,
                FIELD_Y, localZ)));
    }

    private static Map<Group, Reading> readings(int seeds) {
        Map<Group, Reading> readings = new EnumMap<>(Group.class);
        for (Group group : Group.values()) {
            readings.put(group, new Reading(seeds));
        }

        return readings;
    }

    private static void take(Map<Group, Reading> readings, Map<Group, Tally> tallies) {
        for (Map.Entry<Group, Tally> tally : tallies.entrySet()) {
            if (tally.getValue().points > 0) {
                readings.get(tally.getKey()).take(tally.getValue());
            }
        }
    }

    private static Map<Group, Tally> walk(CuboidAtlas atlas, Field field) {
        Map<Group, Tally> tallies = new EnumMap<>(Group.class);
        for (Group group : Group.values()) {
            tallies.put(group, new Tally());
        }

        List<CuboidFace> faces = atlas.faces();
        for (int atlasFace = 0; atlasFace < faces.size(); atlasFace++) {
            CuboidFace face = faces.get(atlasFace);
            walkFace(atlasFace, face, field, tallies.get(Group.WORLD), tallies.get(Group.of(face)));
        }

        return tallies;
    }

    private static void walkFace(int atlasFace, CuboidFace face, Field field, Tally world, Tally group) {
        int columns = face.widthBlocks() / STEP_BLOCKS;
        int rows = face.depthBlocks() / STEP_BLOCKS;
        double[] rowAbove = new double[columns];
        for (int row = 0; row < rows; row++) {
            double previous = 0.0;
            for (int column = 0; column < columns; column++) {
                double value = field.at(atlasFace, face, FIRST_POINT_BLOCKS + column * STEP_BLOCKS,
                        FIRST_POINT_BLOCKS + row * STEP_BLOCKS);
                assertTrue(Double.isFinite(value), "a sample is not finite on " + face.side());
                world.add(value);
                group.add(value);
                if (column > 0) {
                    step(world, group, value - previous);
                }

                if (row > 0) {
                    step(world, group, value - rowAbove[column]);
                }

                previous = value;
                rowAbove[column] = value;
            }
        }
    }

    private static void step(Tally world, Tally group, double delta) {
        world.step(delta);
        group.step(delta);
    }

    private static String row(double cells, Group group, Reading vanilla, Reading cuboid) {
        return String.format(Locale.ROOT,
                "  f=%.4f cells per %d-block edge, %s: rms vanilla %.4f cuboid %.4f (x%.2f)"
                        + " | mean spread vanilla %.4f cuboid %.4f (x%.2f)"
                        + " | largest step vanilla %.4f cuboid %.4f (x%.2f)%n",
                cells, SHORT_EDGE_BLOCKS, group.label, vanilla.rms(), cuboid.rms(),
                cuboid.rms() / vanilla.rms(), vanilla.meanSpread(), cuboid.meanSpread(),
                cuboid.meanSpread() / vanilla.meanSpread(), vanilla.largestStep(), cuboid.largestStep(),
                cuboid.largestStep() / vanilla.largestStep());
    }
}

package com.exoticworlds.scan;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.sun.management.ThreadMXBean;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class LatticeHotPathScan {
    private static final Path REPORT = Path.of(System.getProperty("toroidal.reports", "build/reports"))
            .resolve("scan").resolve("lattice-hot-paths.txt");

    private static final int WARM_UP_CALLS = 300_000;
    private static final int MEASURED_CALLS = 2_000_000;

    private static final int POINTS = 16;
    private static final int CHUNK_BLOCKS = 16;
    private static final double SPREAD_BLOCKS = 8.0;
    private static final double SEAM_INSET_BLOCKS = 3.5;
    private static final double LAP_OVERSHOOT_BLOCKS = 5.5;
    private static final double FLOOR_Y = 64.0;
    private static final double BOX_SIDE_BLOCKS = 0.6;
    private static final double BOX_HEIGHT_BLOCKS = 1.8;

    private static final String ROW_FORMAT =
            "path=%s world=%s fold=%s case=%s bytes_per_call=%.1f ns_per_call=%.1f checksum=%d";

    private static final List<World> WORLDS = List.of(
            new World("square_skew4", new WorldLoopBounds(-16, 16, -16, 16), 4),
            new World("uneven_skew5", new WorldLoopBounds(-24, 8, -10, 6), 5));

    @Test
    void measureTheHotPaths() {
        ThreadMXBean threads = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        List<String> rows = new ArrayList<>();
        for (World world : WORLDS) {
            List<Named> folds = List.of(
                    new Named("per_axis", WorldFolds.of(FlatShape.torus(world.bounds()))),
                    new Named("skewed", WorldFolds.of(FlatShape.latticeTorus(world.bounds(), world.skewChunks()))));
            for (Inputs inputs : List.of(Inputs.sameHalf(world), Inputs.acrossTheSeam(world))) {
                for (Named named : folds) {
                    rows.addAll(rows(threads, world, named, inputs));
                }
            }
        }

        write(rows);
    }

    private static List<String> rows(ThreadMXBean threads, World world, Named named, Inputs in) {
        WorldFold fold = named.fold();
        List<String> rows = new ArrayList<>();
        rows.add(row(threads, "fold_vec3", world, named, in, index -> (int) fold.fold(in.foldPoints[index]).x));
        rows.add(row(threads, "fold_chunk_key", world, named, in,
                index -> (int) fold.foldChunkKey(in.chunkKeys[index])));
        rows.add(row(threads, "nearest_copy_vec3", world, named, in,
                index -> (int) fold.nearestCopy(in.refs[index], in.targets[index]).x));
        rows.add(row(threads, "nearest_copy_block", world, named, in,
                index -> fold.nearestCopy(in.refBlocks[index], in.targetBlocks[index]).getX()));
        rows.add(row(threads, "sqr_distance", world, named, in,
                index -> (int) fold.sqrDistance(in.refs[index], in.targets[index])));
        rows.add(row(threads, "sqr_chunk_distance", world, named, in,
                index -> fold.sqrChunkDistance(in.refChunks[index], in.targetChunks[index])));
        rows.add(row(threads, "sqr_distance_to_box", world, named, in,
                index -> (int) fold.sqrDistanceToBox(in.targetBoxes[index], in.refs[index])));
        return rows;
    }

    private static String row(ThreadMXBean threads, String path, World world, Named named, Inputs inputs,
            HotPath hotPath) {
        int total = 0;
        for (int index = 0; index < WARM_UP_CALLS; index++) {
            total += hotPath.call(index % POINTS);
        }

        long bytesBefore = threads.getCurrentThreadAllocatedBytes();
        long started = System.nanoTime();
        for (int index = 0; index < MEASURED_CALLS; index++) {
            total += hotPath.call(index % POINTS);
        }

        long elapsed = System.nanoTime() - started;
        long bytes = threads.getCurrentThreadAllocatedBytes() - bytesBefore;
        return String.format(Locale.ROOT, ROW_FORMAT, path, world.name(), named.name(), inputs.name(),
                (double) bytes / MEASURED_CALLS, (double) elapsed / MEASURED_CALLS, total);
    }

    private static void write(List<String> rows) {
        try {
            Files.createDirectories(REPORT.getParent());
            Files.write(REPORT, rows);
        } catch (IOException failed) {
            throw new UncheckedIOException(failed);
        }
    }

    @FunctionalInterface
    private interface HotPath {
        int call(int index);
    }

    private record Named(String name, WorldFold fold) {
    }

    private record World(String name, WorldLoopBounds bounds, int skewChunks) {
        WorldLoopBounds.AxisBounds.Looped xAxis() {
            return (WorldLoopBounds.AxisBounds.Looped) this.bounds.x();
        }

        WorldLoopBounds.AxisBounds.Looped zAxis() {
            return (WorldLoopBounds.AxisBounds.Looped) this.bounds.z();
        }
    }

    private static final class Inputs {
        private final String name;
        private final Vec3[] refs = new Vec3[POINTS];
        private final Vec3[] targets = new Vec3[POINTS];
        private final Vec3[] foldPoints = new Vec3[POINTS];
        private final BlockPos[] refBlocks = new BlockPos[POINTS];
        private final BlockPos[] targetBlocks = new BlockPos[POINTS];
        private final ChunkPos[] refChunks = new ChunkPos[POINTS];
        private final ChunkPos[] targetChunks = new ChunkPos[POINTS];
        private final long[] chunkKeys = new long[POINTS];
        private final AABB[] targetBoxes = new AABB[POINTS];

        private Inputs(String name) {
            this.name = name;
        }

        String name() {
            return this.name;
        }

        static Inputs sameHalf(World world) {
            Inputs inputs = new Inputs("same_half");
            double centerX = (world.xAxis().minBlock() + world.xAxis().maxBlock()) / 2.0;
            double centerZ = (world.zAxis().minBlock() + world.zAxis().maxBlock()) / 2.0;
            for (int index = 0; index < POINTS; index++) {
                double offset = spread(index);
                inputs.fill(index, new Vec3(centerX + 0.5, FLOOR_Y, centerZ + 0.5),
                        new Vec3(centerX + offset, FLOOR_Y, centerZ - offset), new Vec3(centerX - offset, FLOOR_Y,
                                centerZ + offset));
            }

            return inputs;
        }

        static Inputs acrossTheSeam(World world) {
            Inputs inputs = new Inputs("across_the_seam");
            WorldLoopBounds.AxisBounds.Looped x = world.xAxis();
            WorldLoopBounds.AxisBounds.Looped z = world.zAxis();
            double centerZ = (z.minBlock() + z.maxBlock()) / 2.0;
            for (int index = 0; index < POINTS; index++) {
                double offset = spread(index);
                Vec3 lapOutside = (index & 1) == 0
                        ? new Vec3(x.maxBlock() + LAP_OVERSHOOT_BLOCKS + offset, FLOOR_Y, centerZ + offset)
                        : new Vec3(x.minBlock() + x.blockWidth() / 2.0 + offset, FLOOR_Y,
                                z.maxBlock() + LAP_OVERSHOOT_BLOCKS + offset);
                inputs.fill(index, new Vec3(x.maxBlock() - SEAM_INSET_BLOCKS, FLOOR_Y, centerZ + offset),
                        new Vec3(x.minBlock() + SEAM_INSET_BLOCKS + Math.abs(offset), FLOOR_Y, centerZ - offset),
                        lapOutside);
            }

            return inputs;
        }

        private static double spread(int index) {
            return (index - POINTS / 2.0) / POINTS * SPREAD_BLOCKS;
        }

        private void fill(int index, Vec3 ref, Vec3 target, Vec3 folded) {
            this.refs[index] = ref;
            this.targets[index] = target;
            this.foldPoints[index] = folded;
            this.refBlocks[index] = BlockPos.containing(ref);
            this.targetBlocks[index] = BlockPos.containing(target);
            this.refChunks[index] = ChunkPos.containing(this.refBlocks[index]);
            this.targetChunks[index] = ChunkPos.containing(this.targetBlocks[index]);
            BlockPos foldedBlock = BlockPos.containing(folded);
            this.chunkKeys[index] = ChunkPos.pack(
                    Math.floorDiv(foldedBlock.getX(), CHUNK_BLOCKS), Math.floorDiv(foldedBlock.getZ(), CHUNK_BLOCKS));
            this.targetBoxes[index] = AABB.ofSize(target.add(0.0, BOX_HEIGHT_BLOCKS / 2.0, 0.0),
                    BOX_SIDE_BLOCKS, BOX_HEIGHT_BLOCKS, BOX_SIDE_BLOCKS);
        }
    }
}

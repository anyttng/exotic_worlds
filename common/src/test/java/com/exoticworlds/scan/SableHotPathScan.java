package com.exoticworlds.scan;

import static com.exoticworlds.compat.CompatFoldFixture.PER_AXIS;
import static com.exoticworlds.compat.CompatFoldFixture.SKEWED;
import static com.exoticworlds.compat.CompatFoldFixture.WORLD_BLOCKS;
import static com.exoticworlds.compat.CompatFoldFixture.WORLD_CHUNKS;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.sable.SableChunkKeys;
import com.exoticworlds.compat.sable.SableSeamQuery;
import com.exoticworlds.compat.sable.SeamFrame;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.sun.management.ThreadMXBean;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class SableHotPathScan {
    private static final Path REPORT = Path.of(System.getProperty("toroidal.reports", "build/reports"))
            .resolve("scan").resolve("sable-hot-paths.txt");

    private static final int WARM_UP_CALLS = 300_000;
    private static final int MEASURED_CALLS = 2_000_000;

    private static final int SUB_LEVELS = 64;
    private static final int QUERIES = 16;
    private static final double SUB_LEVEL_SIDE_BLOCKS = 12.0;
    private static final double QUERY_SIDE_BLOCKS = 6.0;
    private static final double FLOOR_Y = 64.0;
    private static final double SUB_LEVEL_HEIGHT_BLOCKS = 10.0;

    private static final int CHUNK_KEYS = 32;

    private static final Vec3 ENTITY_ON_FAR_HALF = new Vec3(-253.5, 71.0, 3.5);
    private static final Vector3dc POSE_ACROSS_THE_SEAM = new Vector3d(254.5, 70.0, 3.5);
    private static final Vector3dc POSE_ON_THE_SAME_HALF = new Vector3d(-250.5, 70.0, 3.5);

    private static final Operation<Void> TICK = args -> null;
    private static final Operation<BlockPos> ON_POS = args -> BlockPos.ZERO;
    private static final Operation<Void> BOX_QUERY = args -> null;
    private static final Operation<Boolean> FREEZE = args -> Boolean.FALSE;

    private static final String ROW_FORMAT = "path=%s fold=%s bytes_per_call=%.1f ns_per_call=%.1f checksum=%d";

    @Test
    void measureTheHotPaths() {
        ThreadMXBean threads = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        List<String> rows = new ArrayList<>();
        rows.add(row(threads, "wrapper_floor", "none", index -> TICK.call(Subject.AT_FAR_HALF) == null ? 0 : 1));

        BoundingBox3d[] subLevels = subLevels();
        BoundingBox3d[] queries = queries();
        for (Named named : List.of(new Named("per_axis", PER_AXIS), new Named("skewed", SKEWED))) {
            rows.add(row(threads, "sub_level_query", named.name(),
                    index -> intersecting(named.fold(), subLevels, queries[index % QUERIES])));
        }

        for (Named named : List.of(new Named("noop", null), new Named("per_axis", PER_AXIS),
                new Named("skewed", SKEWED))) {
            warmSeamFrameSites(named.fold());
            rows.add(row(threads, "seam_frame", named.name(), index -> seamFrameSite(named.fold(), index)));
        }

        rows.add(row(threads, "pose_unbound", "none", index -> poseSite(POSE_ACROSS_THE_SEAM)));
        for (Named named : List.of(new Named("per_axis", PER_AXIS), new Named("skewed", SKEWED))) {
            SeamFrame.with(named.fold(), (Level) null, () -> ENTITY_ON_FAR_HALF, () -> {
                rows.add(row(threads, "pose_across_the_seam", named.name(), index -> poseSite(POSE_ACROSS_THE_SEAM)));
                rows.add(row(threads, "pose_on_the_same_half", named.name(), index -> poseSite(POSE_ON_THE_SAME_HALF)));
                return null;
            });
        }

        int[] chunkXs = chunkCoords(0);
        int[] chunkZs = chunkCoords(1);
        for (Named named : List.of(new Named("noop", WorldFolds.NOOP), new Named("per_axis", PER_AXIS),
                new Named("skewed", SKEWED))) {
            rows.add(row(threads, "inhabit_chunk", named.name(),
                    index -> inhabitSite(named.fold(), chunkXs[index % CHUNK_KEYS], chunkZs[index % CHUNK_KEYS])));
        }

        write(rows);
    }

    private static int intersecting(WorldFold fold, BoundingBox3d[] subLevels, BoundingBox3d query) {
        int hits = 0;
        Vec3 queryCentre = SableSeamQuery.centreOf(query);
        for (BoundingBox3d subLevel : subLevels) {
            if (SableSeamQuery.intersects(fold, queryCentre, subLevel, query)) {
                hits++;
            }
        }

        return hits;
    }

    private static void warmSeamFrameSites(@Nullable WorldFold fold) {
        for (int index = 0; index < WARM_UP_CALLS; index++) {
            seamFrameSite(fold, index);
        }
    }

    private static int seamFrameSite(@Nullable WorldFold fold, int index) {
        Subject subject = Subject.AT_FAR_HALF;
        return switch (index & 3) {
            case 0 -> {
                tickSite(fold, subject);
                yield 0;
            }
            case 1 -> onPosSite(fold, subject).getX();
            case 2 -> {
                boxQuerySite(fold, subject.box());
                yield 2;
            }
            default -> freezeSite(fold, subject.blockPosition()) ? 3 : 4;
        };
    }

    private static void tickSite(@Nullable WorldFold fold, Subject entity) {
        if (fold == null) {
            SeamFrame.with((LevelReader) null, entity, Subject::position, TICK, entity);
        } else {
            SeamFrame.with(fold, null, entity, Subject::position, TICK, entity);
        }
    }

    private static BlockPos onPosSite(@Nullable WorldFold fold, Subject entity) {
        float distance = 0.2F;
        return fold == null
                ? SeamFrame.with((LevelReader) null, entity, Subject::position, ON_POS, distance)
                : SeamFrame.with(fold, null, entity, Subject::position, ON_POS, distance);
    }

    private static void boxQuerySite(@Nullable WorldFold fold, AABB box) {
        Object consumer = box;
        if (fold == null) {
            SeamFrame.with((LevelReader) null, box, AABB::getCenter, BOX_QUERY, box, consumer);
        } else {
            SeamFrame.with(fold, null, box, AABB::getCenter, BOX_QUERY, box, consumer);
        }
    }

    private static boolean freezeSite(@Nullable WorldFold fold, BlockPos pos) {
        boolean mustBeAtEdge = true;
        return fold == null
                ? SeamFrame.with((LevelReader) null, pos, Vec3::atCenterOf, FREEZE, null, pos, mustBeAtEdge)
                : SeamFrame.with(fold, null, pos, Vec3::atCenterOf, FREEZE, null, pos, mustBeAtEdge);
    }

    private static int poseSite(Vector3dc position) {
        return (int) SeamFrame.seatPose(position).x();
    }

    private static int inhabitSite(WorldFold fold, int x, int z) {
        long physical = SableChunkKeys.physical(fold, ChunkPos.asLong(x, z));
        return ChunkPos.getX(physical) ^ ChunkPos.getZ(physical) ^ (int) physical;
    }

    private static String row(ThreadMXBean threads, String path, String fold, HotPath hotPath) {
        int total = 0;
        for (int index = 0; index < WARM_UP_CALLS; index++) {
            total += hotPath.call(index);
        }

        long bytesBefore = threads.getCurrentThreadAllocatedBytes();
        long started = System.nanoTime();
        for (int index = 0; index < MEASURED_CALLS; index++) {
            total += hotPath.call(index);
        }

        long elapsed = System.nanoTime() - started;
        long bytes = threads.getCurrentThreadAllocatedBytes() - bytesBefore;
        return String.format(Locale.ROOT, ROW_FORMAT, path, fold,
                (double) bytes / MEASURED_CALLS, (double) elapsed / MEASURED_CALLS, total);
    }

    private static BoundingBox3d[] subLevels() {
        BoundingBox3d[] boxes = new BoundingBox3d[SUB_LEVELS];
        double step = (double) WORLD_BLOCKS / SUB_LEVELS;
        for (int index = 0; index < SUB_LEVELS; index++) {
            double minX = -WORLD_BLOCKS / 2.0 + index * step;
            double minZ = -WORLD_BLOCKS / 2.0 + ((index * 7) % SUB_LEVELS) * step;
            boxes[index] = new BoundingBox3d(minX, FLOOR_Y, minZ,
                    minX + SUB_LEVEL_SIDE_BLOCKS, FLOOR_Y + SUB_LEVEL_HEIGHT_BLOCKS, minZ + SUB_LEVEL_SIDE_BLOCKS);
        }

        return boxes;
    }

    private static BoundingBox3d[] queries() {
        BoundingBox3d[] boxes = new BoundingBox3d[QUERIES];
        double step = (double) WORLD_BLOCKS / QUERIES;
        for (int index = 0; index < QUERIES; index++) {
            double minX = -WORLD_BLOCKS / 2.0 + index * step;
            double minZ = WORLD_BLOCKS / 2.0 - QUERY_SIDE_BLOCKS / 2.0;
            boxes[index] = new BoundingBox3d(minX, FLOOR_Y, minZ,
                    minX + QUERY_SIDE_BLOCKS, FLOOR_Y + QUERY_SIDE_BLOCKS, minZ + QUERY_SIDE_BLOCKS);
        }

        return boxes;
    }

    private static int[] chunkCoords(int salt) {
        int[] coords = new int[CHUNK_KEYS];
        for (int index = 0; index < CHUNK_KEYS; index++) {
            coords[index] = ((index * 5 + salt * 3) % (WORLD_CHUNKS * 4)) - WORLD_CHUNKS * 2;
        }

        return coords;
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

    private record Named(String name, @Nullable WorldFold fold) {
    }

    private record Subject(Vec3 position, AABB box, BlockPos blockPosition) {
        private static final Subject AT_FAR_HALF = new Subject(ENTITY_ON_FAR_HALF,
                AABB.ofSize(ENTITY_ON_FAR_HALF, 0.6, 1.8, 0.6), BlockPos.containing(ENTITY_ON_FAR_HALF));
    }
}

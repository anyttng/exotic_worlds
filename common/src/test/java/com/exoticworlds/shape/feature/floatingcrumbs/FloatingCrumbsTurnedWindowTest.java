package com.exoticworlds.shape.feature.floatingcrumbs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.fold.FoldOrientation;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.world.level.ChunkPos;

class FloatingCrumbsTurnedWindowTest {
    private static final int CHUNK_WIDTH_BLOCKS = CoordinateConstants.CHUNK_WIDTH_BLOCKS;
    private static final int WINDOW_CHUNKS = 3;
    private static final int HEIGHT_BLOCKS = 16;
    private static final int MIN_Y = 0;
    private static final int LAYER = 5;
    private static final CellGrid GRID = new CellGrid(CHUNK_WIDTH_BLOCKS, HEIGHT_BLOCKS);

    private static final ChunkPos CENTER = new ChunkPos(3, 0);
    private static final int CENTER_INDEX = 4;
    private static final int EAST_INDEX = 5;
    private static final FoldOrientation TURN = FoldOrientation.QUARTER_TURN_CLOCKWISE;
    private static final WorldFold TURNED_PAST_THE_EAST_EDGE = turnedEastOf(CENTER);

    private static final Box CRUMB = new Box(13, 15, 7, 7);
    private static final Box CONTINUATION = new Box(0, 7, 4, 8);
    private static final Box MASS_APART = new Box(4, 8, 8, 15);

    private record Box(int minX, int maxX, int minZ, int maxZ) {
        int blocks() {
            return (this.maxX - this.minX + 1) * (this.maxZ - this.minZ + 1);
        }
    }

    private static WorldFold turnedEastOf(ChunkPos center) {
        return (WorldFold) Proxy.newProxyInstance(
                WorldFold.class.getClassLoader(),
                new Class<?>[] {WorldFold.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "foldOriented" -> orientedEastOf(center, (ChunkPos) arguments[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static WorldFold.Folded<ChunkPos> orientedEastOf(ChunkPos center, ChunkPos asked) {
        return new WorldFold.Folded<>(asked, asked.x() > center.x() ? TURN : FoldOrientation.IDENTITY);
    }

    private static TerrainMask mask(ChunkPos pos, @Nullable WorldFold turning, FoldOrientation frame,
            @Nullable Box solid) {
        boolean[] cells = new boolean[GRID.getCellCount()];
        if (solid != null) {
            for (int localZ = solid.minZ(); localZ <= solid.maxZ(); localZ++) {
                for (int localX = solid.minX(); localX <= solid.maxX(); localX++) {
                    cells[GRID.cell(frame.applyToLocalX(localX, localZ, CHUNK_WIDTH_BLOCKS), LAYER,
                            frame.applyToLocalZ(localX, localZ, CHUNK_WIDTH_BLOCKS))] = true;
                }
            }
        }

        return TerrainMask.of(cells, pos, MIN_Y, GRID, turning);
    }

    private static TerrainMask[] window(@Nullable WorldFold turning, FoldOrientation eastFrame, Box east) {
        TerrainMask[] window = new TerrainMask[WINDOW_CHUNKS * WINDOW_CHUNKS];
        for (int stepZ = -1; stepZ <= 1; stepZ++) {
            for (int stepX = -1; stepX <= 1; stepX++) {
                int index = (stepZ + 1) * WINDOW_CHUNKS + stepX + 1;
                ChunkPos pos = new ChunkPos(CENTER.x() + stepX, CENTER.z() + stepZ);
                window[index] = switch (index) {
                    case CENTER_INDEX -> mask(pos, turning, FoldOrientation.IDENTITY, CRUMB);
                    case EAST_INDEX -> mask(pos, turning, eastFrame, east);
                    default -> mask(pos, turning, FoldOrientation.IDENTITY, null);
                };
            }
        }

        return window;
    }

    @Test
    void aCrumbContinuingIntoATurnedNeighborIsKept() {
        long[] cleared = FloatingCrumbs.crumbPositions(window(TURNED_PAST_THE_EAST_EDGE, TURN, CONTINUATION));

        assertEquals(0, cleared.length, "a component lying across the turned edge was cut at it and cleared");
    }

    @Test
    void aCrumbBesideATurnedEdgeIsClearedAsBesideAPlainOne() {
        long[] plain = FloatingCrumbs.crumbPositions(window(null, FoldOrientation.IDENTITY, MASS_APART));
        long[] turned = FloatingCrumbs.crumbPositions(window(TURNED_PAST_THE_EAST_EDGE, TURN, MASS_APART));

        assertEquals(CRUMB.blocks(), plain.length, "the crumb beside a plain edge was not cleared whole");
        assertArrayEquals(plain, turned, "the turned neighbor's mass was read as touching the crumb");
    }
}

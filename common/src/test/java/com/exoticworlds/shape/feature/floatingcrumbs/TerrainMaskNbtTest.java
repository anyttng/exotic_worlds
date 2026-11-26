package com.exoticworlds.shape.feature.floatingcrumbs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

class TerrainMaskNbtTest {
    private static final int SIDE_BLOCKS = 16;

    private static final int HEIGHT_BLOCKS = 48;

    private static final int MIN_Y = -16;

    private static final ChunkPos POS = new ChunkPos(-3, 7);

    private static final CellGrid GRID = new CellGrid(SIDE_BLOCKS, HEIGHT_BLOCKS);

    private static boolean solid(int x, int y, int z) {
        return (x * 7 + y * 3 + z * 5) % 4 != 0 && y < HEIGHT_BLOCKS - 5;
    }

    private static boolean written(int x, int y, int z) {
        return x == z && y % 3 == 0;
    }

    private static TerrainMask sample() {
        boolean[] cells = new boolean[GRID.getCellCount()];
        for (int y = 0; y < HEIGHT_BLOCKS; y++) {
            for (int z = 0; z < SIDE_BLOCKS; z++) {
                for (int x = 0; x < SIDE_BLOCKS; x++) {
                    cells[GRID.cell(x, y, z)] = solid(x, y, z);
                }
            }
        }

        TerrainMask mask = TerrainMask.of(cells, POS, MIN_Y, GRID, null);
        for (int y = 0; y < HEIGHT_BLOCKS; y++) {
            for (int z = 0; z < SIDE_BLOCKS; z++) {
                for (int x = 0; x < SIDE_BLOCKS; x++) {
                    if (written(x, y, z)) {
                        mask.beforeBlockWrite(new BlockPos(POS.getMinBlockX() + x, MIN_Y + y, POS.getMinBlockZ() + z));
                    }
                }
            }
        }

        return mask;
    }

    @Test
    void roundTripKeepsEverySolidAndWrittenCell() {
        TerrainMask loaded = TerrainMask.load(sample().save(), POS, MIN_Y, HEIGHT_BLOCKS, null);

        assertNotNull(loaded);
        assertEquals(POS, loaded.getPos());
        assertEquals(MIN_Y, loaded.getLowestY());
        assertEquals(MIN_Y + HEIGHT_BLOCKS - 6, loaded.getHighestY());
        for (int y = 0; y < HEIGHT_BLOCKS; y++) {
            for (int z = 0; z < SIDE_BLOCKS; z++) {
                for (int x = 0; x < SIDE_BLOCKS; x++) {
                    boolean solid = solid(x, y, z);
                    assertEquals(solid, loaded.isSolidAt(x, MIN_Y + y, z), "solid at " + x + ", " + y + ", " + z);
                    assertEquals(solid && !written(x, y, z), loaded.isUntouchedAt(x, MIN_Y + y, z),
                            "untouched at " + x + ", " + y + ", " + z);
                }
            }
        }
    }

    @Test
    void anotherWorldHeightDiscardsTheMask() {
        assertNull(TerrainMask.load(sample().save(), POS, MIN_Y, HEIGHT_BLOCKS + SIDE_BLOCKS, null));
    }

    @Test
    void anotherWorldBottomDiscardsTheMask() {
        assertNull(TerrainMask.load(sample().save(), POS, MIN_Y - SIDE_BLOCKS, HEIGHT_BLOCKS, null));
    }
}

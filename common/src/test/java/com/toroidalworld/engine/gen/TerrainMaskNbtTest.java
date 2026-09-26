package com.toroidalworld.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

class TerrainMaskNbtTest {
    private static final int SIDE = 16;

    private static final int HEIGHT = 48;

    private static final int MIN_Y = -16;

    private static final ChunkPos POS = new ChunkPos(-3, 7);

    private static final CellGrid GRID = new CellGrid(SIDE, HEIGHT);

    private static boolean solid(int x, int y, int z) {
        return (x * 7 + y * 3 + z * 5) % 4 != 0 && y < HEIGHT - 5;
    }

    private static boolean written(int x, int y, int z) {
        return x == z && y % 3 == 0;
    }

    private static TerrainMask sample() {
        boolean[] cells = new boolean[GRID.cells()];
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < SIDE; z++) {
                for (int x = 0; x < SIDE; x++) {
                    cells[GRID.cell(x, y, z)] = solid(x, y, z);
                }
            }
        }

        TerrainMask mask = TerrainMask.of(cells, POS, MIN_Y, GRID);
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < SIDE; z++) {
                for (int x = 0; x < SIDE; x++) {
                    if (written(x, y, z)) {
                        mask.wrote(new BlockPos(POS.getMinBlockX() + x, MIN_Y + y, POS.getMinBlockZ() + z));
                    }
                }
            }
        }

        return mask;
    }

    @Test
    void roundTripKeepsEverySolidAndWrittenCell() {
        TerrainMask loaded = TerrainMask.load(sample().save(), POS, MIN_Y, HEIGHT);

        assertNotNull(loaded);
        assertEquals(POS, loaded.pos());
        assertEquals(MIN_Y, loaded.lowestY());
        assertEquals(MIN_Y + HEIGHT - 6, loaded.highestY());
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < SIDE; z++) {
                for (int x = 0; x < SIDE; x++) {
                    boolean solid = solid(x, y, z);
                    assertEquals(solid, loaded.solidAt(x, MIN_Y + y, z), "solid at " + x + ", " + y + ", " + z);
                    assertEquals(solid && !written(x, y, z), loaded.untouchedAt(x, MIN_Y + y, z),
                            "untouched at " + x + ", " + y + ", " + z);
                }
            }
        }
    }

    @Test
    void anotherWorldHeightDiscardsTheMask() {
        assertNull(TerrainMask.load(sample().save(), POS, MIN_Y, HEIGHT + SIDE));
    }

    @Test
    void anotherWorldBottomDiscardsTheMask() {
        assertNull(TerrainMask.load(sample().save(), POS, MIN_Y - SIDE, HEIGHT));
    }
}

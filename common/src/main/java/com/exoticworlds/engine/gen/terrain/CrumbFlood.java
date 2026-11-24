package com.exoticworlds.engine.gen.terrain;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;

import com.exoticworlds.core.world.CoordinateConstants;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;

final class CrumbFlood {
    static final int SMALLEST_KEPT_BLOCKS = 32;

    static final byte NO_FLUID = 0;

    static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private static final int SECTION_BLOCKS = 16;

    record Sweep(int detached, int swept, int blocks) {
    }

    static Sweep clearCrumbs(CellGrid grid, boolean[] solid, byte[] fluid, int solidBlocks, IntConsumer cleared) {
        boolean[] taken = new boolean[solid.length];
        int[] pending = new int[solidBlocks];
        int detached = 0;
        int swept = 0;
        int blocks = 0;

        for (int start = 0; start < solid.length; start++) {
            if (!solid[start] || taken[start]) {
                continue;
            }

            int size = collect(grid, solid, taken, pending, start);
            if (isTouchingSide(grid, pending, size)) {
                continue;
            }

            detached++;
            if (size >= SMALLEST_KEPT_BLOCKS) {
                continue;
            }

            swept++;
            blocks += size;
            sweep(grid, solid, fluid, Arrays.copyOf(pending, size), cleared);
        }

        return new Sweep(detached, swept, blocks);
    }

    private static int collect(CellGrid grid, boolean[] solid, boolean[] taken, int[] pending, int start) {
        taken[start] = true;
        pending[0] = start;
        int head = 0;
        int tail = 1;
        while (head < tail) {
            int cell = pending[head++];
            tail = takeNeighbors(grid, solid, taken, pending, cell, tail);
        }

        return tail;
    }

    private static int takeNeighbors(CellGrid grid, boolean[] solid, boolean[] taken, int[] pending, int cell,
            int tail) {
        int next = tail;
        for (int axis = 0; axis < CellGrid.AXES; axis++) {
            for (int step = -1; step <= 1; step += 2) {
                int neighbor = grid.neighbor(cell, axis, step);
                if (neighbor != CellGrid.NO_CELL && solid[neighbor] && !taken[neighbor]) {
                    taken[neighbor] = true;
                    pending[next++] = neighbor;
                }
            }
        }

        return next;
    }

    private static boolean isTouchingSide(CellGrid grid, int[] cells, int count) {
        for (int i = 0; i < count; i++) {
            if (grid.isOnSide(cells[i])) {
                return true;
            }
        }

        return false;
    }

    private static void sweep(CellGrid grid, boolean[] solid, byte[] fluid, int[] crumb, IntConsumer cleared) {
        for (int cell : crumb) {
            solid[cell] = false;
        }

        Arrays.sort(crumb);
        floodCleared(grid, fluid, crumb, crumb.length);
        for (int cell : crumb) {
            cleared.accept(cell);
        }
    }

    static void floodCleared(CellGrid grid, byte[] fluid, int[] cells, int count) {
        boolean isSpread = true;
        while (isSpread) {
            isSpread = false;
            for (int i = 0; i < count; i++) {
                int cell = cells[i];
                if (fluid[cell] != NO_FLUID) {
                    continue;
                }

                byte around = fluidAround(grid, fluid, cell);
                if (around != NO_FLUID) {
                    fluid[cell] = around;
                    isSpread = true;
                }
            }
        }
    }

    private static byte fluidAround(CellGrid grid, byte[] fluid, int cell) {
        for (int axis = 0; axis < CellGrid.AXES; axis++) {
            for (int step = -1; step <= 1; step += 2) {
                if (axis == CellGrid.AXIS_Y && step < 0) {
                    continue;
                }

                int neighbor = grid.neighbor(cell, axis, step);
                if (neighbor != CellGrid.NO_CELL && fluid[neighbor] != NO_FLUID) {
                    return fluid[neighbor];
                }
            }
        }

        return NO_FLUID;
    }

    static int fill(ChunkAccess chunk, CellGrid grid, boolean[] solid, byte[] fluid,
            List<BlockState> fluids, int minY) {
        int solidBlocks = 0;

        for (int index = 0; index < chunk.getSectionsCount(); index++) {
            LevelChunkSection section = chunk.getSection(index);
            if (section.hasOnlyAir()) {
                continue;
            }

            int sectionMinY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
            for (int y = 0; y < SECTION_BLOCKS; y++) {
                for (int z = 0; z < CoordinateConstants.CHUNK_WIDTH_BLOCKS; z++) {
                    for (int x = 0; x < CoordinateConstants.CHUNK_WIDTH_BLOCKS; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        int cell = grid.cell(x, sectionMinY + y - minY, z);
                        FluidState carried = state.getFluidState();
                        if (!carried.isEmpty()) {
                            fluid[cell] = codeOf(fluids, sourceBlockOf(carried));
                        }

                        if (isSolidCell(state)) {
                            solid[cell] = true;
                            solidBlocks++;
                        }
                    }
                }
            }
        }

        return solidBlocks;
    }

    static boolean isSolidCell(BlockState state) {
        return !state.isAir() && !(state.getBlock() instanceof LiquidBlock);
    }

    static BlockState sourceBlockOf(FluidState fluid) {
        Fluid type = fluid.getType();
        Fluid source = type instanceof FlowingFluid flowing ? flowing.getSource() : type;
        return source.defaultFluidState().createLegacyBlock();
    }

    private static byte codeOf(List<BlockState> fluids, BlockState fluid) {
        int index = fluids.indexOf(fluid);
        if (index < 0) {
            if (fluids.size() >= Byte.MAX_VALUE) {
                return NO_FLUID;
            }

            fluids.add(fluid);
            index = fluids.size() - 1;
        }

        return (byte) (index + 1);
    }

    static BlockState getBlock(List<BlockState> fluids, byte code) {
        return code == NO_FLUID ? AIR : fluids.get(code - 1);
    }

    private CrumbFlood() {
    }
}

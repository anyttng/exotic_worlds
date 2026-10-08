package com.exoticworlds.compat.wover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WrapDomain;
import com.exoticworlds.engine.noise.ClimateScaleCompression;

import net.minecraft.core.Direction;

class LapMapStackTest {
    private static final LapMapStack.Layering AMPLIFIED_NETHER = new LapMapStack.Layering(64, 192, 1, 256, 12.8);

    private static final int BORDER_Y = 128;

    private static final int VERTICAL = 0;

    private static final int PALETTE = 4;

    private static final long SEED = 0x904L;

    private static final Predicate<Integer> IS_VERTICAL = biome -> biome == VERTICAL;

    private static final LapPicker<Integer> PICKER = new LapPicker<>(
            random -> random.nextInt(PALETTE), (biome, random) -> biome);

    @Test
    void theLayerPickRepeatsOnTheLapAndReachesBothLayers() {
        WorldFold fold = WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-64, 64, -48, 48)));
        LapMapStack<Integer> stack = new LapMapStack<>(fold, List.of(), IS_VERTICAL, AMPLIFIED_NETHER, SEED);
        WrapDomain x = fold.blockDomain(Direction.Axis.X);
        WrapDomain z = fold.blockDomain(Direction.Axis.Z);
        Set<Integer> picked = new HashSet<>();
        for (int bx = x.lowerBound - 64; bx < x.upperBound; bx += 7) {
            for (int bz = z.lowerBound - 64; bz < z.upperBound; bz += 11) {
                int layer = stack.layer(bx, BORDER_Y, bz);
                picked.add(layer);
                assertEquals(layer, stack.layer(bx + x.domainLength, BORDER_Y, bz), "x " + bx + " z " + bz);
                assertEquals(layer, stack.layer(bx, BORDER_Y, bz + z.domainLength), "x " + bx + " z " + bz);
            }
        }

        assertEquals(Set.of(0, 1), picked, "the noise never moved the layer border across block " + BORDER_Y);
        assertEquals(0, stack.layer(0, AMPLIFIED_NETHER.minValue() - 1, 0));
        assertEquals(1, stack.layer(0, AMPLIFIED_NETHER.maxValue() + 1, 0));
    }

    @Test
    void aVerticalCellReadsTheSameOnEveryLayerAndTheLowestLayerWins() {
        List<LapChunk<Integer>> chunks = List.of(chunk(new int[][] {{-1, 2}, {3, 1}}),
                chunk(new int[][] {{-2, 2}, {-3, 3}}), chunk(new int[][] {{2, -4}, {1, 1}}));
        LapMapStack.copyVertical(chunks, biome -> biome < 0);
        for (LapChunk<Integer> chunk : chunks) {
            assertEquals(-1, chunk.get(0, 0), "the lowest layer's vertical cell");
            assertEquals(-4, chunk.get(0, 1), "a vertical cell of the top layer alone");
            assertEquals(-3, chunk.get(1, 0), "a vertical cell of the middle layer alone");
        }

        assertEquals(List.of(1, 3, 1), chunks.stream().map(chunk -> chunk.get(1, 1)).toList(),
                "a cell no layer holds vertical was changed");
    }

    @Test
    void layersSharingAStackCarryEveryVerticalCellThroughAllOfThem() {
        WorldFold fold = WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-64, 64, -64, 64)));
        List<LapMap<Integer>> layers = List.of(
                new HexLapMap<>(fold, 32.0F, ClimateScaleCompression.NO_COMPRESSION, 1, PICKER),
                new HexLapMap<>(fold, 32.0F, ClimateScaleCompression.NO_COMPRESSION, 2, PICKER));
        new LapMapStack<>(fold, layers, IS_VERTICAL, AMPLIFIED_NETHER, SEED);
        LapAxis x = layers.getFirst().axis(Direction.Axis.X);
        LapAxis z = layers.getFirst().axis(Direction.Axis.Z);
        int vertical = 0;
        for (int kx = 0; kx < x.chunks(); kx++) {
            for (int kz = 0; kz < z.chunks(); kz++) {
                LapChunk<Integer> lower = layers.get(0).chunk(kx, kz);
                LapChunk<Integer> upper = layers.get(1).chunk(kx, kz);
                for (int lx = 0; lx < lower.sideX(); lx++) {
                    for (int lz = 0; lz < lower.sideZ(); lz++) {
                        if (lower.get(lx, lz) == VERTICAL || upper.get(lx, lz) == VERTICAL) {
                            vertical++;
                            assertEquals(VERTICAL, lower.get(lx, lz), "lower layer, chunk " + kx + "," + kz);
                            assertEquals(VERTICAL, upper.get(lx, lz), "upper layer, chunk " + kx + "," + kz);
                        }
                    }
                }
            }
        }

        assertTrue(vertical > 0, "no layer laid a vertical cell");
    }

    private static LapChunk<Integer> chunk(int[][] cells) {
        return new LapChunk<>() {
            @Override
            public int sideX() {
                return cells.length;
            }

            @Override
            public int sideZ() {
                return cells[0].length;
            }

            @Override
            public Integer get(int x, int z) {
                return cells[x][z];
            }

            @Override
            public void set(int x, int z, Integer biome) {
                cells[x][z] = biome;
            }
        };
    }
}

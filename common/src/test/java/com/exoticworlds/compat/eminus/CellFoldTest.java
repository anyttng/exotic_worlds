package com.exoticworlds.compat.eminus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.eminus.cell.CellFrame;
import com.eminus.cell.CellKey;
import com.eminus.cell.DetailLevel;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AlignedPeriod;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ToroidalShapeView;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;

class CellFoldTest {
    private static final int SECTION = SectionPos.SECTION_SIZE;
    private static final int LAPS_AROUND = 2;
    private static final double EYE_OFFSET = 0.37;
    private static final double PLACE_OFFSET_X = 0.31;
    private static final double PLACE_OFFSET_Z = 0.53;
    private static final int TINY_CHUNKS = 32;
    private static final int NETHER_CHUNKS = 16;
    private static final int ODD_CHUNKS = 37;
    private static final int SKEWED_CHUNKS = 64;
    private static final int SKEW_CHUNKS = 5;

    private static WorldFold torus(int widthChunks) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidths(widthChunks, widthChunks)));
    }

    private static WorldFold skewed(int widthChunks, int skewChunks) {
        return WorldFolds.of(FlatShape.latticeTorus(WorldLoopBounds.ofWidths(widthChunks, widthChunks), skewChunks));
    }

    private static WorldFold cylinder(int widthChunks) {
        AxisBounds.Looped looped = new AxisBounds.Looped(-widthChunks / 2, widthChunks - widthChunks / 2);
        return WorldFolds.of(FlatShape.torus(new WorldLoopBounds(looped, AxisBounds.Unbounded.INSTANCE)));
    }

    static Stream<Arguments> shapes() {
        return Stream.of(
                Arguments.of("tiny torus", torus(TINY_CHUNKS)),
                Arguments.of("nether torus", torus(NETHER_CHUNKS)),
                Arguments.of("odd torus", torus(ODD_CHUNKS)),
                Arguments.of("skewed torus", skewed(SKEWED_CHUNKS, SKEW_CHUNKS)),
                Arguments.of("odd cylinder", cylinder(ODD_CHUNKS)));
    }

    private record Voxel(long storedKey, int x, int z) {
    }

    private static Voxel voxelOf(CellFold fold, int level, int blockX, int blockZ) {
        long key = CellKey.pack(level, CellFrame.cellX(blockX, level), 0, CellFrame.cellZ(blockZ, level));
        return new Voxel(fold.foldKey(key), CellFrame.voxelX(blockX, level), CellFrame.voxelZ(blockZ, level));
    }

    private static List<int[]> latticeMoves(ToroidalShape shape, int skewBlocks) {
        int widthX = shape.loops(Direction.Axis.X) ? shape.widthBlocks(Direction.Axis.X) : 0;
        int widthZ = shape.loops(Direction.Axis.Z) ? shape.widthBlocks(Direction.Axis.Z) : 0;
        List<int[]> moves = new ArrayList<>();
        for (int lapZ = -LAPS_AROUND; lapZ <= LAPS_AROUND; lapZ++) {
            for (int lapX = -LAPS_AROUND; lapX <= LAPS_AROUND; lapX++) {
                if ((widthX != 0 || lapX == 0) && (widthZ != 0 || lapZ == 0)) {
                    moves.add(new int[] {lapX * widthX + lapZ * skewBlocks, lapZ * widthZ});
                }
            }
        }

        return moves;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shapes")
    void aSectionWrittenOnOneSideReadsAtEveryLapOnEveryLevel(String name, WorldFold worldFold) {
        CellFold fold = CellFold.of(worldFold);
        ToroidalShape shape = new ToroidalShapeView(worldFold);
        int sectionX = shape.loops(Direction.Axis.X) ? shape.maxBlock(Direction.Axis.X) - SECTION : 0;
        int sectionZ = shape.loops(Direction.Axis.Z) ? shape.minBlock(Direction.Axis.Z) : 0;

        for (int level = DetailLevel.MIN; level <= DetailLevel.MAX; level++) {
            Set<Voxel> written = new HashSet<>();
            for (int copy = 0; copy < fold.copies(level); copy++) {
                for (int x = 0; x < SECTION; x++) {
                    for (int z = 0; z < SECTION; z++) {
                        written.add(voxelOf(fold, level, sectionX + fold.copyOffsetX(level, copy) + x,
                                sectionZ + fold.copyOffsetZ(level, copy) + z));
                    }
                }
            }

            for (int[] move : latticeMoves(shape, worldFold.blockLattice().skew())) {
                for (int x = 0; x < SECTION; x++) {
                    for (int z = 0; z < SECTION; z++) {
                        Voxel read = voxelOf(fold, level, sectionX + move[0] + x, sectionZ + move[1] + z);
                        assertTrue(written.contains(read), name + " level " + level + " move " + move[0] + ","
                                + move[1] + " reads an unwritten voxel");
                    }
                }
            }

            AlignedPeriod period = AlignedPeriod.of(shape, worldFold.blockLattice().skew(),
                    Integer.numberOfTrailingZeros(DetailLevel.blocksPerCell(level)));
            for (Voxel voxel : written) {
                assertInsideAlignedWindow(shape, period, voxel.storedKey(), name);
            }
        }
    }

    private static void assertInsideAlignedWindow(ToroidalShape shape, AlignedPeriod period, long storedKey,
            String name) {
        if (shape.loops(Direction.Axis.X)) {
            long x = CellFrame.originXOf(storedKey);
            int min = shape.minBlock(Direction.Axis.X);
            assertTrue(x >= min && x < min + period.xBlocks(), name + " stores X " + x + " outside its window");
        }

        if (shape.loops(Direction.Axis.Z)) {
            long z = CellFrame.originZOf(storedKey);
            int min = shape.minBlock(Direction.Axis.Z);
            assertTrue(z >= min && z < min + period.zBlocks(), name + " stores Z " + z + " outside its window");
        }
    }

    @Test
    void aLapOffTheCellGridWritesEveryLapCopyInsideTheWindow() {
        CellFold fold = CellFold.of(torus(ODD_CHUNKS));
        int top = Integer.numberOfTrailingZeros(DetailLevel.blocksPerCell(DetailLevel.MAX));
        long laps = AlignedPeriod.blocks(ODD_CHUNKS * SECTION, top) / (ODD_CHUNKS * SECTION);
        assertEquals(laps * laps, fold.copies(DetailLevel.MAX));
        assertEquals(1, CellFold.of(torus(TINY_CHUNKS)).copies(DetailLevel.MAX));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shapes")
    void theWindowHoldsOneCopyOfEveryPlaceAroundTheCamera(String name, WorldFold worldFold) {
        CellFold fold = CellFold.of(worldFold);
        ToroidalShape shape = new ToroidalShapeView(worldFold);
        double eyeX = shape.loops(Direction.Axis.X) ? shape.maxBlock(Direction.Axis.X) - EYE_OFFSET : EYE_OFFSET;
        double eyeZ = shape.loops(Direction.Axis.Z) ? shape.minBlock(Direction.Axis.Z) + EYE_OFFSET : EYE_OFFSET;
        List<int[]> moves = latticeMoves(shape, worldFold.blockLattice().skew());

        for (int x = -SECTION * SECTION; x < SECTION * SECTION; x += SECTION + 1) {
            for (int z = -SECTION * SECTION; z < SECTION * SECTION; z += SECTION + 1) {
                int inside = 0;
                for (int[] move : moves) {
                    double placeX = eyeX + x + PLACE_OFFSET_X + move[0];
                    double placeZ = eyeZ + z + PLACE_OFFSET_Z + move[1];
                    if (fold.meetsLap(placeX, placeZ, placeX, placeZ, eyeX, eyeZ, 0.0)) {
                        inside++;
                    }
                }

                assertEquals(1, inside, name + " holds " + inside + " copies of offset " + x + "," + z);
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shapes")
    void twoListedNodesShareAStoredKeyOnlyWhenBothCrossTheEdge(String name, WorldFold worldFold) {
        CellFold fold = CellFold.of(worldFold);
        ToroidalShape shape = new ToroidalShapeView(worldFold);
        int skew = worldFold.blockLattice().skew();
        double eyeX = shape.loops(Direction.Axis.X) ? shape.maxBlock(Direction.Axis.X) - EYE_OFFSET : EYE_OFFSET;
        double eyeZ = shape.loops(Direction.Axis.Z) ? shape.minBlock(Direction.Axis.Z) + EYE_OFFSET : EYE_OFFSET;

        for (int level = DetailLevel.MIN; level <= DetailLevel.MAX; level++) {
            int side = DetailLevel.blocksPerCell(level);
            int reach = farReach(shape) / side + 2;
            int centreX = CellFrame.cellX((int) Math.floor(eyeX), level);
            int centreZ = CellFrame.cellZ((int) Math.floor(eyeZ), level);
            Map<Long, List<double[]>> byStoredKey = new HashMap<>();
            for (int cellX = centreX - reach; cellX <= centreX + reach; cellX++) {
                for (int cellZ = centreZ - reach; cellZ <= centreZ + reach; cellZ++) {
                    double minX = (double) cellX * side;
                    double minZ = (double) cellZ * side;
                    if (fold.meetsLap(minX, minZ, minX + side, minZ + side, eyeX, eyeZ, 0.0)) {
                        long key = CellKey.pack(level, cellX, 0, cellZ);
                        byStoredKey.computeIfAbsent(fold.foldKey(key), stored -> new ArrayList<>())
                                .add(new double[] {minX, minZ});
                    }
                }
            }

            assertFalse(byStoredKey.isEmpty(), name + " lists no node on level " + level);
            for (List<double[]> nodes : byStoredKey.values()) {
                if (nodes.size() < 2) {
                    continue;
                }

                for (double[] node : nodes) {
                    assertFalse(whollyInside(shape, skew, node[0], node[1], side, eyeX, eyeZ),
                            name + " level " + level + " lists a node wholly inside beside its own copy");
                }
            }
        }
    }

    @Test
    void anUnshapedWorldFoldsNothing() {
        long key = CellKey.pack(DetailLevel.MAX, 12345, 3, -6789);
        assertEquals(key, CellFold.NONE.foldKey(key));
        assertEquals(1, CellFold.NONE.copies(DetailLevel.MIN));
        assertEquals(7, CellFold.NONE.foldChunkX(7, 9));
        assertTrue(CellFold.NONE.meetsLap(1.0e6, 1.0e6, 1.0e6, 1.0e6, 0.0, 0.0, 0.0));
        assertFalse(CellFold.of(null).repeats());
        assertNotEquals(CellFold.NONE, CellFold.of(torus(TINY_CHUNKS)));
        assertEquals(CellFold.of(torus(TINY_CHUNKS)), CellFold.of(torus(TINY_CHUNKS)));
    }

    private static int farReach(ToroidalShape shape) {
        int widest = 0;
        for (Direction.Axis axis : List.of(Direction.Axis.X, Direction.Axis.Z)) {
            if (shape.loops(axis)) {
                widest = Math.max(widest, shape.widthBlocks(axis));
            }
        }

        return widest * LAPS_AROUND;
    }

    private static boolean whollyInside(ToroidalShape shape, int skew, double minX, double minZ, int side, double eyeX,
            double eyeZ) {
        for (double cornerX : new double[] {minX, minX + side}) {
            for (double cornerZ : new double[] {minZ, minZ + side}) {
                double dz = cornerZ - eyeZ;
                double dx = cornerX - eyeX;
                if (shape.loops(Direction.Axis.Z)) {
                    double widthZ = shape.widthBlocks(Direction.Axis.Z);
                    if (Math.abs(dz) >= widthZ / 2) {
                        return false;
                    }

                    dx -= dz / widthZ * skew;
                }

                if (shape.loops(Direction.Axis.X) && Math.abs(dx) >= shape.widthBlocks(Direction.Axis.X) / 2.0) {
                    return false;
                }
            }
        }

        return true;
    }
}

package com.exoticworlds.engine.gen.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.core.fold.cuboid.CuboidFixtures;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

class SpreadRingScanTest {
    private static final int SKEW_CHUNKS = 16;
    private static final int SPACING_CHUNKS = 8;
    private static final int SEPARATION = 4;
    private static final int SALT = 14357617;
    private static final long SEED = 0x5EEDL;
    private static final int CENTER_X = 8;

    private static final WorldFold FOLD =
            WorldFolds.of(FlatShape.latticeTorus(ShapeFixtures.loopedBounds(-32, 32, -32, 32), SKEW_CHUNKS));
    private static final RandomSpreadStructurePlacement PLACEMENT =
            new RandomSpreadStructurePlacement(SPACING_CHUNKS, SEPARATION, RandomSpreadType.LINEAR, SALT);
    private static final int CUBOID_EDGE_CHUNKS = 16;
    private static final CuboidAtlas CUBOID = CuboidAtlas.of(CUBOID_EDGE_CHUNKS, CUBOID_EDGE_CHUNKS,
            CUBOID_EDGE_CHUNKS, CuboidFixtures.MIN_CHUNK_X, CuboidFixtures.MIN_CHUNK_Z);
    private static final WorldFold CUBOID_FOLD = CuboidFixtures.cuboidFold(CUBOID);

    private static SpreadRingScan scanAround(ChunkPos center) {
        return SpreadRingScan.of(FOLD, AddedStructureStarts.NONE, PLACEMENT, SEED, center);
    }

    private static int offsetCap(ChunkPos center) {
        TranslationLattice lattice = FOLD.getChunkLattice();
        return Math.max(SectorGridAxis.of(lattice.getX(), SPACING_CHUNKS, center.x()).getOffsetCap(),
                SectorGridAxis.of(lattice.getZ(), SPACING_CHUNKS, center.z()).getOffsetCap());
    }

    @ParameterizedTest
    @ValueSource(ints = {-32, 24})
    void everyCellProbesThePickOfTheCellItsPositionFoldsTo(int centerZ) {
        ChunkPos center = new ChunkPos(CENTER_X, centerZ);
        TranslationLattice lattice = FOLD.getChunkLattice();
        SpreadRingScan scan = scanAround(center);
        int cap = offsetCap(center);
        boolean crossedTheZSeam = false;
        for (int x = -cap; x <= cap; x++) {
            for (int z = -cap; z <= cap; z++) {
                int unwrappedX = center.x() + x * SPACING_CHUNKS;
                int unwrappedZ = center.z() + z * SPACING_CHUNKS;
                ChunkPos expected = PLACEMENT.getPotentialStructureChunk(SEED,
                        lattice.foldX(unwrappedX, unwrappedZ), lattice.foldZ(unwrappedZ));
                assertEquals(expected, scan.cell(x, z, chunk -> chunk, added -> null), "cell " + x + ", " + z);
                crossedTheZSeam |= lattice.zLaps(unwrappedZ) != 0;
            }
        }

        assertTrue(crossedTheZSeam);
    }

    @ParameterizedTest
    @ValueSource(ints = {-32, 24})
    void aRingPastTheOffsetCapProbesNothing(int centerZ) {
        ChunkPos center = new ChunkPos(CENTER_X, centerZ);
        List<ChunkPos> probed = new ArrayList<>();
        assertNull(scanAround(center).ring(offsetCap(center) + 1, chunk -> {
            probed.add(chunk);
            return null;
        }, added -> null));
        assertEquals(List.of(), probed);
    }

    @Test
    void aCellPastTheTopFacesWestEdgeReadsTheWestFace() {
        CuboidFace top = CuboidFixtures.face(CUBOID, CuboidFace.Side.TOP);
        ChunkPos center = new ChunkPos(SectionPos.blockToSectionCoord(top.minBlockX()),
                SectionPos.blockToSectionCoord(top.minBlockZ() + top.depthBlocks() / 2));
        List<SectorCells.Read> reads = new SplitSectorCells(CUBOID_FOLD, SPACING_CHUNKS, center).reads(-1, 0);

        Set<CuboidFace.Side> faces = new HashSet<>();
        for (SectorCells.Read read : reads) {
            CuboidFace face = CUBOID.faceAt(SectionPos.sectionToBlockCoord(read.minChunkX()),
                    SectionPos.sectionToBlockCoord(read.minChunkZ()));
            assertSame(face, CUBOID.faceAt(SectionPos.sectionToBlockCoord(read.maxChunkX()),
                    SectionPos.sectionToBlockCoord(read.maxChunkZ())), "read " + read);
            assertTrue(read.cellX() * SPACING_CHUNKS <= read.maxChunkX() && (read.cellX() + 1) * SPACING_CHUNKS > read.minChunkX()
                    && read.cellZ() * SPACING_CHUNKS <= read.maxChunkZ() && (read.cellZ() + 1) * SPACING_CHUNKS > read.minChunkZ(),
                    "read " + read);
            faces.add(face.side());
        }

        assertTrue(faces.contains(CuboidFace.Side.WEST), "faces read " + faces);
    }

    @ParameterizedTest
    @MethodSource("cuboidCenters")
    void theRingsUpToTheCapProbeEveryPickOnTheNetAndNothingElse(ChunkPos center) {
        SpreadRingScan scan = SpreadRingScan.of(CUBOID_FOLD, AddedStructureStarts.NONE, PLACEMENT, SEED, center);
        Set<ChunkPos> probed = new HashSet<>();
        for (int radius = 0; radius <= cuboidCap(); radius++) {
            scan.ring(radius, chunk -> {
                probed.add(chunk);
                return null;
            }, added -> null);
        }

        assertEquals(picksOnTheNet(), probed);
    }

    @ParameterizedTest
    @MethodSource("cuboidCenters")
    void aCuboidRingPastTheCapProbesNothing(ChunkPos center) {
        List<ChunkPos> probed = new ArrayList<>();
        assertNull(SpreadRingScan.of(CUBOID_FOLD, AddedStructureStarts.NONE, PLACEMENT, SEED, center)
                .ring(cuboidCap() + 1, chunk -> {
                    probed.add(chunk);
                    return null;
                }, added -> null));
        assertEquals(List.of(), probed);
    }

    static Stream<ChunkPos> cuboidCenters() {
        CuboidFace top = CuboidFixtures.face(CUBOID, CuboidFace.Side.TOP);
        CuboidFace front = CuboidFixtures.face(CUBOID, CuboidFace.Side.FRONT);
        return Stream.of(
                new ChunkPos(SectionPos.blockToSectionCoord(top.minBlockX()),
                        SectionPos.blockToSectionCoord(top.minBlockZ() + top.depthBlocks() / 2)),
                new ChunkPos(SectionPos.blockToSectionCoord(top.minBlockX()),
                        SectionPos.blockToSectionCoord(top.minBlockZ())),
                new ChunkPos(SectionPos.blockToSectionCoord(front.minBlockX() + front.widthBlocks() / 2),
                        SectionPos.blockToSectionCoord(front.minBlockZ() + front.depthBlocks() / 2)));
    }

    private static int cuboidCap() {
        return CUBOID_FOLD.stepsToCoverTheWorld(SPACING_CHUNKS * SectionPos.SECTION_SIZE);
    }

    private static Set<ChunkPos> picksOnTheNet() {
        int minChunkX = Integer.MAX_VALUE;
        int minChunkZ = Integer.MAX_VALUE;
        int maxChunkX = Integer.MIN_VALUE;
        int maxChunkZ = Integer.MIN_VALUE;
        for (CuboidFace face : CUBOID.faces()) {
            minChunkX = Math.min(minChunkX, SectionPos.blockToSectionCoord(face.minBlockX()));
            minChunkZ = Math.min(minChunkZ, SectionPos.blockToSectionCoord(face.minBlockZ()));
            maxChunkX = Math.max(maxChunkX, SectionPos.blockToSectionCoord(face.minBlockX() + face.widthBlocks()) - 1);
            maxChunkZ = Math.max(maxChunkZ, SectionPos.blockToSectionCoord(face.minBlockZ() + face.depthBlocks()) - 1);
        }

        Set<ChunkPos> picks = new HashSet<>();
        for (int cellX = Math.floorDiv(minChunkX, SPACING_CHUNKS); cellX <= Math.floorDiv(maxChunkX, SPACING_CHUNKS); cellX++) {
            for (int cellZ = Math.floorDiv(minChunkZ, SPACING_CHUNKS); cellZ <= Math.floorDiv(maxChunkZ, SPACING_CHUNKS); cellZ++) {
                ChunkPos picked = PLACEMENT.getPotentialStructureChunk(SEED, cellX * SPACING_CHUNKS, cellZ * SPACING_CHUNKS);
                if (!CUBOID_FOLD.isOver(picked)) {
                    picks.add(picked);
                }
            }
        }

        return picks;
    }
}

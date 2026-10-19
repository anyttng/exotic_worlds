package com.exoticworlds.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

class SpreadRingScanTest {
    private static final int SKEW_CHUNKS = 16;
    private static final int SPACING = 8;
    private static final int SEPARATION = 4;
    private static final int SALT = 14357617;
    private static final long SEED = 0x5EEDL;
    private static final int CENTER_X = 8;

    private static final WorldFold FOLD =
            WorldFolds.of(FlatShape.latticeTorus(ShapeFixtures.loopedBounds(-32, 32, -32, 32), SKEW_CHUNKS));
    private static final RandomSpreadStructurePlacement PLACEMENT =
            new RandomSpreadStructurePlacement(SPACING, SEPARATION, RandomSpreadType.LINEAR, SALT);

    private static SpreadRingScan scanAround(ChunkPos center) {
        return SpreadRingScan.of(FOLD, AddedStructureStarts.NONE, PLACEMENT, SEED, center);
    }

    private static int offsetCap(ChunkPos center) {
        TranslationLattice lattice = FOLD.chunkLattice();
        return Math.max(SectorGridAxis.of(lattice.x(), SPACING, center.x()).offsetCap(),
                SectorGridAxis.of(lattice.z(), SPACING, center.z()).offsetCap());
    }

    @ParameterizedTest
    @ValueSource(ints = {-32, 24})
    void everyCellProbesThePickOfTheCellItsPositionFoldsTo(int centerZ) {
        ChunkPos center = new ChunkPos(CENTER_X, centerZ);
        TranslationLattice lattice = FOLD.chunkLattice();
        SpreadRingScan scan = scanAround(center);
        int cap = offsetCap(center);
        boolean crossedTheZSeam = false;
        for (int x = -cap; x <= cap; x++) {
            for (int z = -cap; z <= cap; z++) {
                int unwrappedX = center.x() + x * SPACING;
                int unwrappedZ = center.z() + z * SPACING;
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
}

package com.exoticworlds.compat.distanthorizons;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

class DhBeamPiecesTest {
    private static final int HALF_WIDTH_CHUNKS = 32;
    private static final int MIN_BLOCK = -HALF_WIDTH_CHUNKS * 16;
    private static final int MAX_BLOCK = HALF_WIDTH_CHUNKS * 16;
    private static final int WIDTH_BLOCKS = MAX_BLOCK - MIN_BLOCK;

    private final DhLattice lattice = torus();

    private static DhLattice torus() {
        AxisBounds.Looped looped = new AxisBounds.Looped(-HALF_WIDTH_CHUNKS, HALF_WIDTH_CHUNKS);
        return DhLattice.of(WorldFolds.of(ShapeFixtures.torus(new WorldLoopBounds(looped, looped))));
    }

    @Test
    void aMaxXOnTheFirstColumnOfTheNextCopyAsksForThatColumn() {
        List<WorldCopies.Piece> pieces = DhKeys.beamPieces(this.lattice, 0, MAX_BLOCK, -10, 10);
        assertTrue(covers(pieces, WIDTH_BLOCKS, 0, MIN_BLOCK, 0), () -> "pieces " + pieces);
    }

    @Test
    void aMaxZOnTheFirstColumnOfTheNextCopyAsksForThatColumn() {
        List<WorldCopies.Piece> pieces = DhKeys.beamPieces(this.lattice, -10, 10, 0, MAX_BLOCK);
        assertTrue(covers(pieces, 0, WIDTH_BLOCKS, 0, MIN_BLOCK), () -> "pieces " + pieces);
    }

    @Test
    void aMinXOnTheLastColumnOfThePreviousCopyAsksForThatColumn() {
        List<WorldCopies.Piece> pieces = DhKeys.beamPieces(this.lattice, MIN_BLOCK - 1, 0, -10, 10);
        assertTrue(covers(pieces, -WIDTH_BLOCKS, 0, MAX_BLOCK - 1, 0), () -> "pieces " + pieces);
    }

    @Test
    void noPieceAsksPastTheWorld() {
        for (WorldCopies.Piece piece : DhKeys.beamPieces(this.lattice, -600, 600, -600, 600)) {
            assertTrue(piece.minX() >= MIN_BLOCK && piece.maxX() < MAX_BLOCK, () -> "piece " + piece);
            assertTrue(piece.minZ() >= MIN_BLOCK && piece.maxZ() < MAX_BLOCK, () -> "piece " + piece);
        }
    }

    private static boolean covers(List<WorldCopies.Piece> pieces, int dx, int dz, int x, int z) {
        return pieces.stream().anyMatch(piece -> piece.copy().equals(new WorldCopies.Copy(dx, dz))
                && piece.minX() <= x && x <= piece.maxX() && piece.minZ() <= z && z <= piece.maxZ());
    }
}

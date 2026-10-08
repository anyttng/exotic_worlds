package com.exoticworlds.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;

class WorldCopiesTest {
    private static final int MIN_CHUNK = -16;
    private static final int MAX_CHUNK = 16;
    private static final int LOWER = -256;
    private static final int WIDTH = 512;
    private static final int SKEW_CHUNKS = 16;
    private static final int SKEW = 256;

    private static final ToroidalShape TORUS = MapShapes.torus(MIN_CHUNK, MAX_CHUNK);
    private static final ToroidalShape LATTICE = MapShapes.latticeTorus(MIN_CHUNK, MAX_CHUNK, SKEW_CHUNKS);
    private static final ToroidalShape CYLINDER = MapShapes.cylinder(MIN_CHUNK, MAX_CHUNK);

    @Test
    void aViewWiderThanThePlainTorusMeetsEveryCopyOnTheGrid() {
        List<WorldCopies.Copy> copies = WorldCopies.meeting(TORUS, -1000, -1000, 1000, 1000);
        assertEquals(WorldCopies.IDENTITY, copies.get(0), "the world itself is not the first copy");
        assertEquals(expected(0, true, -1000, -1000, 1000, 1000), new HashSet<>(copies),
                "-1000..999 on a 512-block torus is not copies -2..2 on both axes");
    }

    @Test
    void theRowPastTheZSeamOfALatticeTorusIsMovedByTheSkew() {
        Set<WorldCopies.Copy> copies = new HashSet<>(WorldCopies.meeting(LATTICE, LOWER, 256, 256, 768));
        assertEquals(Set.of(new WorldCopies.Copy(-SKEW, WIDTH), new WorldCopies.Copy(SKEW, WIDTH)), copies,
                "the row above the world is not the two copies a skew of 256 puts over -256..255");
        assertFalse(copies.contains(new WorldCopies.Copy(0, WIDTH)), "a copy straight above the world exists");
    }

    @Test
    void aViewWiderThanALatticeTorusMeetsEveryLatticeCopy() {
        assertEquals(expected(SKEW, true, -1500, -1300, 1700, 1100),
                new HashSet<>(WorldCopies.meeting(LATTICE, -1500, -1300, 1700, 1100)),
                "a wide view on the lattice torus is not the lattice's own copies");
    }

    @Test
    void aCylinderMeetsCopiesAlongItsLoopedAxisAlone() {
        assertEquals(expected(0, false, -1000, -50000, 1000, 50000),
                new HashSet<>(WorldCopies.meeting(CYLINDER, -1000, -50000, 1000, 50000)),
                "a cylinder met a copy along Z");
    }

    @Test
    void anUnshapedWorldIsItsOwnOnlyCopy() {
        assertEquals(List.of(WorldCopies.IDENTITY), WorldCopies.meeting(null, -40000000, -40000000, 40000000, 40000000));
        assertEquals(List.of(), WorldCopies.meeting(TORUS, 10, 0, 10, 100), "an empty view met a copy");
    }

    @Test
    void thePiecesOfAViewCoverItOnceInsideTheWorld() {
        for (ToroidalShape shape : List.of(TORUS, LATTICE, CYLINDER)) {
            List<WorldCopies.Piece> pieces = WorldCopies.pieces(shape, -1100, -900, 1300, 700);
            long area = 0;
            for (WorldCopies.Piece piece : pieces) {
                area += (long) (piece.maxX() - piece.minX()) * (piece.maxZ() - piece.minZ());
                assertTrue(piece.minX() >= LOWER && piece.maxX() <= LOWER + WIDTH, "a piece left the world on X");
            }

            assertEquals(2400L * 1600L, area, "the pieces do not cover the view exactly once");
        }
    }

    @Test
    void aLatticePieceIsTheViewMovedBackByItsOwnCopy() {
        assertEquals(Set.of(
                        new WorldCopies.Piece(new WorldCopies.Copy(-SKEW, WIDTH), 0, LOWER, 256, 256),
                        new WorldCopies.Piece(new WorldCopies.Copy(SKEW, WIDTH), LOWER, LOWER, 0, 256)),
                new HashSet<>(WorldCopies.pieces(LATTICE, LOWER, 256, 256, 768)),
                "the row above maps onto the world shifted by the skew, not straight down");
    }

    @Test
    void anUnshapedWorldKeepsTheViewAsItsOnePiece() {
        assertEquals(List.of(new WorldCopies.Piece(WorldCopies.IDENTITY, 15040, 7, 15104, 71)),
                WorldCopies.pieces(null, 15040, 7, 15104, 71));
    }

    @Test
    void thePlainTorusSeamsAreTheWorldsEdgesOnBothAxes() {
        List<WorldCopies.Edge> seams = WorldCopies.seams(TORUS, -300, -300, 300, 300);
        assertEquals(Set.of(LOWER, LOWER + WIDTH), verticalAt(seams, -300, 300), "the X seams are not at -256 and 256");
        assertEquals(Set.of(LOWER, LOWER + WIDTH), horizontalAt(seams), "the Z seams are not at -256 and 256");
    }

    @Test
    void theLatticeXSeamsPastTheZSeamAreMovedByTheSkew() {
        List<WorldCopies.Edge> seams = WorldCopies.seams(LATTICE, -300, -300, 300, 700);
        assertEquals(Set.of(LOWER, LOWER + WIDTH), verticalAt(seams, LOWER, 256), "the world's own X seams moved");
        assertEquals(Set.of(0), verticalAt(seams, 256, 700), "the row above does not put its X seam at -256 + 256 = 0");
    }

    @Test
    void aCylinderHasNoSeamAcrossZ() {
        List<WorldCopies.Edge> seams = WorldCopies.seams(CYLINDER, -300, -5000, 300, 5000);
        assertEquals(Set.of(), horizontalAt(seams), "a cylinder drew a Z seam");
        assertEquals(Set.of(LOWER, LOWER + WIDTH), verticalAt(seams, -5000, 5000), "a cylinder lost its X seams");
    }

    private static Set<WorldCopies.Copy> expected(int skew, boolean zLoops, int minX, int minZ, int maxX, int maxZ) {
        Set<WorldCopies.Copy> copies = new HashSet<>();
        int rows = zLoops ? 20 : 0;
        for (int row = -rows; row <= rows; row++) {
            for (int column = -20; column <= 20; column++) {
                int dx = column * WIDTH + row * skew;
                int dz = row * WIDTH;
                boolean meetsZ = !zLoops || LOWER + dz < maxZ && LOWER + WIDTH + dz > minZ;
                if (meetsZ && LOWER + dx < maxX && LOWER + WIDTH + dx > minX) {
                    copies.add(new WorldCopies.Copy(dx, dz));
                }
            }
        }

        return copies;
    }

    private static Set<Integer> verticalAt(List<WorldCopies.Edge> seams, int fromZ, int toZ) {
        Set<Integer> xs = new HashSet<>();
        for (WorldCopies.Edge seam : seams) {
            if (seam.fromX() == seam.toX() && seam.fromZ() >= fromZ && seam.toZ() <= toZ) {
                xs.add(seam.fromX());
            }
        }

        return xs;
    }

    private static Set<Integer> horizontalAt(List<WorldCopies.Edge> seams) {
        Set<Integer> zs = new HashSet<>();
        for (WorldCopies.Edge seam : seams) {
            if (seam.fromZ() == seam.toZ()) {
                zs.add(seam.fromZ());
            }
        }

        return zs;
    }
}

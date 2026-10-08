package com.exoticworlds.compat.scalablelux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.world.level.ChunkPos;

class LightLockFoldsTest {
    private static final int OWNER = 7;
    private static final int RADIUS = 2;

    private static WorldFold torus(int widthChunks) {
        AxisBounds.Looped looped = new AxisBounds.Looped(0, widthChunks);
        return WorldFolds.of(FlatShape.torus(new WorldLoopBounds(looped, looped)));
    }

    private static ArrayList<Token> square(WorldFold fold, int centerX, int centerZ) {
        ArrayList<Token> tokens = new ArrayList<>();
        for (int i = -RADIUS; i <= RADIUS; i++) {
            for (int j = -RADIUS; j <= RADIUS; j++) {
                tokens.add(new Token(OWNER, LightLockFolds.foldKey(fold, ChunkPos.pack(centerX + i, centerZ + j))));
            }
        }

        return tokens;
    }

    @Test
    void aKeyPastTheSeamNamesThePhysicalChunk() {
        WorldFold fold = torus(32);
        assertEquals(ChunkPos.pack(1, 0), LightLockFolds.foldKey(fold, ChunkPos.pack(33, 0)));
        assertEquals(ChunkPos.pack(30, 31), LightLockFolds.foldKey(fold, ChunkPos.pack(-2, -1)));
        assertEquals(ChunkPos.pack(5, 9), LightLockFolds.foldKey(fold, ChunkPos.pack(5, 9)));
    }

    @Test
    void anUnwrappedOwnerKeepsTheRawKey() {
        assertEquals(ChunkPos.pack(40, -3), LightLockFolds.foldKey(null, ChunkPos.pack(40, -3)));
    }

    @Test
    void squaresOnOppositeSidesOfTheSeamShareTheirPhysicalChunks() {
        WorldFold fold = torus(32);
        Set<Token> last = new HashSet<>(square(fold, 31, 10));
        Set<Token> first = new HashSet<>(square(fold, 0, 10));
        last.retainAll(first);
        assertEquals(4 * 5, last.size());
    }

    @Test
    void aLapNarrowerThanTheSquareLeavesOneTokenPerPhysicalChunk() {
        ArrayList<Token> tokens = square(torus(3), 1, 1);
        ArrayList<Token> distinct = LightLockFolds.distinct(tokens);

        Set<Token> physical = new HashSet<>();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                physical.add(new Token(OWNER, ChunkPos.pack(x, z)));
            }
        }

        assertEquals(25, tokens.size());
        assertEquals(9, distinct.size());
        assertEquals(physical, new HashSet<>(distinct));
    }

    @Test
    void aSquareWithoutDuplicatesIsHandedBackAsIs() {
        ArrayList<Token> tokens = square(torus(32), 31, 0);
        assertSame(tokens, LightLockFolds.distinct(tokens));
    }

    private record Token(int ownerTag, long pos) {
    }
}

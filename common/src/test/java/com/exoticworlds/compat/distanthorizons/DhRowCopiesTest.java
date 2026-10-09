package com.exoticworlds.compat.distanthorizons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

class DhRowCopiesTest {
    private static final byte LEAF = DhKeys.LEAF;

    private static DhLattice lattice(int widthChunks, int skewChunks) {
        return DhLattice.of(WorldFolds.of(
                FlatShape.latticeTorus(WorldLoopBounds.ofWidths(widthChunks, widthChunks), skewChunks)));
    }

    private static DhLattice cylinder(int widthChunks) {
        AxisBounds.Looped looped = new AxisBounds.Looped(0, widthChunks);
        return DhLattice.of(WorldFolds.of(
                FlatShape.torus(new WorldLoopBounds(looped, AxisBounds.Unbounded.INSTANCE))));
    }

    private static ChunkPos key(DhLattice lattice, ChunkPos chunk) {
        return new ChunkPos(DhFold.foldChunkX(lattice, LEAF, chunk.x(), chunk.z()),
                DhFold.foldChunkZ(lattice, LEAF, chunk.z()));
    }

    private static long rowsOfThePeriod(DhLattice lattice) {
        DhFold.Period period = DhFold.period(lattice, LEAF);
        long lapsX = period.xBlocks() / lattice.shape().widthBlocks(Direction.Axis.X);
        long lapsZ = period.zBlocks() / lattice.shape().widthBlocks(Direction.Axis.Z);
        return lapsX * lapsZ;
    }

    @Test
    void aSkewOffTheLeafGridCopiesTheChunkOneZLapOnWithTheSkew() {
        assertEquals(List.of(new ChunkPos(6, 19)), DhRowCopies.copiesOf(lattice(32, 6), 0, -13));
    }

    @Test
    void aSkewOfOneChunkCopiesTheChunkIntoEachOfFourZLaps() {
        assertEquals(List.of(new ChunkPos(3, 69), new ChunkPos(4, 133), new ChunkPos(5, 197)),
                DhRowCopies.copiesOf(lattice(64, 1), 2, 5));
    }

    @Test
    void aWidthOffTheLeafGridCopiesTheChunkAlongBothAxes() {
        assertEquals(15, DhRowCopies.copiesOf(lattice(101, 0), 0, 0).size());
    }

    @Test
    void aShapeWhoseLeafPeriodIsOneLapNeedsNoCopy() {
        assertEquals(List.of(), DhRowCopies.copiesOf(lattice(64, 0), 7, 7));
        assertEquals(List.of(), DhRowCopies.copiesOf(lattice(64, 4), 7, 7));
        assertEquals(List.of(), DhRowCopies.copiesOf(cylinder(64), 7, 7));
    }

    @Test
    void theUnboundedAxisOfACylinderNeverCopies() {
        List<ChunkPos> copies = DhRowCopies.copiesOf(cylinder(101), 3, 9);
        assertEquals(3, copies.size());
        for (ChunkPos copy : copies) {
            assertEquals(9, copy.z());
        }
    }

    @Test
    void theChunkAndItsCopiesFillEveryRowOfThePeriodOnce() {
        for (DhLattice shape : new DhLattice[] {lattice(32, 6), lattice(64, 1), lattice(101, 0), lattice(50, 3)}) {
            for (int chunkX = -40; chunkX <= 40; chunkX += 13) {
                for (int chunkZ = -40; chunkZ <= 40; chunkZ += 11) {
                    List<ChunkPos> copies = DhRowCopies.copiesOf(shape, chunkX, chunkZ);
                    Set<ChunkPos> keys = new HashSet<>();
                    keys.add(key(shape, new ChunkPos(chunkX, chunkZ)));
                    for (ChunkPos copy : copies) {
                        assertTrue(keys.add(key(shape, copy)), "chunk " + chunkX + "," + chunkZ + " copy " + copy);
                        assertEquals(shape.shape().fold(new ChunkPos(chunkX, chunkZ)), shape.shape().fold(copy),
                                "chunk " + chunkX + "," + chunkZ + " copy " + copy);
                    }

                    assertEquals(rowsOfThePeriod(shape), keys.size(), "chunk " + chunkX + "," + chunkZ);
                }
            }
        }
    }

    @Test
    void theCopyPositionIsBoundOnlyWhileTheCopyIsBuilt() {
        ChunkPos copy = new ChunkPos(6, 19);
        assertSame(copy, DhRowCopies.buildAt(copy, DhRowCopies::pendingCopy));
        assertNull(DhRowCopies.pendingCopy());
    }
}

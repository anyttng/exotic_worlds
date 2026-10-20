package com.exoticworlds.client.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.world.phys.Vec3;

class SpawnPacketSeatTest {
    private static final int WORLD_CHUNKS = 16;
    private static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * CoordinateConstants.CHUNK_WIDTH;
    private static final int SKEW_CHUNKS = 3;
    private static final int SKEW_BLOCKS = SKEW_CHUNKS * CoordinateConstants.CHUNK_WIDTH;

    private static final WorldLoopBounds BOUNDS =
            ShapeFixtures.loopedBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);

    private static final WorldFold PER_AXIS = WorldFolds.of(ShapeFixtures.torus(BOUNDS));
    private static final WorldFold DECK_TORUS = new DeckGroupFold(ShapeFixtures.torus(BOUNDS),
            GenerationOptions.DEFAULT);
    private static final WorldFold LATTICE_TORUS = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS),
            GenerationOptions.DEFAULT);

    private static final List<WorldFold> FOLDS = List.of(PER_AXIS, DECK_TORUS);

    private static final Vec3 PLAYER = new Vec3(10.5, 64.0, -20.5);

    @Test
    void aSpawnALapAwayStandsOnTheCopyNearestThePlayer() {
        Vec3 stored = new Vec3(PLAYER.x + WORLD_BLOCKS + 3.0, 71.0, PLAYER.z - WORLD_BLOCKS + 2.0);
        for (WorldFold fold : FOLDS) {
            assertEquals(new Vec3(PLAYER.x + 3.0, 71.0, PLAYER.z + 2.0), SpawnPacketSeat.seated(fold, PLAYER, stored));
        }
    }

    @Test
    void aTranslatedSpawnComesBackAsItself() {
        Vec3 stored = new Vec3(PLAYER.x - 40.0, 70.0, PLAYER.z + 12.0);
        for (WorldFold fold : FOLDS) {
            assertSame(stored, SpawnPacketSeat.seated(fold, PLAYER, stored));
        }
    }

    @Test
    void aSpawnExactlyHalfAWorldAwayTakesNoLap() {
        for (WorldFold fold : FOLDS) {
            Vec3 alongX = PLAYER.add(WORLD_BLOCKS / 2.0, 0.0, 0.0);
            assertSame(alongX, SpawnPacketSeat.seated(fold, PLAYER, alongX));
            Vec3 alongZ = PLAYER.add(0.0, 0.0, -WORLD_BLOCKS / 2.0);
            assertSame(alongZ, SpawnPacketSeat.seated(fold, PLAYER, alongZ));
        }
    }

    @Test
    void aZLapOnTheLatticeTorusMovesXWithIt() {
        Vec3 stored = PLAYER.add(SKEW_BLOCKS + 1.0, 0.0, WORLD_BLOCKS + 1.0);
        assertEquals(PLAYER.add(1.0, 0.0, 1.0), SpawnPacketSeat.seated(LATTICE_TORUS, PLAYER, stored));
    }

    @Test
    void noPlayerOrNoFoldLeavesTheSpawnAsStored() {
        Vec3 stored = new Vec3(PLAYER.x + WORLD_BLOCKS, 64.0, PLAYER.z);
        assertSame(stored, SpawnPacketSeat.seated(DECK_TORUS, null, stored));
        assertSame(stored, SpawnPacketSeat.seated(null, PLAYER, stored));
    }
}

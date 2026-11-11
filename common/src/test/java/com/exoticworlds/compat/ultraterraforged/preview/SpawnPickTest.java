package com.exoticworlds.compat.ultraterraforged.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapShapes;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.WorldLoopSizes;

import net.minecraft.core.BlockPos;

class SpawnPickTest {
    private static final ToroidalShape TORUS = MapShapes.torus(-16, 16);
    private static final ToroidalShape CYLINDER = MapShapes.cylinder(-16, 16);
    private static final ToroidalShape SKEWED = MapShapes.latticeTorus(-16, 16, 4);

    @Test
    void aPickOnACopyLandsInsideTheWorld() {
        assertEquals(new SpawnPoint(-14, 2), SpawnPick.at(TORUS, new BlockPos(300, 0, 40)),
                "300 is block -212 one 512-block lap on, inside chunk -14");
        assertEquals(new SpawnPoint(-4, -14), SpawnPick.at(SKEWED, new BlockPos(0, 0, 300)),
                "crossing the Z edge moves X back by the 64-block skew");
    }

    @Test
    void theEndlessAxisStaysInsideTheSpawnBounds() {
        int max = WorldLoopSizes.maxSpawnChunk(WorldLoopSizes.ENDLESS_AXIS_WIDTH);
        assertEquals(new SpawnPoint(2, max), SpawnPick.at(CYLINDER, new BlockPos(40, 0, 100_000_000)));
    }

    @Test
    void thePinStandsOnThePickedChunksCenter() {
        assertEquals(new BlockPos(56, 0, -72), SpawnPick.pinOf(new SpawnPoint(3, -5)), "the center of chunk 3, -5");
    }
}

package com.exoticworlds.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.shape.StartupRegistrations;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.levelgen.Heightmap;

class BaseHeightsTest {
    private static final int LAP_CHUNKS = 64;
    private static final int LAP_BLOCKS = LAP_CHUNKS * 16;
    private static final Heightmap.Types TYPE = Heightmap.Types.WORLD_SURFACE_WG;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        StartupRegistrations.options();
    }

    @Test
    void aColumnAndItsCopiesALapAwayShareOneAnswer() {
        BaseHeights heights = heights();
        AtomicInteger computed = new AtomicInteger();

        heights.heightAt(5, 7, TYPE, computed::incrementAndGet);
        heights.heightAt(5 + LAP_BLOCKS, 7, TYPE, computed::incrementAndGet);
        heights.heightAt(5, 7 - LAP_BLOCKS, TYPE, computed::incrementAndGet);

        assertEquals(1, computed.get());
    }

    @Test
    void eachHeightmapTypeKeepsItsOwnAnswer() {
        BaseHeights heights = heights();

        heights.heightAt(5, 7, Heightmap.Types.WORLD_SURFACE_WG, () -> 70);

        assertEquals(40, heights.heightAt(5, 7, Heightmap.Types.OCEAN_FLOOR_WG, () -> 40));
    }

    @Test
    void aFullTypeIsClearedBeforeTheNextAnswer() {
        BaseHeights heights = heights();
        AtomicInteger firstColumn = new AtomicInteger();
        heights.heightAt(0, 0, TYPE, firstColumn::incrementAndGet);
        for (int i = 1; i <= BaseHeights.CAP_PER_TYPE; i++) {
            heights.heightAt(i % LAP_BLOCKS, i / LAP_BLOCKS, TYPE, () -> 0);
        }

        heights.heightAt(0, 0, TYPE, firstColumn::incrementAndGet);

        assertEquals(2, firstColumn.get());
    }

    private static BaseHeights heights() {
        return new BaseHeights(
                new CarriedShape(ShapeFixtures.torus(WorldLoopBounds.ofWidth(LAP_CHUNKS)), GenerationOptions.DEFAULT)
                        .fold());
    }
}

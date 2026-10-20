package com.exoticworlds.engine.seam;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.level.PublishedShapes;

import net.minecraft.world.level.Level;

class MapSeamFoldTest {
    private static final WorldFold PUBLISHED = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(64)));

    @AfterEach
    void forget() {
        PublishedShapes.clear();
    }

    @Test
    void withNoLevelAndNoServerTheMapTakesItsDimensionsPublishedFold() {
        PublishedShapes.publish(Level.OVERWORLD, PUBLISHED);

        assertSame(PUBLISHED, MapSeamFold.transformerFor(null, Level.OVERWORLD));
    }

    @Test
    void withNothingPublishedTheMapIsUnfolded() {
        assertNull(MapSeamFold.transformerFor(null, Level.OVERWORLD));
    }

    @Test
    void anotherDimensionsPublishedFoldIsNotTheMapsFold() {
        PublishedShapes.publish(Level.NETHER, PUBLISHED);

        assertNull(MapSeamFold.transformerFor(null, Level.OVERWORLD));
    }
}

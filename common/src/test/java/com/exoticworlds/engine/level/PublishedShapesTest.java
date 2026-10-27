package com.exoticworlds.engine.level;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

class PublishedShapesTest {
    private static final WorldFold WIDE = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(64)));
    private static final WorldFold NARROW = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(16)));
    private static final ResourceKey<Level> DATAPACK_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("cject", "hollow"));

    @AfterEach
    void forget() {
        PublishedShapes.clear();
    }

    @Test
    void aDimensionTheServerNeverPublishedAnswersNull() {
        PublishedShapes.publish(Level.OVERWORLD, WIDE);

        assertNull(PublishedShapes.wrappedFoldOf(Level.NETHER));
        assertNull(PublishedShapes.wrappedFoldOf(DATAPACK_DIMENSION));
    }

    @Test
    void eachDimensionAnswersItsOwnFold() {
        PublishedShapes.publish(Level.OVERWORLD, WIDE);
        PublishedShapes.publish(Level.NETHER, NARROW);
        PublishedShapes.publish(DATAPACK_DIMENSION, WIDE);

        assertSame(WIDE, PublishedShapes.wrappedFoldOf(Level.OVERWORLD));
        assertSame(NARROW, PublishedShapes.wrappedFoldOf(Level.NETHER));
        assertSame(WIDE, PublishedShapes.wrappedFoldOf(DATAPACK_DIMENSION));
    }

    @Test
    void aRectangleAnswersNullLikeAnUnloopedLevel() {
        PublishedShapes.publish(Level.END, WorldFolds.of(FlatShape.rectangle()));

        assertNull(PublishedShapes.wrappedFoldOf(Level.END));
    }

    @Test
    void aRepublishReplacesTheEarlierFold() {
        PublishedShapes.publish(Level.NETHER, WIDE);
        PublishedShapes.publish(Level.NETHER, NARROW);

        assertSame(NARROW, PublishedShapes.wrappedFoldOf(Level.NETHER));
    }

    @Test
    void clearForgetsEveryDimension() {
        PublishedShapes.publish(Level.OVERWORLD, WIDE);
        PublishedShapes.publish(Level.NETHER, NARROW);

        PublishedShapes.clear();

        assertNull(PublishedShapes.wrappedFoldOf(Level.OVERWORLD));
        assertNull(PublishedShapes.wrappedFoldOf(Level.NETHER));
    }
}

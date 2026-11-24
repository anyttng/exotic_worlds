package com.exoticworlds.engine.level;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;

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

        assertNull(PublishedShapes.getWrappedFold(Level.NETHER));
        assertNull(PublishedShapes.getWrappedFold(DATAPACK_DIMENSION));
    }

    @Test
    void eachDimensionAnswersItsOwnFold() {
        PublishedShapes.publish(Level.OVERWORLD, WIDE);
        PublishedShapes.publish(Level.NETHER, NARROW);
        PublishedShapes.publish(DATAPACK_DIMENSION, WIDE);

        assertSame(WIDE, PublishedShapes.getWrappedFold(Level.OVERWORLD));
        assertSame(NARROW, PublishedShapes.getWrappedFold(Level.NETHER));
        assertSame(WIDE, PublishedShapes.getWrappedFold(DATAPACK_DIMENSION));
    }

    @Test
    void aRectangleAnswersNullLikeAnUnloopedLevel() {
        PublishedShapes.publish(Level.END, WorldFolds.of(FlatShape.rectangle()));

        assertNull(PublishedShapes.getWrappedFold(Level.END));
    }

    @Test
    void aRepublishReplacesTheEarlierFold() {
        PublishedShapes.publish(Level.NETHER, WIDE);
        PublishedShapes.publish(Level.NETHER, NARROW);

        assertSame(NARROW, PublishedShapes.getWrappedFold(Level.NETHER));
    }

    @Test
    void clearForgetsEveryDimension() {
        PublishedShapes.publish(Level.OVERWORLD, WIDE);
        PublishedShapes.publish(Level.NETHER, NARROW);

        PublishedShapes.clear();

        assertNull(PublishedShapes.getWrappedFold(Level.OVERWORLD));
        assertNull(PublishedShapes.getWrappedFold(Level.NETHER));
    }
}

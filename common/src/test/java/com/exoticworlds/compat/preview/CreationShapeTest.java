package com.exoticworlds.compat.preview;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.feature.guaranteedland.GuaranteedLand;

class CreationShapeTest {
    private static final AxisBounds.Looped TINY = new AxisBounds.Looped(-16, 16);

    @Test
    void noShapeChosenSchedulesTheTaskItself() {
        Runnable task = CreationShapeTest::task;
        assertSame(task, CreationShape.underRouterBuild(null, task));
    }

    private static void task() {
    }

    @Test
    void aChosenShapeIsTheRouterBuildFoldWhileTheTaskRuns() {
        CarriedShape shown = new CarriedShape(null, ShapeFixtures.torus(new WorldLoopBounds(TINY, TINY)),
                ShapeOptions.DEFAULT);
        AtomicReference<WorldFold> seen = new AtomicReference<>();
        CreationShape.underRouterBuild(shown, () -> seen.set(CreationShape.getRouterBuildFold())).run();
        assertSame(shown.getFold(), seen.get());
        assertSame(WorldFolds.NOOP, CreationShape.getRouterBuildFold());
    }

    @Test
    void theOverworldFeaturesOfTheChosenShapeRunInTheRouterBuild() {
        StartupRegistrations.options();
        CarriedShape shown = new CarriedShape(DeclaredShapes.TORUS,
                ShapeFixtures.torus(new WorldLoopBounds(TINY, TINY)),
                ShapeOptions.DEFAULT.with(GuaranteedLand.FEATURE.getOption(), true));
        AtomicReference<List<GenerationFeature>> seen = new AtomicReference<>();
        CreationShape.underRouterBuild(shown,
                () -> seen.set(GenerationFoldContext.getContext().getRouterBuildFeatures())).run();
        assertTrue(seen.get().contains(GuaranteedLand.FEATURE), seen.get().toString());
    }
}

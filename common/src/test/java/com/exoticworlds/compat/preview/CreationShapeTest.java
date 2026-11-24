package com.exoticworlds.compat.preview;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;

class CreationShapeTest {
    private static final AxisBounds.Looped TINY = new AxisBounds.Looped(-16, 16);

    @Test
    void noShapeChosenSchedulesTheTaskItself() {
        Runnable task = CreationShapeTest::task;
        assertSame(task, CreationShape.underRouterBuild(null, task));
        assertSame(task, CreationShape.underRouterBuild(WorldFolds.NOOP, task));
    }

    private static void task() {
    }

    @Test
    void aChosenShapeIsTheRouterBuildFoldWhileTheTaskRuns() {
        WorldFold fold = WorldFolds.of(ShapeFixtures.torus(new WorldLoopBounds(TINY, TINY)));
        AtomicReference<WorldFold> seen = new AtomicReference<>();
        CreationShape.underRouterBuild(fold, () -> seen.set(CreationShape.getRouterBuildFold())).run();
        assertSame(fold, seen.get());
        assertSame(WorldFolds.NOOP, CreationShape.getRouterBuildFold());
    }
}

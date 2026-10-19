package com.exoticworlds.core;

import java.util.List;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

public final class WorldFoldFixture {
    public static final WorldLoopBounds SQUARE = ShapeFixtures.loopedBounds(-32, 32, -32, 32);
    public static final WorldLoopBounds ODD_BOUNDS = ShapeFixtures.loopedBounds(-2, 3, -2, 3);
    public static final WorldLoopBounds UNEVEN_BOUNDS = ShapeFixtures.loopedBounds(-48, 16, 0, 16);
    public static final WorldLoopBounds UNIT_BOUNDS = ShapeFixtures.loopedBounds(0, 1, 0, 1);
    public static final WorldLoopBounds X_ONLY_BOUNDS =
            new WorldLoopBounds(new AxisBounds.Looped(-32, 32), AxisBounds.Unbounded.INSTANCE);

    public static final WorldFold EVEN = new WorldLoopTransformer(SQUARE, GenerationOptions.DEFAULT);
    public static final WorldFold ODD = new WorldLoopTransformer(ODD_BOUNDS, GenerationOptions.DEFAULT);
    public static final WorldFold UNEVEN = new WorldLoopTransformer(UNEVEN_BOUNDS, GenerationOptions.DEFAULT);
    public static final WorldFold UNIT = new WorldLoopTransformer(UNIT_BOUNDS, GenerationOptions.DEFAULT);
    public static final WorldFold X_ONLY = new WorldLoopTransformer(X_ONLY_BOUNDS, GenerationOptions.DEFAULT);

    public static final List<WorldFold> PER_AXIS = List.of(EVEN, ODD, UNEVEN, X_ONLY, WorldFolds.NOOP);

    private WorldFoldFixture() {
    }
}

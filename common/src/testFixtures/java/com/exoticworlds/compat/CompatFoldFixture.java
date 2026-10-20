package com.exoticworlds.compat;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.FlatShape;

import net.minecraft.core.Direction;

public final class CompatFoldFixture {
    public static final int WORLD_CHUNKS = 16;
    public static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;
    public static final int SKEW_CHUNKS = 4;
    public static final int MIRROR_LINE_CHUNK = 3;
    public static final int MIRROR_LINE_BLOCKS = MIRROR_LINE_CHUNK * 16;

    public static final WorldLoopBounds BOUNDS =
            ShapeFixtures.loopedBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);
    public static final WorldLoopBounds X_ONLY =
            new WorldLoopBounds(new AxisBounds.Looped(-WORLD_CHUNKS, WORLD_CHUNKS), AxisBounds.Unbounded.INSTANCE);

    public static final WorldFold PER_AXIS = WorldFolds.of(ShapeFixtures.torus(BOUNDS));
    public static final WorldFold DECK_TORUS = new DeckGroupFold(ShapeFixtures.torus(BOUNDS), GenerationOptions.DEFAULT);
    public static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS), GenerationOptions.DEFAULT);
    public static final WorldFold MIRRORED =
            new DeckGroupFold(ShapeFixtures.mirrored(BOUNDS, Direction.Axis.Z, MIRROR_LINE_CHUNK), GenerationOptions.DEFAULT);
    public static final WorldFold CYLINDER = WorldFolds.of(ShapeFixtures.cylinder(X_ONLY));
    public static final WorldFold DECK_CYLINDER = new DeckGroupFold(ShapeFixtures.cylinder(X_ONLY), GenerationOptions.DEFAULT);

    private CompatFoldFixture() {
    }
}

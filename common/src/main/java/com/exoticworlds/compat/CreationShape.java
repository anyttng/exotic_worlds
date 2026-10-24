package com.exoticworlds.compat;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.shape.WorldShapes;

import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.world.level.dimension.LevelStem;

public final class CreationShape {
    public static @Nullable CarriedShape carried(WorldCreationContext settings) {
        return WorldShapes.applyAtCreation(settings.worldgenLoadContext(), settings.selectedDimensions())
                .get(LevelStem.OVERWORLD)
                .map(stem -> ShapedChunkGenerator.carriedShapeOf(stem.generator()))
                .orElse(null);
    }

    public static Runnable underRouterBuild(@Nullable WorldFold fold, Runnable task) {
        return () -> GenerationTransformerContext.withRouterBuild(fold, () -> {
            task.run();
            return null;
        });
    }

    public static WorldFold routerBuildFold() {
        WorldFold fold = GenerationTransformerContext.context().routerBuildTransformer();
        return fold != null ? fold : WorldFolds.NOOP;
    }

    private CreationShape() {
    }
}

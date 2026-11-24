package com.exoticworlds.compat.preview;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.gen.ShapedChunkGenerator;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.shape.WorldShapes;

import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.world.level.dimension.LevelStem;

public final class CreationShape {
    public static @Nullable CarriedShape carried(WorldCreationContext settings) {
        return WorldShapes.applyAtCreation(settings.worldgenLoadContext(), settings.selectedDimensions())
                .get(LevelStem.OVERWORLD)
                .map(stem -> ShapedChunkGenerator.getCarriedShape(stem.generator()))
                .orElse(null);
    }

    public static Runnable underRouterBuild(@Nullable WorldFold fold, Runnable task) {
        if (fold == null || !fold.isWrapped()) {
            return task;
        }

        return () ->GenerationFoldContext.withRouterBuild(fold, () -> {
            task.run();
            return null;
        });
    }

    public static WorldFold getRouterBuildFold() {
        WorldFold fold = GenerationFoldContext.getContext().getRouterBuildFold();
        return fold != null ? fold : WorldFolds.NOOP;
    }

    private CreationShape() {
    }
}

package com.exoticworlds.compat.preview;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.gen.GeneratorShapes;
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
                .map(stem -> GeneratorShapes.getCarriedShape(stem.generator()))
                .orElse(null);
    }

    public static Runnable underRouterBuild(@Nullable CarriedShape shown, Runnable task) {
        if (shown == null || !shown.getFold().isWrapped()) {
            return task;
        }

        List<GenerationFeature> features = ShapeFeatures.activeFeatures(shown, LevelStem.OVERWORLD);
        return () -> GenerationFoldContext.withRouterBuild(shown.getFold(), features, () -> {
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

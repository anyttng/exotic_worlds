package com.exoticworlds.bridge.v2.feature;

import com.exoticworlds.api.v2.feature.MomentV2;
import com.exoticworlds.api.v2.feature.OptionFeatureV2;
import com.exoticworlds.api.v2.geometry.WorldGeometryV2;
import com.exoticworlds.bridge.v2.geometry.GeometryBridge;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.CarriedShape;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.LevelStem;

abstract class MomentView implements MomentV2 {
    private final WorldFold fold;
    private final ResourceKey<LevelStem> dimension;
    private final ShapeOptions options;

    MomentView(WorldFold fold, ResourceKey<LevelStem> dimension, ShapeOptions options) {
        this.fold = fold;
        this.dimension = dimension;
        this.options = options;
    }

    MomentView(ServerLevel level) {
        this(WorldLoopAttachments.getFold(level), Registries.levelToLevelStem(level.dimension()), optionsOf(level));
    }

    private static ShapeOptions optionsOf(ServerLevel level) {
        CarriedShape carried = GeneratorShapes.getCarriedShape(level);
        return carried == null ? ShapeOptions.DEFAULT : carried.getOptions();
    }

    @Override
    public final WorldGeometryV2 getGeometry() {
        return GeometryBridge.of(this.fold);
    }

    @Override
    public final ResourceKey<LevelStem> getDimension() {
        return this.dimension;
    }

    @Override
    public final <T> T getValue(OptionFeatureV2<T> feature) {
        return this.options.get(FeatureBridge.optionOf(feature));
    }
}

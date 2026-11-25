package com.exoticworlds.bridge.v2.shape;

import java.util.List;
import java.util.Optional;

import com.exoticworlds.api.v2.shape.DimensionContextV2;
import com.exoticworlds.api.v2.shape.DimensionShapeV2;
import com.exoticworlds.bridge.v2.option.ShapeOptionsAdapter;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.WorldShape;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.exoticworlds.shape.dimension.DimensionShape;
import com.exoticworlds.shape.option.DimensionOption;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

final class DimensionShapeView implements DimensionShape {
    private final DimensionShapeV2 declared;

    DimensionShapeView(DimensionShapeV2 declared) {
        this.declared = declared;
    }

    @Override
    public Identifier getId() {
        return this.declared.getId();
    }

    @Override
    public Component getName() {
        return WorldShape.label(this.getId());
    }

    @Override
    public Component getHint() {
        return WorldShape.hint(this.getId());
    }

    @Override
    public Component describe(ShapeOptions options) {
        return this.getName();
    }

    @Override
    public FlatShape spans(DimensionContext context) {
        return ShapeSpansAdapter.flatShape(this.declared.spans(toV2(context)));
    }

    @Override
    public List<DimensionOption<?>> getParameters() {
        return List.of();
    }

    @Override
    public ShapeOptions defaultOptions(ResourceKey<LevelStem> dimension) {
        return ShapeOptions.DEFAULT;
    }

    @Override
    public boolean canTakePresets() {
        return this.declared.canTakePresets();
    }

    @Override
    public ShapeOptions withPreset(WorldLoopPresets preset, ShapeOptions options) {
        return ShapeOptionsAdapter.fromV2(
                this.declared.withPreset(preset.getWidthChunks(), ShapeOptionsAdapter.toV2(options)));
    }

    @Override
    public Optional<ShapeOptions> optionsFor(FlatShape spans) {
        return Optional.empty();
    }

    private static DimensionContextV2 toV2(DimensionContext context) {
        FlatShape overworld = context.overworld();
        return new DimensionContextV2(context.dimension(), ShapeOptionsAdapter.toV2(context.options()),
                overworld == null ? null : ShapeSpansAdapter.loopSpans(overworld), context.overworldScale(),
                context.coordinateScale());
    }
}

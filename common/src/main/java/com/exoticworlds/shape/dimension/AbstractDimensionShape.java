package com.exoticworlds.shape.dimension;

import java.util.List;
import java.util.Optional;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.registry.StartupRegistry;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.feature.ShapeFeature;
import com.exoticworlds.shape.option.DimensionOption;
import com.exoticworlds.shape.option.DimensionScope;
import com.exoticworlds.shape.option.ScopedOption;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public abstract class AbstractDimensionShape implements DimensionShape {
    private final Identifier id;
    private final ShapeLogic logic;
    private final ShapeParameters parameters;
    private final StartupRegistry<String, ScopedOption> options;

    protected AbstractDimensionShape(Identifier id, ShapeLogic logic, ShapeParameters parameters) {
        this.id = id;
        this.logic = logic;
        this.parameters = parameters;
        this.options = new StartupRegistry<>("Options of shape " + id);
    }

    public final void registerFeature(ShapeFeature<?> feature, DimensionScope scope) {
        this.options.register(feature.getOption().key(), new ScopedOption(feature.getShown(), scope));
    }

    @Override
    public final Identifier getId() {
        return this.id;
    }

    @Override
    public final FlatShape spans(DimensionContext context) {
        return this.logic.spans(context);
    }

    @Override
    public final List<DimensionOption<?>> getParameters() {
        return this.parameters.getAll();
    }

    @Override
    public final List<ScopedOption> getOptions() {
        return List.copyOf(this.options.getEntries().values());
    }

    @Override
    public final ShapeOptions defaultOptions(ResourceKey<LevelStem> dimension) {
        return this.parameters.defaultOptions(dimension);
    }

    @Override
    public final boolean canTakePresets() {
        return this.logic.canTakePresets();
    }

    @Override
    public final ShapeOptions withPreset(WorldLoopPresets preset, ShapeOptions options) {
        return this.logic.withPreset(preset, options);
    }

    @Override
    public final Optional<ShapeOptions> optionsFor(FlatShape spans) {
        return this.logic.optionsFor(spans);
    }
}

package com.exoticworlds.bridge.v2.shape;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exoticworlds.core.world.StemTerrain;
import com.exoticworlds.api.v2.shape.ParameterV2;
import com.exoticworlds.api.v2.shape.ShapeV2;
import com.exoticworlds.bridge.v2.feature.FeatureBridge;
import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.dimension.ShapeParameters;
import com.exoticworlds.shape.option.DimensionOption;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

final class BridgedParameters implements ShapeParameters {
    private static final int AFTER_THE_MODS_OWN_OPTIONS = Integer.MAX_VALUE;

    private final Identifier shape;
    private final List<ParameterV2<?>> declared;
    private final Map<ParameterV2<?>, ShapeOption<?>> options = new LinkedHashMap<>();
    private final List<DimensionOption<?>> shown = new ArrayList<>();

    BridgedParameters(ShapeV2 shape) {
        this.shape = shape.getId();
        this.declared = List.copyOf(shape.getParameters());
        this.declared.forEach(this::add);
    }

    private <T> void add(ParameterV2<T> parameter) {
        ShapeOption<T> option = new ShapeOption<>(parameter.getId().toString(), AFTER_THE_MODS_OWN_OPTIONS,
                parameter.getCodec(), parameter.getStoredDefault());
        this.options.put(parameter, option);
        this.shown.add(DimensionOption.of(option, parameter.getCaptionKey(),
                FeatureBridge.toInternal(parameter.getParts())));
    }

    void registerOptions() {
        this.options.values().forEach(ShapeOptionRegistry::register);
    }

    @SuppressWarnings("unchecked")
    <T> ShapeOption<T> optionOf(ParameterV2<T> parameter) {
        ShapeOption<?> option = this.options.get(parameter);
        if (option == null) {
            throw new IllegalStateException("The parameter " + parameter.getId() + " is not one of the shape "
                    + this.shape + ": return it from getParameters() of that shape.");
        }

        return (ShapeOption<T>) option;
    }

    boolean isTakingPresets() {
        return this.declared.stream().anyMatch(ParameterV2::isTakingPresets);
    }

    ShapeOptions withPresetWidth(ShapeOptions options, int widthChunks) {
        ShapeOptions rewritten = options;
        for (ParameterV2<?> parameter : this.declared) {
            rewritten = withPresetWidth(parameter, rewritten, widthChunks);
        }

        return rewritten;
    }

    private <T> ShapeOptions withPresetWidth(ParameterV2<T> parameter, ShapeOptions options, int widthChunks) {
        ShapeOption<T> option = optionOf(parameter);
        return options.with(option, parameter.withPresetWidth(options.get(option), widthChunks));
    }

    @Override
    public List<DimensionOption<?>> getAll() {
        return List.copyOf(this.shown);
    }

    @Override
    public ShapeOptions defaultOptions(StemTerrain stem) {
        ShapeOptions defaults = ShapeOptions.DEFAULT;
        for (ParameterV2<?> parameter : this.declared) {
            defaults = withNewWorldValue(parameter, defaults, stem.stem());
        }

        return defaults;
    }

    private <T> ShapeOptions withNewWorldValue(ParameterV2<T> parameter, ShapeOptions options,
            ResourceKey<LevelStem> dimension) {
        return options.with(optionOf(parameter), parameter.newWorldValue(dimension));
    }
}

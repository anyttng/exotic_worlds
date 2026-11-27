package com.exoticworlds.bridge.v2.shape;

import com.exoticworlds.bridge.v2.geometry.ShapeSpansAdapter;
import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.shape.DerivedDimensionV2;
import com.exoticworlds.api.v2.geometry.LoopSpansV2;
import com.exoticworlds.api.v2.shape.ParameterV2;
import com.exoticworlds.api.v2.shape.ShapeDimensionV2;
import com.exoticworlds.api.v2.shape.ShapeV2;
import com.exoticworlds.api.v2.shape.ShapeValuesV2;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.DerivedDimension;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.LegacyDerivation;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.dimension.AbstractDimensionShape;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.exoticworlds.shape.dimension.ShapeLogic;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

final class BridgedShape extends AbstractDimensionShape {
    private final ShapeV2 shape;
    private final BridgedParameters parameters;

    BridgedShape(ShapeV2 shape, BridgedParameters parameters) {
        super(shape.getId(), new Logic(shape, parameters), parameters);
        this.shape = shape;
        this.parameters = parameters;
    }

    @Override
    public Component getName() {
        return Component.translatable(this.shape.getNameKey());
    }

    @Override
    public Component getHint() {
        return Component.translatable(this.shape.getHintKey());
    }

    @Override
    public Component describe(ShapeOptions options) {
        return this.shape.describe(new Values(this.parameters, options));
    }

    private record Logic(ShapeV2 shape, BridgedParameters parameters) implements ShapeLogic {
        @Override
        public FlatShape spans(DimensionContext context) {
            return ShapeSpansAdapter.flatShape(this.shape.spansIn(new Dimension(this.parameters, context)));
        }

        @Override
        public boolean canTakePresets() {
            return this.parameters.isTakingPresets();
        }

        @Override
        public ShapeOptions withPreset(WorldLoopPresets preset, ShapeOptions options) {
            return this.parameters.withPresetWidth(options, preset.getWidthChunks());
        }

        @Override
        public @Nullable FlatShape derive(FlatShape overworld, DerivedDimension dimension) {
            LoopSpansV2 derived = this.shape.derivedSpans(new Derived(overworld, dimension));
            return derived == null ? null : ShapeSpansAdapter.flatShape(derived);
        }
    }

    private record Values(BridgedParameters parameters, ShapeOptions options) implements ShapeValuesV2 {
        @Override
        public <T> T getValue(ParameterV2<T> parameter) {
            return this.options.get(this.parameters.optionOf(parameter));
        }
    }

    private record Dimension(BridgedParameters parameters, DimensionContext context) implements ShapeDimensionV2 {
        @Override
        public <T> T getValue(ParameterV2<T> parameter) {
            return this.context.options().get(this.parameters.optionOf(parameter));
        }

        @Override
        public ResourceKey<LevelStem> getDimension() {
            return this.context.dimension();
        }

        @Override
        public @Nullable LoopSpansV2 getOverworld() {
            return this.context.overworld() instanceof FlatShape overworld
                    ? ShapeSpansAdapter.loopSpans(overworld)
                    : null;
        }

        @Override
        public double getOverworldScale() {
            return this.context.overworldScale();
        }

        @Override
        public double getCoordinateScale() {
            return this.context.coordinateScale();
        }
    }

    private record Derived(FlatShape overworld, DerivedDimension dimension) implements DerivedDimensionV2 {
        @Override
        public ResourceKey<LevelStem> getDimension() {
            return this.dimension.dimension();
        }

        @Override
        public LoopSpansV2 getOverworld() {
            return ShapeSpansAdapter.loopSpans(this.overworld);
        }

        @Override
        public double getOverworldScale() {
            return this.dimension.overworldScale();
        }

        @Override
        public double getCoordinateScale() {
            return this.dimension.coordinateScale();
        }

        @Override
        public @Nullable LoopSpansV2 scaledCopy() {
            FlatShape scaled = LegacyDerivation.derive(this.overworld, this.dimension.overworldScale(),
                    this.dimension.coordinateScale());
            return scaled == null ? null : ShapeSpansAdapter.loopSpans(scaled);
        }
    }
}

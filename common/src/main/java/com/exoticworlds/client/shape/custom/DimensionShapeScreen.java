package com.exoticworlds.client.shape.custom;

import java.util.Objects;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.client.shape.LoopSettingsScreen;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.shape.custom.CustomSettings;
import com.exoticworlds.shape.custom.CustomStem;
import com.exoticworlds.shape.custom.DimensionShape;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;

public class DimensionShapeScreen extends LoopSettingsScreen<DimensionShape, DimensionSizeControls> {

    public DimensionShapeScreen(Screen parent, CustomStem dimension, DimensionShape current,
            Consumer<DimensionShape> onDone) {
        super(CommonComponents.optionNameValue(ShapeLabels.dimension(dimension), ShapeLabels.kind(current.kind())),
                parent,
                onChange -> new DimensionSizeControls(dimension,
                        Objects.requireNonNull(current.spans(), "Customize opens only for a wrapped dimension"),
                        current.options().get(SpawnPoint.OPTION), onChange),
                current.options(), CustomSettings.offeredOptions(dimension, current.kind()), onDone);
    }

    @Override
    protected DimensionShape build() {
        SpawnPoint spawn = this.controls.spawn();
        return new DimensionShape(this.controls.spans(), spawn == null ? this.committedOptions()
                : this.committedOptions().with(SpawnPoint.OPTION, spawn));
    }

    @Override
    protected OptionContext newOptionContext() {
        return new ScreenContext();
    }

    private final class ScreenContext extends OptionContext {
        @Override
        public @Nullable Integer loopChunkWidth() {
            Integer x = this.loopChunkWidth(Direction.Axis.X);
            Integer z = this.loopChunkWidth(Direction.Axis.Z);
            if (x == null || z == null) {
                return x != null ? x : z;
            }

            return Math.min(x, z);
        }

        @Override
        public @Nullable Integer loopChunkWidth(Direction.Axis axis) {
            LoopSpans spans = this.loopSpans();
            return spans != null && spans.loops(axis) ? spans.chunkWidth(axis) : null;
        }

        @Override
        public @Nullable LoopSpans loopSpans() {
            return DimensionShapeScreen.this.controls.effectiveSpans();
        }
    }
}

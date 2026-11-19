package com.exoticworlds.client.shape.climate;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

import org.jspecify.annotations.Nullable;

import com.ascendiacore.gui.base.property.Value;
import com.ascendiacore.gui.layout.Column;
import com.ascendiacore.gui.theme.Theme;
import com.ascendiacore.gui.widget.cyclebutton.CycleButton;
import com.ascendiacore.gui.widget.field.Field;
import com.ascendiacore.gui.widget.numberfield.NumberField;
import com.ascendiacore.gui.widget.numberfield.model.Bounds;
import com.ascendiacore.gui.widget.panel.model.FillStyle;
import com.ascendiacore.gui.widget.textfield.TextField;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class CompactBiomesFields {
    private static final String KEY = "gui.exotic_worlds.world_shape_settings.compact_biomes";
    private static final Component FACTOR = Component.translatable(KEY + ".factor");
    private static final Optional<Bounds> FACTOR_BOUNDS =
            Optional.of(new Bounds(ClimateScale.CUSTOM_MIN, ClimateScale.CUSTOM_MAX));
    private static final int FACTOR_MAX_LENGTH = 5;
    private static final int GAP = 4;
    private static final String UNKNOWN_FACTOR = "—";
    private static final String FRACTIONAL_FACTOR_FORMAT = "%.2f";

    private final @Nullable Screen parent;
    private final Value<ClimateScale.Mode> mode = Value.of(ClimateScale.Mode.OFF);
    private final NumberField factor;
    private final TextField preview;
    private final Column section = new Column();
    private @Nullable ClimateScale previewedScale;
    private @Nullable LoopSpans previewedSpans;

    public CompactBiomesFields(Theme theme, FillStyle fill, @Nullable Screen parent, Runnable onChange) {
        this.parent = parent;
        CycleButton<ClimateScale.Mode> modeButton = new CycleButton<>(theme.button(), Component.translatable(KEY),
                () -> List.of(ClimateScale.Mode.values()), this.mode, CompactBiomesFields::modeName);
        modeButton.setHint(theme.tooltip(), KEY, ClimateScale.Mode::getSerializedName);
        modeButton.setOnChange(picked -> onChange.run());

        this.factor = new NumberField(fill, theme.textField(), FACTOR, () -> FACTOR_BOUNDS, onChange);
        this.factor.setMaxLength(FACTOR_MAX_LENGTH);
        this.factor.setValue(ClimateScale.STRONG_FACTOR);
        this.factor.setVisible(() -> this.mode.get() == ClimateScale.Mode.CUSTOM);

        this.preview = new TextField(theme.textField(), edited -> {
        });
        Field previewField = new Field(fill, FACTOR, this.preview);
        previewField.setEnabled(false);
        previewField.setVisible(() -> this.mode.get() == ClimateScale.Mode.AUTO
                || this.mode.get() == ClimateScale.Mode.STRONG);

        this.section.setGap(GAP);
        this.section.add(modeButton);
        this.section.add(this.factor);
        this.section.add(previewField);
    }

    public Column section() {
        return this.section;
    }

    public void show(ClimateScale scale) {
        this.mode.set(scale.mode());
        this.factor.setValue(scale.factor());
    }

    public @Nullable ClimateScale scale() {
        if (this.mode.get() != ClimateScale.Mode.CUSTOM) {
            return ClimateScale.OFF.withMode(this.mode.get());
        }

        Integer chosen = this.factor.value();
        return chosen == null ? null : ClimateScale.custom(chosen);
    }

    public void preview(@Nullable LoopSpans spans, GenerationOptions options) {
        ClimateScale scale = this.scale();
        if (Objects.equals(scale, this.previewedScale) && Objects.equals(spans, this.previewedSpans)) {
            return;
        }

        this.previewedScale = scale;
        this.previewedSpans = spans;
        Screen screen = this.parent;
        this.preview.setValue(screen == null || spans == null || scale == null
                ? UNKNOWN_FACTOR
                : display(ClimateFactorPreview.temperatureFactor(screen,
                        options.with(CompactBiomes.OPTION, scale), spans)));
    }

    private static String display(OptionalDouble factor) {
        if (factor.isEmpty()) {
            return UNKNOWN_FACTOR;
        }

        double value = factor.getAsDouble();
        return value == Math.rint(value)
                ? Integer.toString((int) value)
                : String.format(Locale.ROOT, FRACTIONAL_FACTOR_FORMAT, value);
    }

    private static Component modeName(ClimateScale.Mode mode) {
        return mode == ClimateScale.Mode.OFF
                ? CommonComponents.OPTION_OFF
                : Component.translatable(KEY + "." + mode.getSerializedName());
    }
}

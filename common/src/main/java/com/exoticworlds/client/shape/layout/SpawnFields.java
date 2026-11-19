package com.exoticworlds.client.shape.layout;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.ascendiacore.gui.base.property.Value;
import com.ascendiacore.gui.layout.Column;
import com.ascendiacore.gui.layout.Row;
import com.ascendiacore.gui.layout.model.Length;
import com.ascendiacore.gui.theme.Theme;
import com.ascendiacore.gui.widget.cyclebutton.CycleButton;
import com.ascendiacore.gui.widget.label.Label;
import com.ascendiacore.gui.widget.label.model.LabelRole;
import com.ascendiacore.gui.widget.numberfield.NumberField;
import com.ascendiacore.gui.widget.numberfield.model.Bounds;
import com.ascendiacore.gui.widget.panel.model.FillStyle;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.WorldLoopSizes;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

final class SpawnFields {
    private static final int PAIR_GAP = 6;
    private static final int MAX_LENGTH = 8;

    private final Value<SpawnPoint.Mode> mode = Value.of(SpawnPoint.Mode.DEFAULT);
    private final NumberField x;
    private final NumberField z;
    private final Column section = new Column();

    SpawnFields(Theme theme, FillStyle fill, Function<Direction.Axis, @Nullable Integer> chunkWidth, Runnable onChange) {
        this.x = chunkField(theme, fill, Direction.Axis.X, chunkWidth, onChange);
        this.z = chunkField(theme, fill, Direction.Axis.Z, chunkWidth, onChange);
        CycleButton<SpawnPoint.Mode> modeButton = new CycleButton<>(theme.button(),
                Component.translatable(SettingsLines.SPAWN_KEY), () -> List.of(SpawnPoint.Mode.values()), this.mode,
                SettingsLines::spawnModeName);
        modeButton.setHint(theme.tooltip(), SettingsLines.SPAWN_KEY, SpawnPoint.Mode::getSerializedName);
        modeButton.setOnChange(picked -> onChange.run());

        Row point = new Row();
        point.setGap(PAIR_GAP);
        point.add(this.x, Length.fill());
        point.add(this.z, Length.fill());
        point.setEnabled(() -> this.mode.get() == SpawnPoint.Mode.CUSTOM);

        this.section.setGap(LoopFields.GAP);
        this.section.add(modeButton);
        this.section.add(new Label(fill, LabelRole.NORMAL, SettingsLines.SPAWN_POINT));
        this.section.add(point);
    }

    Column section() {
        return this.section;
    }

    void show(SpawnPoint spawn) {
        this.mode.set(spawn.mode());
        this.x.setValue(spawn.xChunk());
        this.z.setValue(spawn.zChunk());
    }

    @Nullable SpawnPoint point() {
        if (this.mode.get() == SpawnPoint.Mode.DEFAULT) {
            return SpawnPoint.DEFAULT;
        }

        Integer xChunk = this.x.value();
        Integer zChunk = this.z.value();
        return xChunk == null || zChunk == null ? null : new SpawnPoint(xChunk, zChunk);
    }

    private static NumberField chunkField(Theme theme, FillStyle fill, Direction.Axis axis,
            Function<Direction.Axis, @Nullable Integer> chunkWidth, Runnable onChange) {
        NumberField field = new NumberField(fill, theme.textField(), SettingsLines.axisName(axis),
                () -> bounds(chunkWidth.apply(axis)), onChange);
        field.setSigned(true);
        field.setMaxLength(MAX_LENGTH);
        return field;
    }

    private static Optional<Bounds> bounds(@Nullable Integer chunkWidth) {
        return chunkWidth == null
                ? Optional.empty()
                : Optional.of(new Bounds(WorldLoopSizes.minSpawnChunk(chunkWidth), WorldLoopSizes.maxSpawnChunk(chunkWidth)));
    }
}

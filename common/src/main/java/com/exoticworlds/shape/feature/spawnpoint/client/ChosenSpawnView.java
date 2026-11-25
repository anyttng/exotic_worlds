package com.exoticworlds.shape.feature.spawnpoint.client;

import java.util.List;
import java.util.Optional;

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
import com.exoticworlds.shape.feature.client.FeaturePanel;
import com.exoticworlds.shape.feature.client.FeatureView;
import com.exoticworlds.shape.feature.client.FeatureViews;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.dimension.LoopSizeLines;
import com.exoticworlds.shape.feature.spawnpoint.ChosenSpawn;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

public final class ChosenSpawnView implements FeatureView<SpawnPoint> {
    private static final Component POINT = Component.translatable(ChosenSpawn.CAPTION_KEY + ".point");
    private static final String KEY_SEPARATOR = ".";
    private static final int SECTION_GAP_PIXELS = 4;
    private static final int PAIR_GAP_PIXELS = 6;
    private static final int MAX_LENGTH = 8;

    public static void register() {
        FeatureViews.register(SpawnPoint.OPTION, new ChosenSpawnView());
    }

    @Override
    public void extend(FeaturePanel<SpawnPoint> panel) {
        Theme theme = panel.getTheme();
        FillStyle fill = panel.getFill();
        Value<SpawnPoint.Mode> mode = Value.of(SpawnPoint.Mode.DEFAULT);
        NumberField x = chunkField(panel, Direction.Axis.X);
        NumberField z = chunkField(panel, Direction.Axis.Z);
        CycleButton<SpawnPoint.Mode> modeButton = new CycleButton<>(theme.button(),
                Component.translatable(ChosenSpawn.CAPTION_KEY), () -> List.of(SpawnPoint.Mode.values()), mode,
                ChosenSpawnView::modeName);
        modeButton.setHint(theme.tooltip(), ChosenSpawn.CAPTION_KEY, SpawnPoint.Mode::getSerializedName);

        Row point = new Row();
        point.setGapPixels(PAIR_GAP_PIXELS);
        point.add(x, Length.fill());
        point.add(z, Length.fill());
        point.setEnabled(() -> mode.get() == SpawnPoint.Mode.CUSTOM);

        Column section = new Column();
        section.setGapPixels(SECTION_GAP_PIXELS);
        section.add(modeButton);
        section.add(new Label(fill, LabelRole.NORMAL, POINT));
        section.add(point);
        section.setVisible(() -> panel.isOffered() && panel.getSpans() != null);
        panel.add(section);

        panel.onShow(shown -> {
            mode.set(shown.mode());
            x.setValue(shown.chunkX());
            z.setValue(shown.chunkZ());
        });
        panel.takeValueFrom(() -> pointOf(mode.get(), x, z));
    }

    private static @Nullable SpawnPoint pointOf(SpawnPoint.Mode mode, NumberField x, NumberField z) {
        if (mode == SpawnPoint.Mode.DEFAULT) {
            return SpawnPoint.DEFAULT;
        }

        Integer chunkX = x.getValue();
        Integer chunkZ = z.getValue();
        return chunkX == null || chunkZ == null ? null : new SpawnPoint(chunkX, chunkZ);
    }

    private static NumberField chunkField(FeaturePanel<SpawnPoint> panel, Direction.Axis axis) {
        NumberField field = new NumberField(panel.getFill(), panel.getTheme().textField(), LoopSizeLines.axisName(axis),
                () -> boundsOf(panel.getSpans(), axis), () -> {
                });
        field.setSigned(true);
        field.setMaxCharacterCount(MAX_LENGTH);
        return field;
    }

    private static Optional<Bounds> boundsOf(@Nullable FlatShape spans, Direction.Axis axis) {
        if (spans == null) {
            return Optional.empty();
        }

        int widthChunks = spans.bounds().isLooped(axis)
                ? spans.bounds().getWidthChunks(axis)
                : WorldLoopSizes.ENDLESS_AXIS_WIDTH;
        return Optional.of(new Bounds(WorldLoopSizes.minSpawnChunk(widthChunks), WorldLoopSizes.maxSpawnChunk(widthChunks)));
    }

    private static Component modeName(SpawnPoint.Mode mode) {
        return Component.translatable(ChosenSpawn.CAPTION_KEY + KEY_SEPARATOR + mode.getSerializedName());
    }

    private ChosenSpawnView() {
    }
}

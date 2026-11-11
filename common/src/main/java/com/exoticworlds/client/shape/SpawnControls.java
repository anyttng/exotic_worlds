package com.exoticworlds.client.shape;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.WorldLoopSizes;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

public final class SpawnControls {
    private static final Component LABEL = Component.translatable("gui.exotic_worlds.toroidal_settings.spawn_point");
    private static final String POINT_KEY = "gui.exotic_worlds.toroidal_settings.spawn_point_hint.point";
    private static final Component SEARCH =
            Component.translatable("gui.exotic_worlds.toroidal_settings.spawn_point_hint.search");
    private static final Component MODE_LABEL = Component.translatable("gui.exotic_worlds.toroidal_settings.spawn_mode");
    private static final String MODE_KEY_PREFIX = "gui.exotic_worlds.toroidal_settings.spawn_mode.";
    private static final String MODE_HINT_SUFFIX = ".hint";
    private static final int FIELD_MAX_LENGTH = 8;
    private static final int ROW_SPACING = 4;
    private static final int AXIS_LETTER_WIDTH = 10;

    private final Entry x;
    private final Entry z;
    private SpawnPoint.Mode mode;
    private Direction.@Nullable Axis loopedAxis;
    private @Nullable Integer xChunkWidth;
    private @Nullable Integer zChunkWidth;

    private SpawnControls(SpawnPoint spawn, Direction.@Nullable Axis loopedAxis) {
        this.x = new Entry(Direction.Axis.X, spawn.xChunk());
        this.z = new Entry(Direction.Axis.Z, spawn.zChunk());
        this.mode = spawn.mode();
        this.loopedAxis = loopedAxis;
    }

    public static SpawnControls torus(SpawnPoint spawn) {
        return new SpawnControls(spawn, null);
    }

    public static SpawnControls cylinder(SpawnPoint spawn, Direction.Axis loopedAxis) {
        return new SpawnControls(spawn, loopedAxis);
    }

    public void loopedAxis(Direction.Axis axis) {
        this.loopedAxis = axis;
        this.render();
    }

    public void add(Font font, LinearLayout contents, Runnable onEdited) {
        contents.addChild(CycleButton.builder(SpawnControls::modeLabel, this.mode)
                .withValues(SpawnPoint.Mode.values())
                .withTooltip(mode -> Tooltip.create(modeHint(mode)))
                .create(0, 0, LoopSizeControls.FIELD_WIDTH, LoopSizeControls.FIELD_HEIGHT, MODE_LABEL,
                        (button, mode) -> this.chooseMode(mode, onEdited)));
        contents.addChild(new StringWidget(LoopSizeControls.FIELD_WIDTH, font.lineHeight, LABEL, font));
        LinearLayout row = contents.addChild(LinearLayout.horizontal().spacing(ROW_SPACING));
        int boxWidth = (LoopSizeControls.FIELD_WIDTH - 2 * AXIS_LETTER_WIDTH - 3 * ROW_SPACING) / 2;
        boolean custom = this.mode == SpawnPoint.Mode.CUSTOM;
        for (Entry entry : List.of(this.x, this.z)) {
            row.addChild(new StringWidget(AXIS_LETTER_WIDTH, LoopSizeControls.FIELD_HEIGHT, axisLetter(entry.axis),
                    font));
            row.addChild(entry.box(font, boxWidth, custom, onEdited));
        }
    }

    private void chooseMode(SpawnPoint.Mode chosen, Runnable onEdited) {
        this.mode = chosen;
        boolean custom = chosen == SpawnPoint.Mode.CUSTOM;
        this.x.editable(custom);
        this.z.editable(custom);
        onEdited.run();
    }

    public void update(@Nullable Integer xChunkWidth, @Nullable Integer zChunkWidth) {
        this.xChunkWidth = xChunkWidth;
        this.zChunkWidth = zChunkWidth;
        this.render();
    }

    private void render() {
        this.x.read(this.boundsWidth(Direction.Axis.X));
        this.z.read(this.boundsWidth(Direction.Axis.Z));
        Component hint = this.hint();
        this.x.show(hint);
        this.z.show(hint);
    }

    private @Nullable Integer boundsWidth(Direction.Axis axis) {
        if (this.loopedAxis != null && this.loopedAxis != axis) {
            return WorldLoopSizes.ENDLESS_AXIS_WIDTH;
        }

        return axis == Direction.Axis.X ? this.xChunkWidth : this.zChunkWidth;
    }

    public @Nullable SpawnPoint effective() {
        if (this.mode == SpawnPoint.Mode.DEFAULT) {
            return SpawnPoint.DEFAULT;
        }

        Integer xChunk = this.x.effective;
        Integer zChunk = this.z.effective;
        return xChunk == null || zChunk == null ? null : new SpawnPoint(xChunk, zChunk);
    }

    public boolean isComplete() {
        return this.effective() != null;
    }

    private Component hint() {
        HintText hint = new HintText();
        SpawnPoint spawn = this.effective();
        if (spawn != null && spawn.isCustom()) {
            hint.paragraph(Component.translatable(POINT_KEY, spawn.blockX(), spawn.blockZ()));
        }

        return hint.paragraph(SEARCH).build();
    }

    private static Component axisLetter(Direction.Axis axis) {
        return Component.literal(axis.getName().toUpperCase(Locale.ROOT));
    }

    private static Component modeLabel(SpawnPoint.Mode mode) {
        return Component.translatable(MODE_KEY_PREFIX + mode.getSerializedName());
    }

    private static Component modeHint(SpawnPoint.Mode mode) {
        return Component.translatable(MODE_KEY_PREFIX + mode.getSerializedName() + MODE_HINT_SUFFIX);
    }

    private static final class Entry {
        private final Direction.Axis axis;
        private String text;
        private @Nullable Integer effective;
        private @Nullable Component outOfBounds;
        private @Nullable DigitsEditBox edit;

        private Entry(Direction.Axis axis, int spawnChunk) {
            this.axis = axis;
            this.text = String.valueOf(spawnChunk);
            this.effective = spawnChunk;
        }

        private DigitsEditBox box(Font font, int width, boolean editable, Runnable onEdited) {
            DigitsEditBox box = DigitsEditBox.signed(font, width, LoopSizeControls.FIELD_HEIGHT, LABEL);
            box.setMaxLength(FIELD_MAX_LENGTH);
            box.setValue(this.text);
            box.setEditable(editable);
            box.setResponder(value -> {
                this.text = value;
                onEdited.run();
            });
            this.edit = box;
            return box;
        }

        private void editable(boolean editable) {
            if (this.edit != null) {
                this.edit.setEditable(editable);
            }
        }

        private void read(@Nullable Integer chunkWidth) {
            if (this.edit == null) {
                return;
            }

            Integer spawnChunk = this.edit.number();
            this.outOfBounds = spawnChunk == null || chunkWidth == null ? null : outOfBounds(spawnChunk, chunkWidth);
            this.effective = this.outOfBounds == null ? spawnChunk : null;
        }

        private static @Nullable Component outOfBounds(int spawnChunk, int chunkWidth) {
            int min = WorldLoopSizes.minSpawnChunk(chunkWidth);
            int max = WorldLoopSizes.maxSpawnChunk(chunkWidth);
            if (spawnChunk < min) {
                return Component.translatable(SizeField.TOO_SMALL_KEY, min);
            }

            return spawnChunk > max ? Component.translatable(SizeField.TOO_LARGE_KEY, max) : null;
        }

        private void show(Component hint) {
            if (this.edit != null) {
                Component bound = this.outOfBounds;
                this.edit.setTooltip(Tooltip.create(bound == null ? hint
                        : new HintText().paragraph(bound).paragraph(hint).build()));
            }
        }
    }
}

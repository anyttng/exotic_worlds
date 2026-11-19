package com.exoticworlds.client.shape;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.uber.nullaway.annotations.Initializer;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.shape.StemSettings;
import com.exoticworlds.shape.WorldLoopPresets;
import com.google.common.math.IntMath;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

public final class LoopSizeControls implements SizeControls {
    private static final String SIZE_X_LABEL_KEY = "gui.exotic_worlds.toroidal_settings.size_x";
    private static final String SIZE_Z_LABEL_KEY = "gui.exotic_worlds.toroidal_settings.size_z";
    private static final Component SKEW_LABEL = Component.translatable("gui.exotic_worlds.toroidal_settings.skew");
    private static final String SKEW_LAPS_KEY = "gui.exotic_worlds.toroidal_settings.skew_laps";
    private static final Component SKEW_HINT = Component.translatable("gui.exotic_worlds.toroidal_settings.skew_hint");
    private static final String NETHER_SCALE_KEY = "gui.exotic_worlds.toroidal_settings.nether_scale";
    private static final String NETHER_EFFECTIVE_KEY = "gui.exotic_worlds.toroidal_settings.nether_effective";
    private static final String NETHER_EFFECTIVE_XZ_KEY = "gui.exotic_worlds.toroidal_settings.nether_effective_xz";
    private static final Component NETHER_HINT = Component.translatable("gui.exotic_worlds.toroidal_settings.nether_hint");

    private static final String PRESET_KEY_PREFIX = "gui.exotic_worlds.toroidal_settings.preset.";
    private static final String STRUCTURES_SCARCE_KEY = "gui.exotic_worlds.toroidal_settings.consequence.structures_scarce";
    private static final String STRUCTURES_VILLAGES_KEY = "gui.exotic_worlds.toroidal_settings.consequence.structures_villages";
    private static final String STRUCTURES_COMMON_KEY = "gui.exotic_worlds.toroidal_settings.consequence.structures_common";
    private static final String STRUCTURES_ALL_KEY = "gui.exotic_worlds.toroidal_settings.consequence.structures_all";

    private static final int VILLAGE_GRID_CHUNKS = 34;
    private static final int MANSION_GRID_CHUNKS = 80;
    private static final int STRONGHOLD_RING_CHUNKS = 336;

    public static final int FIELD_WIDTH = 310;
    public static final int FIELD_HEIGHT = 20;

    private static final int PRESET_SPACING = 5;
    private static final String INCOMPLETE = "Done waits for isComplete";

    private static final int PRESET_WIDTH =
            (FIELD_WIDTH - PRESET_SPACING * (WorldLoopPresets.values().length - 1)) / WorldLoopPresets.values().length;

    private final Runnable onChange;

    private final SizeField size;
    private final @Nullable SizeField zSize;
    private final @Nullable SkewField skew;
    private final SpawnControls spawn;
    private final SizeField endSize;

    private int netherScale;
    private int wantedNetherScale;
    private int scalePickedForXSize;
    private int scalePickedForZSize;
    private int scalePickedForSkew;

    private final Map<WorldLoopPresets, Button> presetButtons = new EnumMap<>(WorldLoopPresets.class);
    private Button netherScaleButton;

    private LoopSizeControls(SizeField size, @Nullable SizeField zSize, @Nullable SkewField skew, SpawnControls spawn,
            int xChunkWidth, int zChunkWidth, int skewChunks, int netherScale, int endChunkWidth, Runnable onChange) {
        this.onChange = onChange;
        this.size = size;
        this.zSize = zSize;
        this.skew = skew;
        this.spawn = spawn;
        this.endSize = SizeField.end(endChunkWidth);
        this.netherScale = netherScale;
        this.wantedNetherScale = netherScale;
        this.scalePickedForXSize = xChunkWidth;
        this.scalePickedForZSize = zChunkWidth;
        this.scalePickedForSkew = skewChunks;
    }

    public static LoopSizeControls single(StemSettings current, Direction.Axis axis, Runnable onChange) {
        int chunkWidth = current.overworld().chunkWidth(axis);
        SpawnPoint spawn = current.generationOptions().get(SpawnPoint.OPTION);
        return new LoopSizeControls(SizeField.world(chunkWidth), null, null,
                SpawnControls.cylinder(spawn, axis),
                chunkWidth, chunkWidth, FlatShape.NO_SKEW, current.netherScale(), current.end().chunkWidth(axis),
                onChange);
    }

    public static LoopSizeControls perAxis(StemSettings current, Runnable onChange) {
        LoopSpans overworld = current.overworld();
        int xChunkWidth = overworld.chunkWidth(Direction.Axis.X);
        int zChunkWidth = overworld.chunkWidth(Direction.Axis.Z);
        int skewChunks = Math.floorMod(overworld.skewChunks(), xChunkWidth);
        SpawnPoint spawn = current.generationOptions().get(SpawnPoint.OPTION);
        return new LoopSizeControls(SizeField.world(SIZE_X_LABEL_KEY, xChunkWidth),
                SizeField.world(SIZE_Z_LABEL_KEY, zChunkWidth), new SkewField(skewChunks),
                SpawnControls.torus(spawn),
                xChunkWidth, zChunkWidth, skewChunks, current.netherScale(),
                current.end().chunkWidth(Direction.Axis.X), onChange);
    }

    @Override
    public void addPresets(LinearLayout contents) {
        LinearLayout presetRow = contents.addChild(LinearLayout.horizontal().spacing(PRESET_SPACING));
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            Button presetButton = presetRow.addChild(Button.builder(presetLabel(preset), button -> this.apply(preset))
                    .width(PRESET_WIDTH)
                    .build());
            presetButton.setTooltip(this.presetTooltip(preset));
            this.presetButtons.put(preset, presetButton);
        }
    }

    private Tooltip presetTooltip(WorldLoopPresets preset) {
        return Tooltip.create(this.size.effectiveLine(preset.chunkWidth()).copy()
                .append(CommonComponents.NEW_LINE)
                .append(netherScaleLine(preset.netherScale()))
                .append(CommonComponents.NEW_LINE)
                .append(this.endSize.effectiveLine(preset.endChunkWidth()))
                .append(CommonComponents.NEW_LINE)
                .append(Component.translatable(structureRoomKey(preset.chunkWidth()))));
    }

    @Override
    @Initializer
    public void addFields(Font font, LinearLayout contents) {
        this.size.add(font, contents, this::onSizeChanged);
        if (this.zSize != null) {
            this.zSize.add(font, contents, this::onSizeChanged);
        }

        if (this.skew != null) {
            this.skew.add(font, contents, this::onSizeChanged);
        }

        this.spawn.add(font, contents, this::onSpawnChanged);

        this.netherScaleButton = contents.addChild(Button.builder(Component.empty(), button -> this.cycleNetherScale())
                .width(FIELD_WIDTH)
                .build());

        this.endSize.add(font, contents, this::onEndSizeChanged);
    }

    @Override
    public void refresh() {
        this.onSizeChanged();
        this.onSpawnChanged();
        this.onEndSizeChanged();
    }

    public @Nullable Integer effectiveSize(Direction.Axis axis) {
        return axis == Direction.Axis.Z && this.zSize != null ? this.zSize.effective() : this.size.effective();
    }

    public @Nullable Integer effectiveSkew() {
        return this.skew == null ? Integer.valueOf(FlatShape.NO_SKEW) : this.skew.effective();
    }

    public int netherScale() {
        return this.netherScale;
    }

    public @Nullable Integer effectiveEndSize() {
        return this.endSize.effective();
    }

    public int completeSize(Direction.Axis axis) {
        return Objects.requireNonNull(this.effectiveSize(axis), INCOMPLETE);
    }

    public int completeSkew() {
        return Objects.requireNonNull(this.effectiveSkew(), INCOMPLETE);
    }

    public int completeEndSize() {
        return Objects.requireNonNull(this.effectiveEndSize(), INCOMPLETE);
    }

    public void loopedAxis(Direction.Axis axis) {
        this.spawn.loopedAxis(axis);
    }

    public SpawnPoint completeSpawn() {
        return Objects.requireNonNull(this.spawn.effective(), INCOMPLETE);
    }

    @Override
    public boolean isComplete() {
        return this.effectiveSize(Direction.Axis.X) != null && this.effectiveSize(Direction.Axis.Z) != null
                && this.effectiveSkew() != null && this.spawn.isComplete() && this.endSize.effective() != null;
    }

    private void apply(WorldLoopPresets preset) {
        this.netherScale = preset.netherScale();
        this.wantedNetherScale = preset.netherScale();
        this.size.setValue(preset.chunkWidth());
        if (this.zSize != null) {
            this.zSize.setValue(preset.chunkWidth());
        }

        if (this.skew != null) {
            this.skew.setValue(FlatShape.NO_SKEW);
        }

        this.endSize.setValue(preset.endChunkWidth());
    }

    private boolean matchesPreset(WorldLoopPresets preset) {
        Integer effectiveXSize = this.effectiveSize(Direction.Axis.X);
        Integer effectiveZSize = this.effectiveSize(Direction.Axis.Z);
        Integer effectiveSkew = this.effectiveSkew();
        Integer effectiveEndSize = this.endSize.effective();
        return effectiveXSize != null && effectiveXSize == preset.chunkWidth()
                && effectiveZSize != null && effectiveZSize == preset.chunkWidth()
                && effectiveSkew != null && effectiveSkew == FlatShape.NO_SKEW
                && this.netherScale == preset.netherScale()
                && effectiveEndSize != null && effectiveEndSize == preset.endChunkWidth();
    }

    private void changed() {
        for (Map.Entry<WorldLoopPresets, Button> entry : this.presetButtons.entrySet()) {
            entry.getValue().active = !this.matchesPreset(entry.getKey());
        }

        this.onChange.run();
    }

    private void onSizeChanged() {
        this.size.update();
        if (this.zSize != null) {
            this.zSize.update();
        }

        if (this.skew != null) {
            this.skew.update(this.effectiveSize(Direction.Axis.X));
        }

        this.updateSpawn();
        Integer xSizeChunks = this.effectiveSize(Direction.Axis.X);
        Integer zSizeChunks = this.effectiveSize(Direction.Axis.Z);
        Integer skewChunks = this.effectiveSkew();
        if (xSizeChunks == null || zSizeChunks == null || skewChunks == null) {
            this.netherScaleButton.active = false;
        } else {
            this.refreshNetherScale(xSizeChunks, zSizeChunks, skewChunks);
        }

        this.changed();
    }

    private void onSpawnChanged() {
        this.updateSpawn();
        this.changed();
    }

    private void updateSpawn() {
        this.spawn.update(this.effectiveSize(Direction.Axis.X), this.effectiveSize(Direction.Axis.Z));
    }

    private void onEndSizeChanged() {
        this.endSize.update();
        this.changed();
    }

    private void refreshNetherScale(int xSizeChunks, int zSizeChunks, int skewChunks) {
        List<Integer> allowed = NetherScales.allowedFor(xSizeChunks, zSizeChunks, skewChunks);
        boolean sizeChanged = xSizeChunks != this.scalePickedForXSize || zSizeChunks != this.scalePickedForZSize
                || skewChunks != this.scalePickedForSkew;
        this.netherScale = NetherScales.normalize(sizeChanged ? this.wantedNetherScale : this.netherScale, allowed);
        this.scalePickedForXSize = xSizeChunks;
        this.scalePickedForZSize = zSizeChunks;
        this.scalePickedForSkew = skewChunks;
        this.showNetherScale(xSizeChunks, zSizeChunks, allowed);
    }

    private void showNetherScale(int xSizeChunks, int zSizeChunks, List<Integer> allowed) {
        this.netherScaleButton.active = allowed.size() > 1;
        this.netherScaleButton.setMessage(netherScaleLine(this.netherScale));
        this.netherScaleButton.setTooltip(Tooltip.create(
                this.netherEffectiveLine(xSizeChunks, zSizeChunks).copy()
                        .append(CommonComponents.NEW_LINE)
                        .append(NETHER_HINT)));
    }

    private Component netherEffectiveLine(int xSizeChunks, int zSizeChunks) {
        int xNetherChunks = NetherScales.netherChunkWidth(xSizeChunks, this.netherScale);
        if (this.zSize == null) {
            return Component.translatable(NETHER_EFFECTIVE_KEY, xNetherChunks,
                    xNetherChunks * CoordinateConstants.CHUNK_WIDTH);
        }

        int zNetherChunks = NetherScales.netherChunkWidth(zSizeChunks, this.netherScale);
        return Component.translatable(NETHER_EFFECTIVE_XZ_KEY, xNetherChunks, zNetherChunks,
                xNetherChunks * CoordinateConstants.CHUNK_WIDTH, zNetherChunks * CoordinateConstants.CHUNK_WIDTH);
    }

    private void cycleNetherScale() {
        Integer effectiveXSize = this.effectiveSize(Direction.Axis.X);
        Integer effectiveZSize = this.effectiveSize(Direction.Axis.Z);
        Integer effectiveSkew = this.effectiveSkew();
        if (effectiveXSize == null || effectiveZSize == null || effectiveSkew == null) {
            return;
        }

        this.netherScale = NetherScales.next(this.netherScale, effectiveXSize, effectiveZSize, effectiveSkew);
        this.wantedNetherScale = this.netherScale;
        this.refreshNetherScale(effectiveXSize, effectiveZSize, effectiveSkew);
        this.changed();
    }

    private static Component presetLabel(WorldLoopPresets preset) {
        return Component.translatable(PRESET_KEY_PREFIX + preset.id());
    }

    private static String structureRoomKey(int sizeChunks) {
        if (sizeChunks < VILLAGE_GRID_CHUNKS) {
            return STRUCTURES_SCARCE_KEY;
        }

        if (sizeChunks < MANSION_GRID_CHUNKS) {
            return STRUCTURES_VILLAGES_KEY;
        }

        if (sizeChunks < STRONGHOLD_RING_CHUNKS) {
            return STRUCTURES_COMMON_KEY;
        }

        return STRUCTURES_ALL_KEY;
    }

    private static Component netherScaleLine(int scale) {
        return Component.translatable(NETHER_SCALE_KEY, scale);
    }

    private static final class SkewField {
        private String text;
        private @Nullable Integer effective;
        private DigitsEditBox edit;
        private StringWidget laps;

        private SkewField(int skewChunks) {
            this.text = String.valueOf(skewChunks);
        }

        @Initializer
        private void add(Font font, LinearLayout contents, Runnable onEdited) {
            this.edit = new DigitsEditBox(font, FIELD_WIDTH, FIELD_HEIGHT, SKEW_LABEL);
            this.edit.setMaxLength(SizeField.FIELD_MAX_LENGTH);
            this.edit.setValue(this.text);
            this.edit.setResponder(value -> {
                this.text = value;
                onEdited.run();
            });
            contents.addChild(CommonLayouts.labeledElement(font, this.edit, SKEW_LABEL));
            this.laps = contents.addChild(new StringWidget(FIELD_WIDTH, font.lineHeight, Component.empty(), font));
        }

        private void update(@Nullable Integer xChunkWidth) {
            Integer skewChunks = this.edit.number();
            if (skewChunks == null || xChunkWidth == null) {
                this.effective = null;
                this.laps.setMessage(Component.empty());
                this.edit.setTooltip(Tooltip.create(SKEW_HINT));
                return;
            }

            if (skewChunks >= xChunkWidth) {
                this.effective = null;
                this.laps.setMessage(Component.empty());
                this.edit.setTooltip(Tooltip.create(Component.translatable(SizeField.TOO_LARGE_KEY, xChunkWidth - 1).copy()
                        .append(CommonComponents.NEW_LINE)
                        .append(SKEW_HINT)));
                return;
            }

            this.effective = skewChunks;
            this.laps.setMessage(Component.translatable(SKEW_LAPS_KEY,
                    xChunkWidth / IntMath.gcd(xChunkWidth, skewChunks)));
            this.edit.setTooltip(Tooltip.create(SKEW_HINT));
        }

        private void setValue(int skewChunks) {
            this.edit.setValue(String.valueOf(skewChunks));
        }

        private @Nullable Integer effective() {
            return this.effective;
        }
    }
}

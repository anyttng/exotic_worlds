package com.exoticworlds.client.shape;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.core.WorldLoopSizes;
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

public final class LoopSizeControls {
    private static final Component HINT = Component.translatable("gui.exotic_worlds.toroidal_settings.hint");
    private static final String SIZE_LABEL_KEY = "gui.exotic_worlds.toroidal_settings.size";
    private static final String SIZE_X_LABEL_KEY = "gui.exotic_worlds.toroidal_settings.size_x";
    private static final String SIZE_Z_LABEL_KEY = "gui.exotic_worlds.toroidal_settings.size_z";
    private static final Component SKEW_LABEL = Component.translatable("gui.exotic_worlds.toroidal_settings.skew");
    private static final String SKEW_LAPS_KEY = "gui.exotic_worlds.toroidal_settings.skew_laps";
    private static final Component SKEW_HINT = Component.translatable("gui.exotic_worlds.toroidal_settings.skew_hint");
    private static final String EFFECTIVE_KEY = "gui.exotic_worlds.toroidal_settings.effective";
    private static final String TOO_SMALL_KEY = "gui.exotic_worlds.toroidal_settings.too_small";
    private static final String TOO_LARGE_KEY = "gui.exotic_worlds.toroidal_settings.too_large";
    private static final String NETHER_SCALE_KEY = "gui.exotic_worlds.toroidal_settings.nether_scale";
    private static final String NETHER_EFFECTIVE_KEY = "gui.exotic_worlds.toroidal_settings.nether_effective";
    private static final String NETHER_EFFECTIVE_XZ_KEY = "gui.exotic_worlds.toroidal_settings.nether_effective_xz";
    private static final Component NETHER_HINT = Component.translatable("gui.exotic_worlds.toroidal_settings.nether_hint");
    private static final String END_SIZE_LABEL_KEY = "gui.exotic_worlds.toroidal_settings.end_size";
    private static final String END_EFFECTIVE_KEY = "gui.exotic_worlds.toroidal_settings.end_effective";
    private static final Component END_HINT = Component.translatable("gui.exotic_worlds.toroidal_settings.end_hint");

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

    private static final int FIELD_MAX_LENGTH = 7;
    private static final int PRESET_SPACING = 5;

    private static final int PRESET_WIDTH =
            (FIELD_WIDTH - PRESET_SPACING * (WorldLoopPresets.values().length - 1)) / WorldLoopPresets.values().length;

    private final Runnable onChange;

    private final SizeField size;
    private final @Nullable SizeField zSize;
    private final @Nullable SkewField skew;
    private final SizeField endSize;

    private int netherScale;
    private int wantedNetherScale;
    private int scalePickedForXSize;
    private int scalePickedForZSize;
    private int scalePickedForSkew;

    private final Map<WorldLoopPresets, Button> presetButtons = new EnumMap<>(WorldLoopPresets.class);
    private Button netherScaleButton;

    private LoopSizeControls(SizeField size, @Nullable SizeField zSize, @Nullable SkewField skew, int xChunkWidth,
            int zChunkWidth, int skewChunks, int netherScale, int endChunkWidth, Runnable onChange) {
        this.onChange = onChange;
        this.size = size;
        this.zSize = zSize;
        this.skew = skew;
        this.endSize = new SizeField(END_SIZE_LABEL_KEY, END_EFFECTIVE_KEY, END_HINT,
                WorldLoopSizes.END_MIN_CHUNK_WIDTH, WorldLoopSizes::isEndInRange, endChunkWidth);
        this.netherScale = netherScale;
        this.wantedNetherScale = netherScale;
        this.scalePickedForXSize = xChunkWidth;
        this.scalePickedForZSize = zChunkWidth;
        this.scalePickedForSkew = skewChunks;
    }

    public static LoopSizeControls single(int chunkWidth, int netherScale, int endChunkWidth, Runnable onChange) {
        return new LoopSizeControls(worldSizeField(SIZE_LABEL_KEY, chunkWidth), null, null, chunkWidth, chunkWidth,
                FlatShape.NO_SKEW, netherScale, endChunkWidth, onChange);
    }

    public static LoopSizeControls perAxis(int xChunkWidth, int zChunkWidth, int skewChunks, int netherScale,
            int endChunkWidth, Runnable onChange) {
        return new LoopSizeControls(worldSizeField(SIZE_X_LABEL_KEY, xChunkWidth),
                worldSizeField(SIZE_Z_LABEL_KEY, zChunkWidth), new SkewField(skewChunks), xChunkWidth, zChunkWidth,
                skewChunks, netherScale, endChunkWidth, onChange);
    }

    public void addPresets(LinearLayout contents) {
        LinearLayout presetRow = contents.addChild(LinearLayout.horizontal().spacing(PRESET_SPACING));
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            Button presetButton = presetRow.addChild(Button.builder(presetLabel(preset), button -> this.apply(preset))
                    .width(PRESET_WIDTH)
                    .build());
            presetButton.setTooltip(Tooltip.create(
                    this.size.effectiveLine(preset.chunkWidth()).copy()
                            .append(CommonComponents.NEW_LINE)
                            .append(netherScaleLine(preset.netherScale()))
                            .append(CommonComponents.NEW_LINE)
                            .append(this.endSize.effectiveLine(preset.endChunkWidth()))
                            .append(CommonComponents.NEW_LINE)
                            .append(Component.translatable(structureRoomKey(preset.chunkWidth())))));
            this.presetButtons.put(preset, presetButton);
        }
    }

    public void addFields(Font font, LinearLayout contents) {
        this.size.add(font, contents, this::onSizeChanged);
        if (this.zSize != null) {
            this.zSize.add(font, contents, this::onSizeChanged);
        }

        if (this.skew != null) {
            this.skew.add(font, contents, this::onSizeChanged);
        }

        this.netherScaleButton = contents.addChild(Button.builder(Component.empty(), button -> this.cycleNetherScale())
                .width(FIELD_WIDTH)
                .build());

        this.endSize.add(font, contents, this::onEndSizeChanged);
    }

    public void refresh() {
        this.onSizeChanged();
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

    public boolean isComplete() {
        return this.effectiveSize(Direction.Axis.X) != null && this.effectiveSize(Direction.Axis.Z) != null
                && this.effectiveSkew() != null && this.endSize.effective() != null;
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

        if (this.effectiveSize(Direction.Axis.X) == null || this.effectiveSize(Direction.Axis.Z) == null
                || this.effectiveSkew() == null) {
            this.netherScaleButton.active = false;
        } else {
            this.refreshNetherScale();
        }

        this.changed();
    }

    private void onEndSizeChanged() {
        this.endSize.update();
        this.changed();
    }

    private void refreshNetherScale() {
        int xSizeChunks = this.effectiveSize(Direction.Axis.X);
        int zSizeChunks = this.effectiveSize(Direction.Axis.Z);
        int skewChunks = this.effectiveSkew();
        List<Integer> allowed = NetherScales.allowedFor(xSizeChunks, zSizeChunks, skewChunks);
        boolean sizeChanged = xSizeChunks != this.scalePickedForXSize || zSizeChunks != this.scalePickedForZSize
                || skewChunks != this.scalePickedForSkew;
        this.netherScale = NetherScales.normalize(sizeChanged ? this.wantedNetherScale : this.netherScale, allowed);
        this.scalePickedForXSize = xSizeChunks;
        this.scalePickedForZSize = zSizeChunks;
        this.scalePickedForSkew = skewChunks;
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
        this.refreshNetherScale();
        this.changed();
    }

    private static SizeField worldSizeField(String labelKey, int chunkWidth) {
        return new SizeField(labelKey, EFFECTIVE_KEY, HINT,
                WorldLoopSizes.MIN_CHUNK_WIDTH, WorldLoopSizes::isInRange, chunkWidth);
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

    private static final class SizeField {
        private final String labelKey;
        private final String effectiveKey;
        private final Component hint;
        private final int minChunks;
        private final IntPredicate inRange;

        private String text;
        private @Nullable Integer effective;
        private DigitsEditBox edit;

        private SizeField(String labelKey, String effectiveKey, Component hint, int minChunks, IntPredicate inRange,
                int chunkWidth) {
            this.labelKey = labelKey;
            this.effectiveKey = effectiveKey;
            this.hint = hint;
            this.minChunks = minChunks;
            this.inRange = inRange;
            this.text = String.valueOf(chunkWidth);
        }

        private void add(Font font, LinearLayout contents, Runnable onEdited) {
            this.edit = new DigitsEditBox(font, FIELD_WIDTH, FIELD_HEIGHT, this.label());
            this.edit.setMaxLength(FIELD_MAX_LENGTH);
            this.edit.setValue(this.text);
            this.edit.setResponder(value -> {
                this.text = value;
                onEdited.run();
            });
            contents.addChild(CommonLayouts.labeledElement(font, this.edit, this.label()));
        }

        private void update() {
            Integer sizeChunks = this.edit.number();
            this.effective = sizeChunks != null && this.inRange.test(sizeChunks) ? sizeChunks : null;
            this.edit.setTooltip(Tooltip.create(this.effective != null
                    ? this.effectiveLine(this.effective).copy().append(CommonComponents.NEW_LINE).append(this.hint)
                    : this.boundHint(sizeChunks)));
        }

        private void setValue(int chunkWidth) {
            this.edit.setValue(String.valueOf(chunkWidth));
        }

        private @Nullable Integer effective() {
            return this.effective;
        }

        private Component effectiveLine(int chunkWidth) {
            return Component.translatable(this.effectiveKey, chunkWidth, chunkWidth * CoordinateConstants.CHUNK_WIDTH);
        }

        private Component label() {
            return Component.translatable(this.labelKey, this.minChunks, WorldLoopSizes.MAX_CHUNK_WIDTH);
        }

        private Component boundHint(@Nullable Integer sizeChunks) {
            if (sizeChunks == null) {
                return this.hint;
            }

            Component bound = sizeChunks < this.minChunks
                    ? Component.translatable(TOO_SMALL_KEY, this.minChunks)
                    : Component.translatable(TOO_LARGE_KEY, WorldLoopSizes.MAX_CHUNK_WIDTH);
            return bound.copy().append(CommonComponents.NEW_LINE).append(this.hint);
        }
    }

    private static final class SkewField {
        private String text;
        private @Nullable Integer effective;
        private DigitsEditBox edit;
        private StringWidget laps;

        private SkewField(int skewChunks) {
            this.text = String.valueOf(skewChunks);
        }

        private void add(Font font, LinearLayout contents, Runnable onEdited) {
            this.edit = new DigitsEditBox(font, FIELD_WIDTH, FIELD_HEIGHT, SKEW_LABEL);
            this.edit.setMaxLength(FIELD_MAX_LENGTH);
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
                this.edit.setTooltip(Tooltip.create(Component.translatable(TOO_LARGE_KEY, xChunkWidth - 1).copy()
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

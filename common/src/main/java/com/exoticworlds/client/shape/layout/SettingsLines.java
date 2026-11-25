package com.exoticworlds.client.shape.layout;

import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.ascendiacore.gui.base.component.model.TooltipStyle;
import com.ascendiacore.gui.base.hint.Hint;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.WorldShape;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.DimensionShape;
import com.exoticworlds.shape.dimension.LoopSizeLines;
import com.exoticworlds.shape.layout.DimensionLayout;
import com.exoticworlds.shape.layout.ShapeKind;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

final class SettingsLines {
    private static final String PREFIX = LoopSizeLines.CAPTION_KEY;
    private static final String PRESET_PREFIX = PREFIX + ".preset.";
    private static final String STRUCTURES_PREFIX = PRESET_PREFIX + "structures.";
    private static final String SHAPE_SIZE_ERROR_KEY = PREFIX + ".shape.size_error";
    private static final String SCALE_PREFIX = "1:";
    private static final String ROOM_SCARCE = "scarce";
    private static final String ROOM_VILLAGES = "villages";
    private static final String ROOM_COMMON = "common";
    private static final String ROOM_ALL = "all";
    private static final int VILLAGE_GRID_CHUNKS = 34;
    private static final int MANSION_GRID_CHUNKS = 80;
    private static final int STRONGHOLD_RING_CHUNKS = 336;

    static final String SHAPE_KEY = PREFIX + ".shape";
    private static final String SHAPE_HINT_PREFIX = SHAPE_KEY + ".hint.";
    static final Component TITLE = Component.translatable(PREFIX);
    static final Component EXISTING_WORLD = Component.translatable(PREFIX + ".existing_world");
    static final Component VANILLA_DIMENSIONS = Component.translatable(PREFIX + ".dimensions.vanilla");
    static final Component MOD_DIMENSIONS = Component.translatable(PREFIX + ".dimensions.mod");
    static final Component UNDECIDED_DIMENSIONS = Component.translatable(PREFIX + ".dimensions.undecided");
    static final Component OVERWORLD = Component.translatable(PREFIX + ".dimension.overworld");
    static final Component NETHER = Component.translatable(PREFIX + ".dimension.nether");
    static final Component END = Component.translatable(PREFIX + ".dimension.end");
    static final Component NETHER_SCALE = Component.translatable(PREFIX + ".nether_scale");
    static final Component ADDITIONAL = Component.translatable(PREFIX + ".additional");

    static Component lockReason(DimensionLock lock) {
        return Component.translatable(PREFIX + ".locked." + lock.name().toLowerCase(Locale.ROOT));
    }

    static Component presetLabel(WorldLoopPresets preset) {
        return Component.translatable(PRESET_PREFIX + preset.getId());
    }

    static Component presetHint(TooltipStyle style, WorldLoopPresets preset) {
        int endWidthChunks = preset.getEndWidthChunks();
        Component sizes = LoopSizeLines.widthLine(preset.getWidthChunks()).copy()
                .append(CommonComponents.NEW_LINE)
                .append(Component.translatable(PRESET_PREFIX + "nether", preset.getNetherScale()))
                .append(CommonComponents.NEW_LINE)
                .append(Component.translatable(PRESET_PREFIX + "end", endWidthChunks,
                        endWidthChunks * CoordinateConstants.CHUNK_WIDTH_BLOCKS));
        return Hint.of(style)
                .derived(sizes)
                .note(Component.translatable(STRUCTURES_PREFIX + structureRoom(preset.getWidthChunks())))
                .build();
    }

    static Component shapeName(ShapeKind kind) {
        return Component.translatable(SHAPE_KEY + "." + kind.name().toLowerCase(Locale.ROOT));
    }

    static Component modeHint(ShapeKind kind) {
        return Component.translatable(SHAPE_HINT_PREFIX + kind.name().toLowerCase(Locale.ROOT));
    }

    static Component netherScaleName(int scale) {
        return Component.literal(SCALE_PREFIX + scale);
    }

    static Component shapeSizeError(int widthChunks, int minWidthChunks) {
        return Component.translatable(SHAPE_SIZE_ERROR_KEY, widthChunks, minWidthChunks,
                WorldLoopSizes.MAX_WIDTH_CHUNKS);
    }

    static Component summary(DimensionLayout layout, @Nullable FlatShape spans, boolean isDerivationKnown) {
        Identifier chosen = layout.shape();
        DimensionShape shape = chosen == null ? null : DeclaredShapes.of(chosen);
        if (chosen != null && (shape == null || shape.getParameters().isEmpty())) {
            return shape == null ? WorldShape.label(chosen) : shape.getName();
        }

        if (layout.kind() != ShapeKind.FROM_OVERWORLD) {
            return shapeSummary(spans);
        }

        Component fromOverworld = shapeName(ShapeKind.FROM_OVERWORLD);
        return isDerivationKnown ? fromOverworld.copy().append(LoopSizeLines.SEPARATOR).append(shapeSummary(spans)) : fromOverworld;
    }

    static Component shapeSummary(@Nullable FlatShape spans) {
        if (spans != null) {
            for (DimensionShape shape : DeclaredShapes.getAll()) {
                Optional<ShapeOptions> recognized = shape.optionsFor(spans);
                if (recognized.isPresent()) {
                    return shape.describe(recognized.get());
                }
            }
        }

        return shapeName(ShapeKind.NORMAL);
    }

    private static String structureRoom(int widthChunks) {
        if (widthChunks < VILLAGE_GRID_CHUNKS) {
            return ROOM_SCARCE;
        }

        if (widthChunks < MANSION_GRID_CHUNKS) {
            return ROOM_VILLAGES;
        }

        return widthChunks < STRONGHOLD_RING_CHUNKS ? ROOM_COMMON : ROOM_ALL;
    }

    private SettingsLines() {
    }
}

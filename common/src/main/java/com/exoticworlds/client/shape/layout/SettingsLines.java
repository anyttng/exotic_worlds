package com.exoticworlds.client.shape.layout;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.ascendiacore.gui.base.component.model.TooltipStyle;
import com.ascendiacore.gui.base.hint.Hint;
import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.WorldShape;
import com.exoticworlds.shape.layout.DimensionLayout;
import com.exoticworlds.shape.layout.ShapeKind;
import com.google.common.math.IntMath;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

final class SettingsLines {
    private static final String PREFIX = "gui.exotic_worlds.world_shape_settings";
    private static final String PRESET_PREFIX = PREFIX + ".preset.";
    private static final String STRUCTURES_PREFIX = PRESET_PREFIX + "structures.";
    private static final String SIZE_EFFECTIVE_KEY = PREFIX + ".size.effective";
    private static final String SHIFT_LAPS_KEY = PREFIX + ".shift.laps";
    private static final String SUMMARY_SHIFT_KEY = PREFIX + ".summary.shift";
    private static final String SHAPE_SIZE_ERROR_KEY = PREFIX + ".shape.size_error";
    private static final String SEPARATOR = ", ";
    private static final String SCALE_PREFIX = "1:";
    private static final String TIMES = " × ";
    private static final String ROOM_SCARCE = "scarce";
    private static final String ROOM_VILLAGES = "villages";
    private static final String ROOM_COMMON = "common";
    private static final String ROOM_ALL = "all";
    private static final int VILLAGE_GRID_CHUNKS = 34;
    private static final int MANSION_GRID_CHUNKS = 80;
    private static final int STRONGHOLD_RING_CHUNKS = 336;

    static final String SHAPE_KEY = PREFIX + ".shape";
    static final String AXIS_KEY = PREFIX + ".axis";
    static final String SPAWN_KEY = PREFIX + ".spawn";

    static final Component TITLE = Component.translatable(PREFIX);
    static final Component EXISTING_WORLD = Component.translatable(PREFIX + ".existing_world");
    static final Component VANILLA_DIMENSIONS = Component.translatable(PREFIX + ".dimensions.vanilla");
    static final Component MOD_DIMENSIONS = Component.translatable(PREFIX + ".dimensions.mod");
    static final Component UNDECIDED_DIMENSIONS = Component.translatable(PREFIX + ".dimensions.undecided");
    static final Component OVERWORLD = Component.translatable(PREFIX + ".dimension.overworld");
    static final Component NETHER = Component.translatable(PREFIX + ".dimension.nether");
    static final Component END = Component.translatable(PREFIX + ".dimension.end");
    static final Component SHIFT = Component.translatable(PREFIX + ".shift");
    static final Component SPAWN_POINT = Component.translatable(PREFIX + ".spawn.point");
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
        Component sizes = sizeEffective(preset.getWidthChunks()).copy()
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

    static Component sizeEffective(int widthChunks) {
        return Component.translatable(SIZE_EFFECTIVE_KEY, widthChunks, widthChunks * CoordinateConstants.CHUNK_WIDTH_BLOCKS);
    }

    static Component shiftLaps(int xWidthChunks, int shiftChunks) {
        return Component.translatable(SHIFT_LAPS_KEY, xWidthChunks / IntMath.gcd(xWidthChunks, shiftChunks));
    }

    static Component shapeName(ShapeKind kind) {
        return Component.translatable(SHAPE_KEY + "." + kind.name().toLowerCase(Locale.ROOT));
    }

    static Component spawnModeName(SpawnPoint.Mode mode) {
        return Component.translatable(SPAWN_KEY + "." + mode.getSerializedName());
    }

    static Component netherScaleName(int scale) {
        return Component.literal(SCALE_PREFIX + scale);
    }

    static Component axisName(Direction.Axis axis) {
        return Component.literal(axis.getName().toUpperCase(Locale.ROOT));
    }

    static Component shapeSizeError(int widthChunks, int minWidthChunks) {
        return Component.translatable(SHAPE_SIZE_ERROR_KEY, widthChunks, minWidthChunks,
                WorldLoopSizes.MAX_WIDTH_CHUNKS);
    }

    static Component summary(DimensionLayout layout, @Nullable FlatShape spans, boolean isDerivationKnown) {
        Identifier declared = layout.shape();
        if (declared != null) {
            return WorldShape.label(declared);
        }

        if (layout.kind() != ShapeKind.FROM_OVERWORLD) {
            return shapeSummary(spans);
        }

        Component fromOverworld = shapeName(ShapeKind.FROM_OVERWORLD);
        return isDerivationKnown ? fromOverworld.copy().append(SEPARATOR).append(shapeSummary(spans)) : fromOverworld;
    }

    static Component shapeSummary(@Nullable FlatShape spans) {
        ShapeKind kind = ShapeKind.of(spans);
        if (spans == null || kind == ShapeKind.NORMAL) {
            return shapeName(ShapeKind.NORMAL);
        }

        WorldLoopBounds bounds = spans.bounds();
        MutableComponent summary = shapeName(kind).copy();
        if (kind == ShapeKind.CYLINDER) {
            Direction.Axis axis = bounds.isLooped(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z;
            return summary.append(" ").append(axisName(axis)).append(SEPARATOR + bounds.getWidthChunks(axis));
        }

        summary.append(SEPARATOR + bounds.getWidthChunks(Direction.Axis.X) + TIMES + bounds.getWidthChunks(Direction.Axis.Z));
        int shiftChunks = Math.floorMod(spans.skewChunks(), bounds.getWidthChunks(Direction.Axis.X));
        return shiftChunks == 0 ? summary : summary.append(SEPARATOR).append(Component.translatable(SUMMARY_SHIFT_KEY, shiftChunks));
    }

    static Component sizeCaption(int minWidthChunks) {
        return sizeCaption(".size", minWidthChunks);
    }

    static Component sizeCaption(Direction.Axis axis, int minWidthChunks) {
        return sizeCaption(axis == Direction.Axis.X ? ".size_x" : ".size_z", minWidthChunks);
    }

    private static Component sizeCaption(String key, int minWidthChunks) {
        return Component.translatable(PREFIX + key, minWidthChunks, WorldLoopSizes.MAX_WIDTH_CHUNKS);
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

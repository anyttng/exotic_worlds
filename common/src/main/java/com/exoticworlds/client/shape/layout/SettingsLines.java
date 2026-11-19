package com.exoticworlds.client.shape.layout;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.layout.DimensionLayout;
import com.exoticworlds.shape.layout.ShapeKind;
import com.google.common.math.IntMath;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class SettingsLines {
    private static final String PREFIX = "gui.exotic_worlds.world_shape_settings";
    private static final String PRESET_PREFIX = PREFIX + ".preset.";
    private static final String STRUCTURES_PREFIX = PRESET_PREFIX + "structures.";
    private static final String SIZE_EFFECTIVE_KEY = PREFIX + ".size.effective";
    private static final String SHIFT_LAPS_KEY = PREFIX + ".shift.laps";
    private static final String SUMMARY_SHIFT_KEY = PREFIX + ".summary.shift";
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
    static final String GUARANTEED_LAND_KEY = PREFIX + ".guaranteed_land";
    static final String GUARANTEED_NETHER_COMPLEXES_KEY = PREFIX + ".guaranteed_nether_complexes";

    static final Component TITLE = Component.translatable(PREFIX);
    static final Component EXISTING_WORLD = Component.translatable(PREFIX + ".existing_world");
    static final Component VANILLA_DIMENSIONS = Component.translatable(PREFIX + ".dimensions.vanilla");
    static final Component MOD_DIMENSIONS = Component.translatable(PREFIX + ".dimensions.mod");
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
        return Component.translatable(PRESET_PREFIX + preset.id());
    }

    static Component presetHint(WorldLoopPresets preset) {
        int endChunkWidth = preset.endChunkWidth();
        return sizeEffective(preset.chunkWidth()).copy()
                .append(CommonComponents.NEW_LINE)
                .append(Component.translatable(PRESET_PREFIX + "nether", preset.netherScale()))
                .append(CommonComponents.NEW_LINE)
                .append(Component.translatable(PRESET_PREFIX + "end", endChunkWidth,
                        endChunkWidth * CoordinateConstants.CHUNK_WIDTH))
                .append(CommonComponents.NEW_LINE)
                .append(Component.translatable(STRUCTURES_PREFIX + structureRoom(preset.chunkWidth())));
    }

    static Component sizeEffective(int chunkWidth) {
        return Component.translatable(SIZE_EFFECTIVE_KEY, chunkWidth, chunkWidth * CoordinateConstants.CHUNK_WIDTH);
    }

    static Component shiftLaps(int xChunkWidth, int shiftChunks) {
        return Component.translatable(SHIFT_LAPS_KEY, xChunkWidth / IntMath.gcd(xChunkWidth, shiftChunks));
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

    static Component summary(DimensionLayout layout, @Nullable LoopSpans spans, boolean derivationKnown) {
        if (layout.kind() != ShapeKind.FROM_OVERWORLD) {
            return shapeSummary(spans);
        }

        Component fromOverworld = shapeName(ShapeKind.FROM_OVERWORLD);
        return derivationKnown ? fromOverworld.copy().append(SEPARATOR).append(shapeSummary(spans)) : fromOverworld;
    }

    static Component shapeSummary(@Nullable LoopSpans spans) {
        ShapeKind kind = ShapeKind.of(spans);
        if (spans == null || kind == ShapeKind.NORMAL) {
            return shapeName(ShapeKind.NORMAL);
        }

        MutableComponent summary = shapeName(kind).copy();
        if (kind == ShapeKind.CYLINDER) {
            Direction.Axis axis = spans.loops(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z;
            return summary.append(" ").append(axisName(axis)).append(SEPARATOR + spans.chunkWidth(axis));
        }

        summary.append(SEPARATOR + spans.chunkWidth(Direction.Axis.X) + TIMES + spans.chunkWidth(Direction.Axis.Z));
        int shift = Math.floorMod(spans.skewChunks(), spans.chunkWidth(Direction.Axis.X));
        return shift == 0 ? summary : summary.append(SEPARATOR).append(Component.translatable(SUMMARY_SHIFT_KEY, shift));
    }

    static Component sizeCaption(int minChunkWidth) {
        return sizeCaption(".size", minChunkWidth);
    }

    static Component sizeCaption(Direction.Axis axis, int minChunkWidth) {
        return sizeCaption(axis == Direction.Axis.X ? ".size_x" : ".size_z", minChunkWidth);
    }

    private static Component sizeCaption(String key, int minChunkWidth) {
        return Component.translatable(PREFIX + key, minChunkWidth, WorldLoopSizes.MAX_CHUNK_WIDTH);
    }

    private static String structureRoom(int chunkWidth) {
        if (chunkWidth < VILLAGE_GRID_CHUNKS) {
            return ROOM_SCARCE;
        }

        if (chunkWidth < MANSION_GRID_CHUNKS) {
            return ROOM_VILLAGES;
        }

        return chunkWidth < STRONGHOLD_RING_CHUNKS ? ROOM_COMMON : ROOM_ALL;
    }

    private SettingsLines() {
    }
}

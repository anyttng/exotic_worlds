package com.exoticworlds.shape.dimension;

import java.util.Locale;

import com.exoticworlds.core.world.CoordinateConstants;
import com.google.common.math.IntMath;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

public final class LoopSizeLines {
    public static final String CAPTION_KEY = "gui.exotic_worlds.world_shape_settings";

    private static final String WIDTH_KEY = CAPTION_KEY + ".size.effective";
    private static final String SHIFT_LAPS_KEY = CAPTION_KEY + ".shift.laps";
    private static final String SHIFT_SUMMARY_KEY = CAPTION_KEY + ".summary.shift";

    public static final String SEPARATOR = ", ";
    public static final String TIMES = " × ";

    public static Component widthLine(int widthChunks) {
        return Component.translatable(WIDTH_KEY, widthChunks, widthChunks * CoordinateConstants.CHUNK_WIDTH_BLOCKS);
    }

    public static Component shiftLaps(int xWidthChunks, int shiftChunks) {
        return Component.translatable(SHIFT_LAPS_KEY, xWidthChunks / IntMath.gcd(xWidthChunks, shiftChunks));
    }

    public static Component shiftSummary(int shiftChunks) {
        return Component.translatable(SHIFT_SUMMARY_KEY, shiftChunks);
    }

    public static Component axisName(Direction.Axis axis) {
        return Component.literal(axis.getName().toUpperCase(Locale.ROOT));
    }

    private LoopSizeLines() {
    }
}

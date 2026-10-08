package com.exoticworlds.compat.simpleatlas;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;

import net.minecraft.core.Direction;

public final class AtlasCopies {
    public record Offset(float x, float y) {
    }

    public static final List<Offset> BASE_ONLY = List.of(new Offset(0.0F, 0.0F));

    public static int lapTiles(@Nullable ToroidalShape shape, Direction.Axis axis, int blocksPerTile) {
        if (shape == null || !shape.loops(axis)) {
            return 0;
        }

        int width = shape.widthBlocks(axis);
        return width % blocksPerTile == 0 ? width / blocksPerTile : 0;
    }

    public static List<Offset> visible(float periodX, float periodZ, float originX, float originY, float width,
            float height, AtlasView area) {
        int[] lapsX = laps(periodX, originX, width, area.contentX(), area.contentWidth());
        int[] lapsZ = laps(periodZ, originY, height, area.contentY(), area.contentHeight());
        List<Offset> offsets = new ArrayList<>(lapsX.length * lapsZ.length);
        for (int lapZ : lapsZ) {
            for (int lapX : lapsX) {
                offsets.add(new Offset(lapX * periodX, lapZ * periodZ));
            }
        }

        return offsets;
    }

    public static double ontoBase(double coord, float period, float baseStart, float baseLength) {
        if (period <= 0.0F || coord >= baseStart && coord < baseStart + baseLength) {
            return coord;
        }

        double shifted = coord - Math.floor((coord - baseStart) / period) * period;
        return shifted < baseStart + baseLength ? shifted : coord;
    }

    private static int[] laps(float period, float start, float length, float areaStart, float areaLength) {
        if (period <= 0.0F) {
            return new int[] {0};
        }

        int first = (int) Math.floor((areaStart - start - length) / period) + 1;
        int last = (int) Math.ceil((areaStart + areaLength - start) / period) - 1;
        return AxisCopies.lapRange(first, last);
    }

    private AtlasCopies() {
    }
}

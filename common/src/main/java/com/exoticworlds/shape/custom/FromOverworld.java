package com.exoticworlds.shape.custom;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.shape.ShapeStems;

import net.minecraft.core.Direction;

public final class FromOverworld {

    public static @Nullable LoopSpans nether(@Nullable LoopSpans overworld, int netherScale) {
        return overworld == null || StemKind.of(overworld) == StemKind.NONE
                ? null
                : ShapeStems.netherOf(overworld, netherScale);
    }

    public static @Nullable LoopSpans end(@Nullable LoopSpans overworld) {
        if (overworld == null) {
            return null;
        }

        boolean x = overworld.loops(Direction.Axis.X);
        boolean z = overworld.loops(Direction.Axis.Z);
        if (x && z) {
            return LoopSpans.ofWidths(endWidth(overworld, Direction.Axis.X), endWidth(overworld, Direction.Axis.Z));
        }

        Direction.Axis axis = x ? Direction.Axis.X : Direction.Axis.Z;
        return x || z ? LoopSpans.ofWidth(axis, endWidth(overworld, axis)) : null;
    }

    public static @Nullable Integer netherScaleOf(LoopSpans overworld, @Nullable LoopSpans nether) {
        for (int scale : ShapeStems.netherScalesFor(overworld)) {
            if (Objects.equals(overworld.scaledDown(scale), nether)) {
                return scale;
            }
        }

        return null;
    }

    public static boolean isEnd(LoopSpans overworld, @Nullable LoopSpans end) {
        LoopSpans derived = end(overworld);
        return derived != null && Objects.equals(derived, end);
    }

    private static int endWidth(LoopSpans overworld, Direction.Axis axis) {
        return WorldLoopSizes.endChunkWidthFor(overworld.chunkWidth(axis));
    }

    private FromOverworld() {
    }
}

package com.exoticworlds.shape.layout;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.engine.gen.ShapedDimensions;

import net.minecraft.core.Direction;

public final class OverworldDerivation {

    public static @Nullable LoopSpans nether(@Nullable LoopSpans overworld, int netherScale) {
        return overworld == null || ShapeKind.of(overworld) == ShapeKind.NORMAL
                ? null
                : overworld.scaledDown(NetherScales.normalize(netherScale, netherScales(overworld)));
    }

    public static @Nullable LoopSpans end(@Nullable LoopSpans overworld) {
        if (overworld == null) {
            return null;
        }

        return switch (ShapeKind.of(overworld)) {
            case TORUS -> LoopSpans.ofWidths(endWidth(overworld, Direction.Axis.X), endWidth(overworld, Direction.Axis.Z));
            case CYLINDER -> LoopSpans.ofWidth(loopedAxis(overworld), endWidth(overworld, loopedAxis(overworld)));
            case NORMAL, FROM_OVERWORLD -> null;
        };
    }

    public static @Nullable LoopSpans modDimension(@Nullable LoopSpans overworld, double overworldScale,
            double coordinateScale) {
        if (overworld == null || ShapeKind.of(overworld) == ShapeKind.NORMAL) {
            return null;
        }

        FlatShape derived = ShapedDimensions.derivedShape(FlatShape.of(overworld), overworldScale, coordinateScale);
        return derived == null ? null : LayoutDimensions.spansOf(derived);
    }

    public static @Nullable Integer netherScaleOf(LoopSpans overworld, @Nullable LoopSpans nether) {
        for (int scale : netherScales(overworld)) {
            if (Objects.equals(overworld.scaledDown(scale), nether)) {
                return scale;
            }
        }

        return null;
    }

    public static List<Integer> netherScales(LoopSpans overworld) {
        int looped = overworld.chunkWidth(loopedAxis(overworld));
        int xChunkWidth = overworld.loops(Direction.Axis.X) ? overworld.chunkWidth(Direction.Axis.X) : looped;
        int zChunkWidth = overworld.loops(Direction.Axis.Z) ? overworld.chunkWidth(Direction.Axis.Z) : looped;
        return NetherScales.allowedFor(xChunkWidth, zChunkWidth, overworld.skewChunks());
    }

    private static Direction.Axis loopedAxis(LoopSpans spans) {
        return spans.loops(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z;
    }

    private static int endWidth(LoopSpans overworld, Direction.Axis axis) {
        return WorldLoopSizes.endChunkWidthFor(overworld.chunkWidth(axis));
    }

    private OverworldDerivation() {
    }
}

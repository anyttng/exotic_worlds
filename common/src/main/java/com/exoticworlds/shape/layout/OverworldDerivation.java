package com.exoticworlds.shape.layout;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.engine.gen.ShapedDimensions;

import net.minecraft.core.Direction;

public final class OverworldDerivation {

    public static @Nullable FlatShape nether(@Nullable FlatShape overworld, int netherScale) {
        return overworld == null || ShapeKind.of(overworld) == ShapeKind.NORMAL
                ? null
                : overworld.scaledDown(NetherScales.normalize(netherScale, netherScales(overworld)));
    }

    public static @Nullable FlatShape end(@Nullable FlatShape overworld) {
        if (overworld == null) {
            return null;
        }

        return switch (ShapeKind.of(overworld)) {
            case TORUS -> FlatShape.unskewed(WorldLoopBounds.ofWidths(
                    endWidth(overworld, Direction.Axis.X), endWidth(overworld, Direction.Axis.Z)));
            case CYLINDER -> FlatShape.unskewed(
                    WorldLoopBounds.ofWidth(loopedAxis(overworld), endWidth(overworld, loopedAxis(overworld))));
            case NORMAL, FROM_OVERWORLD -> null;
        };
    }

    public static @Nullable FlatShape modDimension(@Nullable FlatShape overworld, double overworldScale,
            double coordinateScale) {
        if (overworld == null || ShapeKind.of(overworld) == ShapeKind.NORMAL) {
            return null;
        }

        return ShapedDimensions.derivedShape(overworld, overworldScale, coordinateScale);
    }

    public static @Nullable Integer netherScaleOf(FlatShape overworld, @Nullable FlatShape nether) {
        for (int scale : netherScales(overworld)) {
            if (Objects.equals(overworld.scaledDown(scale), nether)) {
                return scale;
            }
        }

        return null;
    }

    public static List<Integer> netherScales(FlatShape overworld) {
        int looped = overworld.bounds().chunkWidth(loopedAxis(overworld));
        int xChunkWidth = overworld.bounds().loops(Direction.Axis.X) ? overworld.bounds().chunkWidth(Direction.Axis.X) : looped;
        int zChunkWidth = overworld.bounds().loops(Direction.Axis.Z) ? overworld.bounds().chunkWidth(Direction.Axis.Z) : looped;
        return NetherScales.allowedFor(xChunkWidth, zChunkWidth, overworld.skewChunks());
    }

    private static Direction.Axis loopedAxis(FlatShape spans) {
        return spans.bounds().loops(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z;
    }

    private static int endWidth(FlatShape overworld, Direction.Axis axis) {
        return WorldLoopSizes.endChunkWidthFor(overworld.bounds().chunkWidth(axis));
    }

    private OverworldDerivation() {
    }
}

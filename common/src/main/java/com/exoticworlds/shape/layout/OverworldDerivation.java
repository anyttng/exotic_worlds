package com.exoticworlds.shape.layout;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.NetherScales;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.engine.gen.ShapedDimensions;

import net.minecraft.core.Direction;

public final class OverworldDerivation {

    public static @Nullable FlatShape nether(@Nullable FlatShape overworld, int netherScale) {
        return overworld == null || !isLooping(overworld)
                ? null
                : overworld.scaledDown(NetherScales.normalize(netherScale, netherScales(overworld)));
    }

    public static @Nullable FlatShape end(@Nullable FlatShape overworld) {
        if (overworld == null) {
            return null;
        }

        boolean isXLooped = overworld.bounds().isLooped(Direction.Axis.X);
        boolean isZLooped = overworld.bounds().isLooped(Direction.Axis.Z);
        if (isXLooped && isZLooped) {
            return FlatShape.unskewed(WorldLoopBounds.ofWidths(
                    endWidthChunks(overworld, Direction.Axis.X), endWidthChunks(overworld, Direction.Axis.Z)));
        }

        return isXLooped || isZLooped
                ? FlatShape.unskewed(
                        WorldLoopBounds.ofWidth(loopedAxis(overworld), endWidthChunks(overworld, loopedAxis(overworld))))
                : null;
    }

    public static @Nullable FlatShape modDimension(@Nullable FlatShape overworld, double overworldScale,
            double coordinateScale) {
        if (overworld == null || !isLooping(overworld)) {
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
        int looped = overworld.bounds().getWidthChunks(loopedAxis(overworld));
        int xWidthChunks = overworld.bounds().isLooped(Direction.Axis.X) ? overworld.bounds().getWidthChunks(Direction.Axis.X) : looped;
        int zWidthChunks = overworld.bounds().isLooped(Direction.Axis.Z) ? overworld.bounds().getWidthChunks(Direction.Axis.Z) : looped;
        return NetherScales.allowedFor(xWidthChunks, zWidthChunks, overworld.skewChunks());
    }

    private static boolean isLooping(FlatShape spans) {
        return spans.bounds().isLooped(Direction.Axis.X) || spans.bounds().isLooped(Direction.Axis.Z);
    }

    private static Direction.Axis loopedAxis(FlatShape spans) {
        return spans.bounds().isLooped(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z;
    }

    private static int endWidthChunks(FlatShape overworld, Direction.Axis axis) {
        return WorldLoopSizes.endWidthChunksFor(overworld.bounds().getWidthChunks(axis));
    }

    private OverworldDerivation() {
    }
}

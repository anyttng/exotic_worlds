package com.exoticworlds.compat.simpleatlas;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.ExoticWorldsClientApi;
import com.exoticworlds.compat.AxisCopies;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import rubbertoe.simple_atlas.network.AtlasTilePayload;

public final class AtlasTileFold {
    public static final int MAP_PIXELS = 128;

    private static final int[] NO_EDGES = new int[0];

    public static double nearestToTile(AtlasTilePayload tile, Direction.Axis axis, double coord) {
        ToroidalShape shape = shapeOf(tile.dimension());
        return shape == null ? coord : shape.nearestCoord(axis, center(tile, axis), coord);
    }

    public static double foldOnTile(AtlasTilePayload tile, Direction.Axis axis, double coord) {
        ToroidalShape shape = shapeOf(tile.dimension());
        return shape == null ? coord : shape.foldCoord(axis, coord);
    }

    public static int[] edgePixels(AtlasTilePayload tile, int blocksPerTile, Direction.Axis axis) {
        ToroidalShape shape = shapeOf(tile.dimension());
        if (shape == null) {
            return NO_EDGES;
        }

        int blocksPerPixel = blocksPerTile / MAP_PIXELS;
        int min = center(tile, axis) - blocksPerTile / 2;
        int max = min + blocksPerTile;
        return Arrays.stream(AxisCopies.of(shape, axis).seams(min, max))
                .filter(edge -> edge >= min && edge < max)
                .map(edge -> (edge - min) / blocksPerPixel)
                .toArray();
    }

    private static int center(AtlasTilePayload tile, Direction.Axis axis) {
        return axis == Direction.Axis.X ? tile.centerX() : tile.centerZ();
    }

    public static @Nullable ToroidalShape shapeOf(String dimension) {
        Identifier id = Identifier.tryParse(dimension);
        return id == null
                ? null
                : ExoticWorldsClientApi.shapeOf(ResourceKey.create(Registries.DIMENSION, id)).orElse(null);
    }

    private AtlasTileFold() {
    }
}

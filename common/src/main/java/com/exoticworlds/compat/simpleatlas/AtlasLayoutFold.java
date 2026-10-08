package com.exoticworlds.compat.simpleatlas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;

import net.minecraft.core.Direction;

import rubbertoe.simple_atlas.network.AtlasTilePayload;

public final class AtlasLayoutFold {
    public static List<AtlasTilePayload> relaid(List<AtlasTilePayload> tiles, int blocksPerTile,
            Function<String, @Nullable ToroidalShape> shapes, MapCopies copies) {
        if (tiles.isEmpty()) {
            return tiles;
        }

        Map<String, AtlasTilePayload> firstOfDimension = new HashMap<>();
        for (AtlasTilePayload tile : tiles) {
            firstOfDimension.putIfAbsent(tile.dimension(), tile);
        }

        int[] cellsX = new int[tiles.size()];
        int[] cellsZ = new int[tiles.size()];
        AtlasTilePayload origin = tiles.getFirst();
        int originX = seated(origin, Direction.Axis.X, firstOfDimension, shapes, copies);
        int originZ = seated(origin, Direction.Axis.Z, firstOfDimension, shapes, copies);
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (int i = 0; i < tiles.size(); i++) {
            AtlasTilePayload tile = tiles.get(i);
            cellsX[i] = cell(seated(tile, Direction.Axis.X, firstOfDimension, shapes, copies) - originX,
                    blocksPerTile);
            cellsZ[i] = cell(seated(tile, Direction.Axis.Z, firstOfDimension, shapes, copies) - originZ,
                    blocksPerTile);
            minX = Math.min(minX, cellsX[i]);
            minZ = Math.min(minZ, cellsZ[i]);
        }

        List<AtlasTilePayload> relaid = new ArrayList<>(tiles.size());
        for (int i = 0; i < tiles.size(); i++) {
            AtlasTilePayload tile = tiles.get(i);
            relaid.add(new AtlasTilePayload(tile.mapId(), tile.centerX(), tile.centerZ(), cellsX[i] - minX,
                    cellsZ[i] - minZ, tile.dimension()));
        }

        return relaid;
    }

    private static int seated(AtlasTilePayload tile, Direction.Axis axis, Map<String, AtlasTilePayload> firstOfDimension,
            Function<String, @Nullable ToroidalShape> shapes, MapCopies copies) {
        int center = center(tile, axis);
        ToroidalShape shape = shapes.apply(tile.dimension());
        if (shape == null || copies == MapCopies.SINGLE) {
            return center;
        }

        return (int) shape.nearestCoord(axis, center(firstOfDimension.get(tile.dimension()), axis), center);
    }

    private static int cell(int delta, int blocksPerTile) {
        return (int) Math.round((double) delta / blocksPerTile);
    }

    private static int center(AtlasTilePayload tile, Direction.Axis axis) {
        return axis == Direction.Axis.X ? tile.centerX() : tile.centerZ();
    }

    private AtlasLayoutFold() {
    }
}

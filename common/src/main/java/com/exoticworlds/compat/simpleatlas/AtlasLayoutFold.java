package com.exoticworlds.compat.simpleatlas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;

import net.minecraft.core.BlockPos;

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
        BlockPos origin = seated(tiles.getFirst(), firstOfDimension, shapes, copies);
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (int i = 0; i < tiles.size(); i++) {
            BlockPos seated = seated(tiles.get(i), firstOfDimension, shapes, copies);
            cellsX[i] = cell(seated.getX() - origin.getX(), blocksPerTile);
            cellsZ[i] = cell(seated.getZ() - origin.getZ(), blocksPerTile);
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

    private static BlockPos seated(AtlasTilePayload tile, Map<String, AtlasTilePayload> firstOfDimension,
            Function<String, @Nullable ToroidalShape> shapes, MapCopies copies) {
        BlockPos center = center(tile);
        ToroidalShape shape = shapes.apply(tile.dimension());
        if (shape == null || copies == MapCopies.SINGLE) {
            return center;
        }

        return shape.nearestCopy(center(firstOfDimension.get(tile.dimension())), center);
    }

    private static int cell(int delta, int blocksPerTile) {
        return (int) Math.round((double) delta / blocksPerTile);
    }

    private static BlockPos center(AtlasTilePayload tile) {
        return new BlockPos(tile.centerX(), 0, tile.centerZ());
    }

    private AtlasLayoutFold() {
    }
}

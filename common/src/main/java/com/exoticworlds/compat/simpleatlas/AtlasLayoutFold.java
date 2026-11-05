package com.exoticworlds.compat.simpleatlas;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;

import net.minecraft.core.BlockPos;

import rubbertoe.simple_atlas.network.AtlasTilePayload;

public final class AtlasLayoutFold {
    public static List<AtlasTilePayload> relaid(List<AtlasTilePayload> tiles,
            ToIntFunction<AtlasTilePayload> blocksPerTile, Function<String, @Nullable ToroidalShape> shapes,
            MapCopies copies) {
        Map<String, List<Integer>> byDimension = new LinkedHashMap<>();
        for (int i = 0; i < tiles.size(); i++) {
            byDimension.computeIfAbsent(tiles.get(i).dimension(), dimension -> new ArrayList<>()).add(i);
        }

        List<AtlasTilePayload> relaid = null;
        for (Map.Entry<String, List<Integer>> dimension : byDimension.entrySet()) {
            ToroidalShape shape = shapes.apply(dimension.getKey());
            List<Integer> indices = dimension.getValue();
            int span = blocksPerTile.applyAsInt(tiles.get(indices.getFirst()));
            if (shape == null || span == 0) {
                continue;
            }

            if (relaid == null) {
                relaid = new ArrayList<>(tiles);
            }
            relayDimension(tiles, indices, shape, span, copies, relaid);
        }

        return relaid == null ? tiles : relaid;
    }

    private static void relayDimension(List<AtlasTilePayload> tiles, List<Integer> indices, ToroidalShape shape,
            int blocksPerTile, MapCopies copies, List<AtlasTilePayload> relaid) {
        int[] cellsX = new int[indices.size()];
        int[] cellsZ = new int[indices.size()];
        BlockPos origin = center(tiles.get(indices.getFirst()));
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (int i = 0; i < indices.size(); i++) {
            BlockPos center = center(tiles.get(indices.get(i)));
            BlockPos seated = copies == MapCopies.SINGLE ? center : shape.nearestCopy(origin, center);
            cellsX[i] = cell(seated.getX() - origin.getX(), blocksPerTile);
            cellsZ[i] = cell(seated.getZ() - origin.getZ(), blocksPerTile);
            minX = Math.min(minX, cellsX[i]);
            minZ = Math.min(minZ, cellsZ[i]);
        }

        for (int i = 0; i < indices.size(); i++) {
            AtlasTilePayload tile = tiles.get(indices.get(i));
            relaid.set(indices.get(i), new AtlasTilePayload(tile.mapId(), tile.centerX(), tile.centerZ(),
                    cellsX[i] - minX, cellsZ[i] - minZ, tile.dimension()));
        }
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

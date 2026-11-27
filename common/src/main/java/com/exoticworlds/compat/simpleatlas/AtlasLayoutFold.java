package com.exoticworlds.compat.simpleatlas;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.compat.map.MapCopies;

import net.minecraft.core.BlockPos;

import rubbertoe.simple_atlas.network.AtlasTilePayload;

public final class AtlasLayoutFold {
    public static List<AtlasTilePayload> relaid(List<AtlasTilePayload> tiles,
            ToIntFunction<AtlasTilePayload> blocksPerTile, Function<String, @Nullable WorldFold> shapes,
            MapCopies copies) {
        Map<String, List<Integer>> byDimension = new LinkedHashMap<>();
        for (int i = 0; i < tiles.size(); i++) {
            byDimension.computeIfAbsent(tiles.get(i).dimension(), dimension -> new ArrayList<>()).add(i);
        }

        List<AtlasTilePayload> relaid = null;
        for (Map.Entry<String, List<Integer>> dimension : byDimension.entrySet()) {
            WorldFold fold = shapes.apply(dimension.getKey());
            List<Integer> indices = dimension.getValue();
            int spanBlocks = blocksPerTile.applyAsInt(tiles.get(indices.getFirst()));
            if (fold == null || spanBlocks == 0) {
                continue;
            }

            if (relaid == null) {
                relaid = new ArrayList<>(tiles);
            }
            relayDimension(tiles, indices, fold, spanBlocks, copies, relaid);
        }

        return relaid == null ? tiles : relaid;
    }

    private static void relayDimension(List<AtlasTilePayload> tiles, List<Integer> indices, WorldFold fold,
            int blocksPerTile, MapCopies copies, List<AtlasTilePayload> relaid) {
        int[] cellXs = new int[indices.size()];
        int[] cellZs = new int[indices.size()];
        BlockPos origin = center(tiles.get(indices.getFirst()));
        int minCellX = Integer.MAX_VALUE;
        int minCellZ = Integer.MAX_VALUE;
        for (int i = 0; i < indices.size(); i++) {
            BlockPos center = center(tiles.get(indices.get(i)));
            BlockPos seated = copies == MapCopies.SINGLE ? center : fold.nearestCopy(origin, center);
            cellXs[i] = cell(seated.getX() - origin.getX(), blocksPerTile);
            cellZs[i] = cell(seated.getZ() - origin.getZ(), blocksPerTile);
            minCellX = Math.min(minCellX, cellXs[i]);
            minCellZ = Math.min(minCellZ, cellZs[i]);
        }

        for (int i = 0; i < indices.size(); i++) {
            AtlasTilePayload tile = tiles.get(indices.get(i));
            relaid.set(indices.get(i), new AtlasTilePayload(tile.mapId(), tile.centerX(), tile.centerZ(),
                    cellXs[i] - minCellX, cellZs[i] - minCellZ, tile.dimension()));
        }
    }

    private static int cell(int deltaBlocks, int blocksPerTile) {
        return (int) Math.round((double) deltaBlocks / blocksPerTile);
    }

    private static BlockPos center(AtlasTilePayload tile) {
        return new BlockPos(tile.centerX(), 0, tile.centerZ());
    }

    private AtlasLayoutFold() {
    }
}

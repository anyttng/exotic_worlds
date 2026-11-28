package com.exoticworlds.compat.mapatlases;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.map.AtlasTileCopies;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.utils.AtlasMap;

public final class MapGridKeyCopies {
    private static final int NEIGHBOR_REACH_TILES = 1;

    public static MapGridKey foldKey(Level level, MapGridKey key) {
        return moved(key, foldCenter(getFold(level), key.mapX, key.mapZ, key.gridWidth));
    }

    public static Set<MapGridKey> foldKeys(Level level, Set<MapGridKey> keys) {
        Set<MapGridKey> folded = new HashSet<>();
        for (MapGridKey key : keys) {
            folded.add(foldKey(level, key));
        }

        return folded;
    }

    public static List<MapGridKey> copies(Level level, MapGridKey key) {
        return copies(getFold(level), key);
    }

    public static List<MapGridKey> copies(@Nullable WorldFold fold, MapGridKey key) {
        List<AtlasTileCopies.TileCenter> centers = AtlasTileCopies.of(fold, key.mapX, key.mapZ, key.gridWidth);
        List<MapGridKey> keys = new ArrayList<>(centers.size());
        for (AtlasTileCopies.TileCenter center : centers) {
            keys.add(moved(key, center));
        }

        return keys;
    }

    public static @Nullable AtlasMap getShiftedUnder(Level level, MapCollection maps, MapGridKey key, double x,
            double z) {
        return getShiftedUnder(getFold(level), maps, key, x, z);
    }

    public static @Nullable AtlasMap getShiftedUnder(@Nullable WorldFold fold, MapCollection maps, MapGridKey key,
            double x, double z) {
        return shiftedUnder(fold, new AtlasTileCopies.TileCenter(key.mapX, key.mapZ), x, z, key.gridWidth,
                lookup(maps, key));
    }

    public static boolean isCoveredAcrossEdge(Level level, MapCollection maps, MapGridKey key) {
        return AtlasTileCopies.isCoveredByShifted(getFold(level), new AtlasTileCopies.TileCenter(key.mapX, key.mapZ),
                key.gridWidth, lookup(maps, key));
    }

    public static void addShiftedNeighbors(Level level, MapCollection maps, MapGridKey center,
            List<AtlasMap> inView) {
        for (AtlasTileCopies.Placed<AtlasMap> copy : AtlasTileCopies.shifted(getFold(level),
                new AtlasTileCopies.TileCenter(center.mapX, center.mapZ), NEIGHBOR_REACH_TILES, center.gridWidth,
                lookup(maps, center))) {
            if (!inView.contains(copy.map())) {
                inView.add(copy.map());
            }
        }
    }

    static AtlasTileCopies.TileCenter foldCenter(@Nullable WorldFold fold, int x, int z, int mapBlocks) {
        for (AtlasTileCopies.TileCenter center : AtlasTileCopies.of(fold, x, z, mapBlocks)) {
            if (isInside(fold, center)) {
                return center;
            }
        }

        return new AtlasTileCopies.TileCenter(x, z);
    }

    static <T> @Nullable T shiftedUnder(@Nullable WorldFold fold, AtlasTileCopies.TileCenter tile, double x, double z,
            int mapBlocks, AtlasTileCopies.TileLookup<T> lookup) {
        int half = mapBlocks / 2;
        for (AtlasTileCopies.Placed<T> copy : AtlasTileCopies.shifted(fold, tile, 0, mapBlocks, lookup)) {
            if (x >= copy.at().x() - half && x < copy.at().x() + half && z >= copy.at().z() - half
                    && z < copy.at().z() + half) {
                return copy.map();
            }
        }

        return null;
    }

    private static boolean isInside(@Nullable WorldFold fold, AtlasTileCopies.TileCenter center) {
        return isInside(fold, Direction.Axis.X, center.x()) && isInside(fold, Direction.Axis.Z, center.z());
    }

    private static boolean isInside(@Nullable WorldFold fold, Direction.Axis axis, int coord) {
        return fold == null || !fold.isLooped(axis)
                || (coord >= fold.getMinBlock(axis) && coord < fold.getMinBlock(axis) + fold.getWidthBlocks(axis));
    }

    private static MapGridKey moved(MapGridKey key, AtlasTileCopies.TileCenter center) {
        return key.offset((center.x() - key.mapX) / key.gridWidth, (center.z() - key.mapZ) / key.gridWidth);
    }

    private static AtlasTileCopies.TileLookup<AtlasMap> lookup(MapCollection maps, MapGridKey key) {
        return (x, z) -> maps.getMapAt(x, z, key.slice);
    }

    private static @Nullable WorldFold getFold(Level level) {
        return WorldLoopAttachments.getWrappedFold(level);
    }

    private MapGridKeyCopies() {
    }
}

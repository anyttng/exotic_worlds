package com.exoticworlds.compat.ultimatemapatlases;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ExoticWorldsApi;
import com.exoticworlds.api.v1.ToroidalShape;

import net.minecraft.world.level.Level;
import net.nanaky.ultimate_map_atlases.map_collection.IMapCollection;
import net.nanaky.ultimate_map_atlases.map_collection.MapKey;
import net.nanaky.ultimate_map_atlases.utils.MapDataHolder;

public final class AtlasCopyLookup {
    public static List<AtlasTileCopies.TileCenter> copies(Level level, IMapCollection maps, int centerX,
            int centerZ) {
        return AtlasTileCopies.of(shapeOf(level), centerX, centerZ, mapBlocks(maps));
    }

    public static boolean heldAcrossEdge(Level level, IMapCollection maps, MapKey key) {
        ToroidalShape shape = shapeOf(level);
        AtlasTileCopies.TileLookup<MapDataHolder> held = (x, z) -> maps.select(new MapKey(x, z, key.slice()));
        return AtlasTileCopies.acrossEdge(shape, key.mapX(), key.mapZ(), mapBlocks(maps), held) != null
                || AtlasTileCopies.coveredByShifted(shape, new AtlasTileCopies.TileCenter(key.mapX(), key.mapZ()),
                        mapBlocks(maps), held);
    }

    public static void addShiftedNeighbors(Level level, IMapCollection maps, MapKey active,
            List<MapDataHolder> nearby) {
        for (AtlasTileCopies.Placed<MapDataHolder> copy : AtlasTileCopies.shifted(shapeOf(level),
                new AtlasTileCopies.TileCenter(active.mapX(), active.mapZ()), 1, mapBlocks(maps),
                (x, z) -> maps.select(new MapKey(x, z, active.slice())))) {
            if (!nearby.contains(copy.map())) {
                nearby.add(copy.map());
            }
        }
    }

    private static @Nullable ToroidalShape shapeOf(Level level) {
        return ExoticWorldsApi.shapeOf(level).orElse(null);
    }

    private static int mapBlocks(IMapCollection maps) {
        return AtlasTileCopies.MAP_PIXELS << maps.getScale();
    }

    private AtlasCopyLookup() {
    }
}

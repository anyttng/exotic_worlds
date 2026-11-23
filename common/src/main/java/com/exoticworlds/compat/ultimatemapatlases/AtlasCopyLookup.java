package com.exoticworlds.compat.ultimatemapatlases;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.world.level.Level;
import net.nanaky.ultimate_map_atlases.map_collection.IMapCollection;
import net.nanaky.ultimate_map_atlases.map_collection.MapKey;
import net.nanaky.ultimate_map_atlases.utils.MapDataHolder;

public final class AtlasCopyLookup {
    public static List<AtlasTileCopies.TileCenter> copies(Level level, IMapCollection maps, int centerX,
            int centerZ) {
        return AtlasTileCopies.of(foldOf(level), centerX, centerZ, mapBlocks(maps));
    }

    public static boolean heldAcrossEdge(Level level, IMapCollection maps, MapKey key) {
        WorldFold fold = foldOf(level);
        AtlasTileCopies.TileLookup<MapDataHolder> held = (x, z) -> maps.select(new MapKey(x, z, key.slice()));
        return AtlasTileCopies.acrossEdge(fold, key.mapX(), key.mapZ(), mapBlocks(maps), held) != null
                || AtlasTileCopies.coveredByShifted(fold, new AtlasTileCopies.TileCenter(key.mapX(), key.mapZ()),
                        mapBlocks(maps), held);
    }

    public static void addShiftedNeighbors(Level level, IMapCollection maps, MapKey active,
            List<MapDataHolder> nearby) {
        for (AtlasTileCopies.Placed<MapDataHolder> copy : AtlasTileCopies.shifted(foldOf(level),
                new AtlasTileCopies.TileCenter(active.mapX(), active.mapZ()), 1, mapBlocks(maps),
                (x, z) -> maps.select(new MapKey(x, z, active.slice())))) {
            if (!nearby.contains(copy.map())) {
                nearby.add(copy.map());
            }
        }
    }

    private static @Nullable WorldFold foldOf(Level level) {
        return WorldLoopAttachments.wrappedFoldOf(level);
    }

    private static int mapBlocks(IMapCollection maps) {
        return AtlasTileCopies.MAP_PIXELS << maps.getScale();
    }

    private AtlasCopyLookup() {
    }
}

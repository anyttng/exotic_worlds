package com.exoticworlds.compat.ftbxaerocompat;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.ftbchunks.FtbChunksFold;

import net.minecraft.world.level.ChunkPos;

import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftblibrary.math.XZ;

import xaero.map.gui.MapTileSelection;

public final class FtbXaeroFold {
    public static ChunkPos playerChunkNearSelection(ChunkPos player, @Nullable MapTileSelection selection) {
        if (selection == null) {
            return player;
        }

        ChunkPos middle = new ChunkPos(Math.floorDiv(selection.getLeft() + selection.getRight(), 2),
                Math.floorDiv(selection.getTop() + selection.getBottom(), 2));
        return FtbChunksFold.nearestChunk(middle, player);
    }

    public static boolean regionHasStoredClaims(int regionX, int regionZ) {
        MapDimension dimension = MapDimension.getCurrent().orElse(null);
        if (dimension == null) {
            return false;
        }

        Map<XZ, MapRegion> regions = dimension.getRegions();
        for (XZ region : FtbChunksFold.canonicalRegions(regionX, regionZ)) {
            if (regions.get(region) != null) {
                return true;
            }
        }

        return false;
    }

    private FtbXaeroFold() {
    }
}

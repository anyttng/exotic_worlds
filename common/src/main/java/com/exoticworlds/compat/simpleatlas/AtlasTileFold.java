package com.exoticworlds.compat.simpleatlas;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.compat.map.ClientFolds;
import com.exoticworlds.compat.map.SeamLine;
import com.exoticworlds.compat.map.WorldCopies;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

import rubbertoe.simple_atlas.network.AtlasTilePayload;

public final class AtlasTileFold {
    public static final int MAP_PIXELS = 128;

    public static Vec3 nearestToTile(AtlasTilePayload tile, double x, double z) {
        return nearestToTile(getFold(tile.dimension()), tile, x, z);
    }

    static Vec3 nearestToTile(@Nullable WorldFold fold, AtlasTilePayload tile, double x, double z) {
        Vec3 point = new Vec3(x, 0.0, z);
        return fold == null ? point : fold.nearestCopy(new Vec3(tile.centerX(), 0.0, tile.centerZ()), point);
    }

    public static Vec3 foldOnTile(AtlasTilePayload tile, double x, double z) {
        return foldOnTile(getFold(tile.dimension()), x, z);
    }

    static Vec3 foldOnTile(@Nullable WorldFold fold, double x, double z) {
        return ClientFolds.fold(fold, new Vec3(x, 0.0, z));
    }

    public static List<SeamLine> seamLines(AtlasTilePayload tile, int blocksPerTile) {
        return seamLines(getFold(tile.dimension()), tile, blocksPerTile);
    }

    static List<SeamLine> seamLines(@Nullable WorldFold fold, AtlasTilePayload tile, int blocksPerTile) {
        int blocksPerPixel = blocksPerTile / MAP_PIXELS;
        int minX = tile.centerX() - blocksPerTile / 2;
        int minZ = tile.centerZ() - blocksPerTile / 2;
        List<SeamLine> lines = new ArrayList<>();
        for (WorldCopies.Edge edge : WorldCopies.seams(fold, minX, minZ, minX + blocksPerTile,
                minZ + blocksPerTile)) {
            lines.add(SeamLine.of(edge, -minX / blocksPerPixel, -minZ / blocksPerPixel, 1.0 / blocksPerPixel));
        }

        return lines;
    }

    public static @Nullable AtlasTilePayload firstOf(List<AtlasTilePayload> tiles, String dimension) {
        for (AtlasTilePayload tile : tiles) {
            if (tile.dimension().equals(dimension)) {
                return tile;
            }
        }

        return null;
    }

    public static int getBlocksPerTile(@Nullable Level level, @Nullable AtlasTilePayload tile) {
        MapItemSavedData data = tile == null || level == null ? null : level.getMapData(new MapId(tile.mapId()));
        return data == null ? 0 : MAP_PIXELS << data.scale;
    }

    public static @Nullable WorldFold getFold(String dimension) {
        Identifier id = Identifier.tryParse(dimension);
        return id == null ? null : ClientFolds.of(ResourceKey.create(Registries.DIMENSION, id));
    }

    private AtlasTileFold() {
    }
}

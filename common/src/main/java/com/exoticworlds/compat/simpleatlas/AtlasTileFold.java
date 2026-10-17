package com.exoticworlds.compat.simpleatlas;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.ClientShapes;
import com.exoticworlds.compat.SeamLine;
import com.exoticworlds.compat.WorldCopies;

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
        return nearestToTile(shapeOf(tile.dimension()), tile, x, z);
    }

    static Vec3 nearestToTile(@Nullable ToroidalShape shape, AtlasTilePayload tile, double x, double z) {
        Vec3 point = new Vec3(x, 0.0, z);
        return shape == null ? point : shape.nearestCopy(new Vec3(tile.centerX(), 0.0, tile.centerZ()), point);
    }

    public static Vec3 foldOnTile(AtlasTilePayload tile, double x, double z) {
        return foldOnTile(shapeOf(tile.dimension()), x, z);
    }

    static Vec3 foldOnTile(@Nullable ToroidalShape shape, double x, double z) {
        return ClientShapes.fold(shape, new Vec3(x, 0.0, z));
    }

    public static List<SeamLine> seamLines(AtlasTilePayload tile, int blocksPerTile) {
        return seamLines(shapeOf(tile.dimension()), tile, blocksPerTile);
    }

    static List<SeamLine> seamLines(@Nullable ToroidalShape shape, AtlasTilePayload tile, int blocksPerTile) {
        int blocksPerPixel = blocksPerTile / MAP_PIXELS;
        int minX = tile.centerX() - blocksPerTile / 2;
        int minZ = tile.centerZ() - blocksPerTile / 2;
        List<SeamLine> lines = new ArrayList<>();
        for (WorldCopies.Edge edge : WorldCopies.seams(shape, minX, minZ, minX + blocksPerTile,
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

    public static int blocksPerTile(@Nullable Level level, @Nullable AtlasTilePayload tile) {
        MapItemSavedData data = tile == null || level == null ? null : level.getMapData(new MapId(tile.mapId()));
        return data == null ? 0 : MAP_PIXELS << data.scale;
    }

    public static @Nullable ToroidalShape shapeOf(String dimension) {
        Identifier id = Identifier.tryParse(dimension);
        return id == null ? null : ClientShapes.of(ResourceKey.create(Registries.DIMENSION, id));
    }

    private AtlasTileFold() {
    }
}

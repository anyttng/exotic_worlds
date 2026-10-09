package com.exoticworlds.compat.simpleatlas;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.ExoticWorldsClientApi;
import com.exoticworlds.compat.WorldCopies;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;

import rubbertoe.simple_atlas.network.AtlasTilePayload;

public final class AtlasTileFold {
    public static final int MAP_PIXELS = 128;

    public record SeamLine(boolean vertical, int at, int from, int to) {
    }

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
        Vec3 point = new Vec3(x, 0.0, z);
        return shape == null ? point : shape.fold(point);
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
            boolean vertical = edge.fromX() == edge.toX();
            lines.add(vertical
                    ? new SeamLine(true, (edge.fromX() - minX) / blocksPerPixel, (edge.fromZ() - minZ) / blocksPerPixel,
                            Math.ceilDiv(edge.toZ() - minZ, blocksPerPixel))
                    : new SeamLine(false, (edge.fromZ() - minZ) / blocksPerPixel,
                            (edge.fromX() - minX) / blocksPerPixel, Math.ceilDiv(edge.toX() - minX, blocksPerPixel)));
        }

        return lines;
    }

    public static @Nullable ToroidalShape shapeOf(String dimension) {
        Identifier id = Identifier.tryParse(dimension);
        return id == null
                ? null
                : ExoticWorldsClientApi.shapeOf(ResourceKey.create(Registries.DIMENSION, id)).orElse(null);
    }

    private AtlasTileFold() {
    }
}

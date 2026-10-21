package com.exoticworlds.compat.ultimatemapatlases;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class AtlasTileCopies {
    public static final int MAP_PIXELS = 128;

    public record TileCenter(int x, int z) {
    }

    public interface TileLookup<T> {
        @Nullable T at(int centerX, int centerZ);
    }

    public static <T> @Nullable T first(@Nullable ToroidalShape shape, int x, int z, int mapBlocks,
            TileLookup<T> lookup) {
        return firstOf(of(shape, x, z, mapBlocks), lookup);
    }

    public static <T> @Nullable T acrossEdge(@Nullable ToroidalShape shape, int x, int z, int mapBlocks,
            TileLookup<T> lookup) {
        List<TileCenter> centers = of(shape, x, z, mapBlocks);
        return firstOf(centers.subList(1, centers.size()), lookup);
    }

    public static List<TileCenter> of(@Nullable ToroidalShape shape, int x, int z, int mapBlocks) {
        TileCenter request = new TileCenter(x, z);
        if (shape == null) {
            return List.of(request);
        }

        int half = mapBlocks / 2;
        AABB near = new AABB(lower(shape, Direction.Axis.X, x) - half, 0.0, lower(shape, Direction.Axis.Z, z) - half,
                upper(shape, Direction.Axis.X, x) + half, 1.0, upper(shape, Direction.Axis.Z, z) + half);
        List<TileCenter> centers = new ArrayList<>();
        centers.add(request);
        for (ToroidalShape.Oriented<BlockPos> copy : shape.copiesInside(near, new BlockPos(x, 0, z))) {
            if (!copy.orientation().isIdentity()) {
                throw new IllegalStateException("A map draws its copies unturned, got " + copy.orientation());
            }

            int moveX = copy.value().getX() - x;
            int moveZ = copy.value().getZ() - z;
            if ((moveX != 0 || moveZ != 0) && moveX % mapBlocks == 0 && moveZ % mapBlocks == 0) {
                centers.add(new TileCenter(copy.value().getX(), copy.value().getZ()));
            }
        }

        return centers;
    }

    private static <T> @Nullable T firstOf(List<TileCenter> centers, TileLookup<T> lookup) {
        for (TileCenter center : centers) {
            T found = lookup.at(center.x(), center.z());
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private static int lower(ToroidalShape shape, Direction.Axis axis, int coord) {
        return shape.loops(axis) ? shape.minBlock(axis) : coord;
    }

    private static int upper(ToroidalShape shape, Direction.Axis axis, int coord) {
        return shape.loops(axis) ? shape.minBlock(axis) + shape.widthBlocks(axis) : coord;
    }

    private AtlasTileCopies() {
    }
}

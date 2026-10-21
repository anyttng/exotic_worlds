package com.exoticworlds.compat.ultimatemapatlases;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.WorldCopies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class AtlasTileCopies {
    public static final int MAP_PIXELS = 128;

    private static final int GRID_OFFSET_BLOCKS = 64;

    public record TileCenter(int x, int z) {
    }

    public record Placed<T>(T map, TileCenter at) {
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
            if ((moveX != 0 || moveZ != 0) && wholeMaps(moveX, moveZ, mapBlocks)) {
                centers.add(new TileCenter(copy.value().getX(), copy.value().getZ()));
            }
        }

        return centers;
    }

    public static <T> List<Placed<T>> shifted(@Nullable ToroidalShape shape, TileCenter center, int reachTiles,
            int mapBlocks, TileLookup<T> lookup) {
        if (shape == null) {
            return List.of();
        }

        int reach = reachTiles * mapBlocks + mapBlocks / 2;
        int minX = center.x() - reach;
        int minZ = center.z() - reach;
        int maxX = center.x() + reach;
        int maxZ = center.z() + reach;
        List<Placed<T>> placed = new ArrayList<>();
        for (WorldCopies.Copy move : WorldCopies.meeting(shape, minX - mapBlocks, minZ - mapBlocks, maxX + mapBlocks,
                maxZ + mapBlocks)) {
            if (wholeMaps(move.dx(), move.dz(), mapBlocks)) {
                continue;
            }

            int lastX = lastCenter(shape, Direction.Axis.X, maxX - move.dx(), mapBlocks);
            int lastZ = lastCenter(shape, Direction.Axis.Z, maxZ - move.dz(), mapBlocks);
            for (int x = firstCenter(shape, Direction.Axis.X, minX - move.dx(), mapBlocks); x <= lastX; x += mapBlocks) {
                for (int z = firstCenter(shape, Direction.Axis.Z, minZ - move.dz(), mapBlocks); z <= lastZ;
                        z += mapBlocks) {
                    T map = lookup.at(x, z);
                    if (map != null) {
                        placed.add(new Placed<>(map, new TileCenter(x + move.dx(), z + move.dz())));
                    }
                }
            }
        }

        return placed;
    }

    public static <T> boolean coveredByShifted(@Nullable ToroidalShape shape, TileCenter tile, int mapBlocks,
            TileLookup<T> lookup) {
        List<Placed<T>> copies = shifted(shape, tile, 0, mapBlocks, lookup);
        if (copies.isEmpty()) {
            return false;
        }

        int half = mapBlocks / 2;
        List<Integer> xs = edges(tile.x(), half, copies.stream().map(copy -> copy.at().x()).toList());
        List<Integer> zs = edges(tile.z(), half, copies.stream().map(copy -> copy.at().z()).toList());
        for (int i = 1; i < xs.size(); i++) {
            for (int j = 1; j < zs.size(); j++) {
                double x = (xs.get(i - 1) + xs.get(i)) / 2.0;
                double z = (zs.get(j - 1) + zs.get(j)) / 2.0;
                if (copies.stream().noneMatch(copy -> Math.abs(x - copy.at().x()) < half
                        && Math.abs(z - copy.at().z()) < half)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static List<Integer> edges(int center, int half, List<Integer> copyCenters) {
        TreeSet<Integer> edges = new TreeSet<>(List.of(center - half, center + half));
        for (int copy : copyCenters) {
            for (int edge : new int[] {copy - half, copy + half}) {
                if (edge > center - half && edge < center + half) {
                    edges.add(edge);
                }
            }
        }

        return List.copyOf(edges);
    }

    private static boolean wholeMaps(int moveX, int moveZ, int mapBlocks) {
        return moveX % mapBlocks == 0 && moveZ % mapBlocks == 0;
    }

    private static int firstCenter(ToroidalShape shape, Direction.Axis axis, int viewMin, int mapBlocks) {
        int past = Math.max(viewMin, lower(shape, axis, viewMin)) - mapBlocks / 2;
        int center = gridCenter(past, mapBlocks);
        return center > past ? center : center + mapBlocks;
    }

    private static int lastCenter(ToroidalShape shape, Direction.Axis axis, int viewMax, int mapBlocks) {
        int before = Math.min(viewMax, upper(shape, axis, viewMax)) + mapBlocks / 2;
        int center = gridCenter(before, mapBlocks);
        return center < before ? center : center - mapBlocks;
    }

    private static int gridCenter(int coord, int mapBlocks) {
        return Math.floorDiv(coord + GRID_OFFSET_BLOCKS, mapBlocks) * mapBlocks + mapBlocks / 2 - GRID_OFFSET_BLOCKS;
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

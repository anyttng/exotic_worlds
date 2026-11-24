package com.exoticworlds.core.fold;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RectangleCopies {
    public static List<DeckTransformation> meeting(WorldFold fold, AABB box) {
        int minX = Mth.floor(box.minX);
        int maxX = Mth.ceil(box.maxX) - 1;
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.ceil(box.maxZ) - 1;
        if (maxX < minX || maxZ < minZ) {
            return List.of();
        }

        return fold.copiesTouching(new BoundingBox(minX, 0, minZ, maxX, 0, maxZ));
    }

    public static <R> List<R> inside(WorldFold fold, AABB box, Vec3 pos,
            BiFunction<Vec3, FoldOrientation, R> oriented) {
        List<DeckTransformation> copies = meeting(fold, box);
        WorldFold.Folded<Vec3> folded = fold.foldOriented(pos);
        List<R> inside = new ArrayList<>(copies.size());
        for (DeckTransformation copy : copies) {
            Vec3 moved = copy.apply(folded.value());
            if (isHolding(box, moved.x, moved.z)) {
                inside.add(oriented.apply(
                        moved.equals(pos) ? pos : moved, folded.orientation().compose(copy.getOrientation())));
            }
        }

        return inside;
    }

    public static <R> List<R> inside(
            WorldFold fold, AABB box, BlockPos pos, BiFunction<BlockPos, FoldOrientation, R> oriented) {
        List<DeckTransformation> copies = meeting(fold, box);
        WorldFold.Folded<BlockPos> folded = fold.foldOriented(pos);
        List<R> inside = new ArrayList<>(copies.size());
        for (DeckTransformation copy : copies) {
            BlockPos moved = copy.apply(folded.value());
            if (isHolding(box, moved.getX(), moved.getZ())) {
                inside.add(oriented.apply(
                        moved.equals(pos) ? pos : moved, folded.orientation().compose(copy.getOrientation())));
            }
        }

        return inside;
    }

    private static boolean isHolding(AABB box, double x, double z) {
        return x >= box.minX && x < box.maxX && z >= box.minZ && z < box.maxZ;
    }

    private RectangleCopies() {
    }
}

package com.exoticworlds.engine.fold;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.WorldFold;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

public final class NearestCopy {
    public static Vec3 toward(@Nullable WorldFold fold, Vec3 anchor, Vec3 target) {
        return fold == null ? target : fold.nearestCopy(anchor, target);
    }

    public static BlockPos toward(@Nullable WorldFold fold, BlockPos anchor, BlockPos target) {
        return fold == null ? target : fold.nearestCopy(anchor, target);
    }

    public static Vec3i toward(@Nullable WorldFold fold, BlockPos anchor, Vec3i target) {
        return fold == null ? target : fold.nearestCopy(anchor, asBlockPos(target));
    }

    public static Position toward(@Nullable WorldFold fold, Vec3 anchor, Position target) {
        return fold == null ? target : fold.nearestCopy(anchor, asVec3(target));
    }

    public static Position towardCenterOf(@Nullable WorldFold fold, BlockPos block, Position target) {
        return fold == null ? target : fold.nearestCopy(Vec3.atCenterOf(block), asVec3(target));
    }

    public static BlockPos toward(@Nullable WorldFold fold, BoundingBox box, BlockPos target) {
        return fold == null ? target : fold.nearestCopy(box.getCenter(), target);
    }

    public static ChunkPos toward(@Nullable WorldFold fold, ChunkPos anchor, ChunkPos target) {
        return fold == null ? target : fold.nearestCopy(anchor, target);
    }

    public static double toward(@Nullable WorldFold fold, Direction.Axis axis, Vec3 anchor, Vec3 target) {
        return fold == null ? target.get(axis) : fold.nearestCopy(anchor, target).get(axis);
    }

    // mc/1.21: calls the transformation forms, unused on main.
    public static DeckTransformation transformationToward(@Nullable WorldFold fold, Vec3 ref, Vec3 target) {
        return fold == null ? DeckTransformation.IDENTITY : fold.nearestCopyTransformation(ref, target);
    }

    public static DeckTransformation transformationToward(@Nullable WorldFold fold, BlockPos ref, BlockPos target) {
        return fold == null ? DeckTransformation.IDENTITY : fold.nearestCopyTransformation(ref, target);
    }

    private static BlockPos asBlockPos(Vec3i pos) {
        return pos instanceof BlockPos blockPos ? blockPos : new BlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    private static Vec3 asVec3(Position pos) {
        return pos instanceof Vec3 vec ? vec : new Vec3(pos.x(), pos.y(), pos.z());
    }

    private NearestCopy() {
    }
}

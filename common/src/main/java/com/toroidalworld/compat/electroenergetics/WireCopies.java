package com.toroidalworld.compat.electroenergetics;

import com.toroidalworld.core.DeckTransformation;
import com.toroidalworld.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class WireCopies {
    public static final int FIRST_END = 0;
    public static final int SECOND_END = 1;

    public record Span(Vec3 pos1, Vec3 pos2) {
    }

    public record BlockSpan(BlockPos pos1, BlockPos pos2) {
    }

    public static boolean parted(WorldFold fold, Vec3 pos1, Vec3 pos2) {
        return !fold.nearestCopy(pos1, pos2).equals(pos2);
    }

    public static boolean parted(WorldFold fold, BlockPos pos1, BlockPos pos2) {
        return !fold.nearestCopy(pos1, pos2).equals(pos2);
    }

    public static Span from(WorldFold fold, Vec3 pos1, Vec3 pos2, int end) {
        return end == SECOND_END
                ? new Span(fold.nearestCopy(pos2, pos1), pos2)
                : new Span(pos1, fold.nearestCopy(pos1, pos2));
    }

    public static BlockSpan from(WorldFold fold, BlockPos pos1, BlockPos pos2, int end) {
        return end == SECOND_END
                ? new BlockSpan(fold.nearestCopy(pos2, pos1), pos2)
                : new BlockSpan(pos1, fold.nearestCopy(pos1, pos2));
    }

    public static Span around(WorldFold fold, Vec3 pos1, Vec3 pos2, float point, Vec3 viewer) {
        Span first = from(fold, pos1, pos2, FIRST_END);
        DeckTransformation move = fold.nearestCopyTransformation(viewer, first.pos1().lerp(first.pos2(), point));
        return new Span(move.apply(first.pos1()), move.apply(first.pos2()));
    }

    private WireCopies() {
    }
}

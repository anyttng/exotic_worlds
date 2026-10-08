package com.exoticworlds.compat.electroenergetics;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedCopies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WireSpan {
    public static Vec3 seat(Level level, Vec3 anchor, Vec3 end) {
        return WorldLoopAttachments.transformerOf(level).nearestCopy(anchor, end);
    }

    public static @Nullable Vec3 seatIfPresent(Level level, @Nullable Vec3 anchor, @Nullable Vec3 end) {
        return anchor == null || end == null ? end : seat(level, anchor, end);
    }

    public static @Nullable Vec3 seatOnClientIfPresent(Level level, @Nullable Vec3 anchor, @Nullable Vec3 end) {
        return anchor == null || end == null
                ? end
                : WorldLoopAttachments.transformerOfReader(level).nearestCopy(anchor, end);
    }

    public static BlockPos seat(Level level, BlockPos anchor, Vec3i end) {
        return WorldLoopAttachments.transformerOf(level)
                .nearestCopy(anchor, end instanceof BlockPos pos ? pos : new BlockPos(end));
    }

    public static BlockPos block(Level level, BlockPos pos) {
        return WorldLoopAttachments.transformerOf(level).fold(pos);
    }

    public static SectionPos section(Level level, SectionPos section) {
        return WorldLoopAttachments.transformerOf(level).fold(section);
    }

    public static long sectionKey(Level level, long section) {
        return WorldLoopAttachments.transformerOf(level).foldSectionNode(section);
    }

    public static long chunkKey(Level level, long chunk) {
        return WorldLoopAttachments.transformerOf(level).foldChunkKey(chunk);
    }

    public static DeckTransformation wireToward(Level level, Vec3 anchor, AABB wireBox) {
        WorldFold fold = WorldLoopAttachments.transformerOf(level);
        return fold.nearestCopyTransformation(anchor, wireBox.getCenter());
    }

    public static AABB apply(DeckTransformation move, AABB box) {
        return move.isIdentity() ? box : move.apply(box);
    }

    public static List<Vec3> apply(DeckTransformation move, List<Vec3> points) {
        return move.isIdentity() ? points : FoldedCopies.of(points, move::apply);
    }

    private WireSpan() {
    }
}

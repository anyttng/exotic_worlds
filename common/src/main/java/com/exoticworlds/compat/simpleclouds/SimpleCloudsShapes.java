package com.exoticworlds.compat.simpleclouds;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.ThreadScope;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class SimpleCloudsShapes {
    public static final float BLOCKS_PER_CLOUD_UNIT = 8.0F;

    private static final ThreadScope<CloudShape> BOUND = new ThreadScope<>();

    private static final ThreadLocal<@Nullable FoldShape> LAST = new ThreadLocal<>();

    private record FoldShape(WorldFold fold, CloudShape shape) {
    }

    public static @Nullable CloudShape of(@Nullable Level level) {
        WorldFold fold = level != null && level.isClientSide()
                ? WorldLoopAttachments.wrappedClientBoundsFoldOf(level)
                : WorldLoopAttachments.wrappedFoldOf(level);
        if (fold == null) {
            return null;
        }

        FoldShape last = LAST.get();
        if (last == null || last.fold() != fold) {
            last = new FoldShape(fold, CloudShape.of(fold));
            LAST.set(last);
        }

        return last.shape();
    }

    public static @Nullable CloudShape enter(@Nullable Level level) {
        return BOUND.enter(of(level));
    }

    public static void exit(@Nullable CloudShape previous) {
        BOUND.exit(previous);
    }

    public static @Nullable CloudShape current() {
        return BOUND.current();
    }

    public static float lapInCloudUnits(ToroidalShape shape, Direction.Axis axis) {
        return shape.loops(axis) ? shape.widthBlocks(axis) / BLOCKS_PER_CLOUD_UNIT : 0.0F;
    }

    public static CloudLattice latticeOf(CloudShape cloudShape, float m00, float m01, float m10, float m11) {
        ToroidalShape shape = cloudShape.shape();
        return CloudLattice.of(lapInCloudUnits(shape, Direction.Axis.X), lapInCloudUnits(shape, Direction.Axis.Z),
                cloudShape.skewBlocks() / BLOCKS_PER_CLOUD_UNIT, m00, m01, m10, m11);
    }

    private SimpleCloudsShapes() {
    }
}

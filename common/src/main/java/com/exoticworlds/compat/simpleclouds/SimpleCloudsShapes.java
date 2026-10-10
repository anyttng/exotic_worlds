package com.exoticworlds.compat.simpleclouds;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.ThreadScope;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class SimpleCloudsShapes {
    public static final float BLOCKS_PER_CLOUD_UNIT = 8.0F;

    private static final ThreadScope<CloudShape> BOUND = new ThreadScope<>();

    public static @Nullable CloudShape of(@Nullable Level level) {
        WorldFold fold = level != null && level.isClientSide()
                ? WorldLoopAttachments.wrappedClientBoundsTransformerOf(level)
                : WorldLoopAttachments.wrappedTransformerOf(level);
        return fold == null ? null : CloudShape.of(fold);
    }

    public static <R> R bound(@Nullable Level level, Supplier<R> body) {
        return BOUND.with(of(level), body);
    }

    public static void bound(@Nullable Level level, Runnable body) {
        BOUND.with(of(level), () -> {
            body.run();
            return null;
        });
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

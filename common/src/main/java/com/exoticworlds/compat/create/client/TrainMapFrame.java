package com.exoticworlds.compat.create.client;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.compat.trainmap.TrainMapRenderer;
import com.exoticworlds.compat.create.CreateSeamFold;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class TrainMapFrame {
    private static boolean bound;

    private static WorldFold frame = WorldFolds.NOOP;

    public static <T> T during(@Nullable ResourceKey<Level> drawnDimension, Supplier<T> pass) {
        boolean previouslyBound = bound;
        WorldFold previousFrame = frame;
        frame = resolve(drawnDimension);
        bound = true;
        try {
            return pass.get();
        } finally {
            bound = previouslyBound;
            frame = previousFrame;
        }
    }

    static WorldFold current() {
        return bound ? frame : resolve(TrainMapRenderer.INSTANCE.trackingDim);
    }

    public static @Nullable WorldFold wrappedFoldOf(ResourceKey<Level> dimension) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || dimension != level.dimension()) {
            return null;
        }

        return CreateSeamFold.wrappedFoldOf(level, null);
    }

    private static WorldFold resolve(@Nullable ResourceKey<Level> drawnDimension) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || (drawnDimension != null && drawnDimension != level.dimension())) {
            return WorldFolds.NOOP;
        }

        return CreateSeamFold.foldOf(level, null);
    }

    private TrainMapFrame() {
    }
}

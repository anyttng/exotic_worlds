package com.exoticworlds.core;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.ClientBoundsHolder;
import com.exoticworlds.accessors.FoldCache;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;

public final class WorldLoopAttachments {
    public static WorldFold foldOf(Level level) {
        return ((FoldCache) level).exotic_worlds$fold();
    }

    public static WorldFold foldIfLevel(@Nullable LevelHeightAccessor accessor) {
        return accessor instanceof Level level ? foldOf(level) : WorldFolds.NOOP;
    }

    public static @Nullable WorldFold wrappedFoldOf(@Nullable Level level) {
        if (level == null) {
            return null;
        }

        WorldFold fold = foldOf(level);
        return fold.isWrapped() ? fold : null;
    }

    public static @Nullable WorldFold wrappedFoldOf(
            @Nullable MinecraftServer server, ResourceKey<Level> dimension) {
        return server != null ? wrappedFoldOf(server.getLevel(dimension)) : null;
    }

    public static WorldFold clientBoundsFoldOf(Level level) {
        return level instanceof ClientBoundsHolder holder ? holder.exotic_worlds$clientBounds() : WorldFolds.NOOP;
    }

    public static @Nullable WorldFold wrappedClientBoundsFoldOf(@Nullable Level level) {
        if (level == null) {
            return null;
        }

        WorldFold fold = clientBoundsFoldOf(level);
        return fold.isWrapped() ? fold : null;
    }

    private static @Nullable Level levelOf(@Nullable LevelReader reader) {
        if (reader instanceof Level level) {
            return level;
        }

        return reader != null ? serverLevelOf(reader) : null;
    }

    public static WorldFold foldOfReader(@Nullable LevelReader reader) {
        Level level = levelOf(reader);
        if (level == null) {
            return WorldFolds.NOOP;
        }

        WorldFold clientBounds = wrappedClientBoundsFoldOf(level);
        return clientBounds != null ? clientBounds : foldOf(level);
    }

    public static WorldFold noiseFoldOf(Level level) {
        return foldOfReader(level);
    }

    public static @Nullable WorldFold noiseFoldOfReader(LevelReader reader) {
        Level level = levelOf(reader);
        return level != null ? foldOfReader(level) : null;
    }

    public static @Nullable ServerLevel serverLevelOf(LevelReader reader) {
        if (reader instanceof ServerLevel level) {
            return level;
        }

        if (reader instanceof Level level && level.isClientSide()) {
            return null;
        }

        return reader instanceof ServerLevelAccessor accessor ? accessor.getLevel() : null;
    }

    private WorldLoopAttachments() {
    }
}

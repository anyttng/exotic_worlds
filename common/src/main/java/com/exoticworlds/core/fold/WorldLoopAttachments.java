package com.exoticworlds.core.fold;

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
    public static WorldFold getFold(Level level) {
        return ((FoldCache) level).exotic_worlds$getFold();
    }

    public static WorldFold getFoldIfLevel(@Nullable LevelHeightAccessor accessor) {
        return accessor instanceof Level level ? getFold(level) : WorldFolds.NOOP;
    }

    public static @Nullable WorldFold getWrappedFold(@Nullable Level level) {
        if (level == null) {
            return null;
        }

        WorldFold fold = getFold(level);
        return fold.isWrapped() ? fold : null;
    }

    public static @Nullable WorldFold getWrappedFold(
            @Nullable MinecraftServer server, ResourceKey<Level> dimension) {
        return server != null ? getWrappedFold(server.getLevel(dimension)) : null;
    }

    public static WorldFold getClientBoundsFold(Level level) {
        return level instanceof ClientBoundsHolder holder ? holder.exotic_worlds$getClientBounds() : WorldFolds.NOOP;
    }

    public static @Nullable WorldFold getWrappedClientBoundsFold(@Nullable Level level) {
        if (level == null) {
            return null;
        }

        WorldFold fold = getClientBoundsFold(level);
        return fold.isWrapped() ? fold : null;
    }

    private static @Nullable Level getLevel(@Nullable LevelReader reader) {
        if (reader instanceof Level level) {
            return level;
        }

        return reader != null ? getServerLevel(reader) : null;
    }

    public static WorldFold getFoldOfReader(@Nullable LevelReader reader) {
        Level level = getLevel(reader);
        if (level == null) {
            return WorldFolds.NOOP;
        }

        WorldFold clientBounds = getWrappedClientBoundsFold(level);
        return clientBounds != null ? clientBounds : getFold(level);
    }

    public static WorldFold getNoiseFold(Level level) {
        return getFoldOfReader(level);
    }

    public static @Nullable WorldFold getNoiseFoldOfReader(LevelReader reader) {
        Level level = getLevel(reader);
        return level != null ? getFoldOfReader(level) : null;
    }

    public static @Nullable ServerLevel getServerLevel(LevelReader reader) {
        if (reader instanceof ServerLevel level) {
            return level;
        }

        if (reader instanceof Level level && level.isClientSide()) {
            return null;
        }

        return reader instanceof ServerLevelAccessor accessor ? accessor.getLevel() : null;
    }

    public static @Nullable WorldFold getWrappedFoldOfReader(@Nullable LevelReader reader) {
        Level level = getLevel(reader);
        if (level == null) {
            return null;
        }

        WorldFold clientBounds = getWrappedClientBoundsFold(level);
        return clientBounds != null ? clientBounds : getWrappedFold(level);
    }

    private WorldLoopAttachments() {
    }
}

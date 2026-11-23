package com.exoticworlds.compat.map;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.level.PublishedShapes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ClientFolds {
    public static @Nullable WorldFold current() {
        return of(Minecraft.getInstance().level);
    }

    public static @Nullable WorldFold of(@Nullable Level level) {
        return level instanceof ClientLevel client ? WorldLoopAttachments.wrappedClientBoundsFoldOf(client) : null;
    }

    public static @Nullable WorldFold of(ResourceKey<Level> dimension) {
        return PublishedShapes.wrappedFoldOf(dimension);
    }

    public static BlockPos fold(@Nullable WorldFold fold, BlockPos pos) {
        return fold == null ? pos : fold.fold(pos);
    }

    public static Vec3 fold(@Nullable WorldFold fold, Vec3 point) {
        return fold == null ? point : fold.fold(point);
    }

    public static Vec3 fold(@Nullable WorldFold fold, double blockX, double blockZ) {
        return fold(fold, new Vec3(blockX, 0.0, blockZ));
    }

    public static ChunkPos fold(@Nullable WorldFold fold, ChunkPos chunk) {
        return fold == null ? chunk : fold.fold(chunk);
    }

    private ClientFolds() {
    }
}

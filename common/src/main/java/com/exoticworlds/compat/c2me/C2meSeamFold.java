package com.exoticworlds.compat.c2me;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.core.WorldFold;
import com.ishland.c2me.base.common.scheduler.SchedulingManager;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.flowsched.executor.LockToken;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;

public final class C2meSeamFold {
    public static <T> StaticCache2D.Initializer<T> foldingInitializer(
            ChunkLoadingContext context,
            int centerX,
            int centerZ,
            StaticCache2D.Initializer<T> initializer) {
        WorldFold fold = ((FoldSource) context.theChunkSystem()).exotic_worlds$wrappedFold();
        return foldingInitializer(fold, centerX, centerZ, initializer);
    }

    private static <T> StaticCache2D.Initializer<T> foldingInitializer(
            @Nullable WorldFold fold,
            int centerX,
            int centerZ,
            StaticCache2D.Initializer<T> initializer) {
        if (fold == null) {
            return initializer;
        }

        return (slotX, slotZ) -> {
            ChunkPos slot = canonicalSlot(fold, centerX, centerZ, slotX, slotZ);
            return initializer.get(slot.x, slot.z);
        };
    }

    private static ChunkPos canonicalSlot(
            WorldFold fold, int centerX, int centerZ, int slotX, int slotZ) {
        if (slotX == centerX && slotZ == centerZ) {
            return new ChunkPos(slotX, slotZ);
        }

        return canonical(fold, slotX, slotZ);
    }

    public static ChunkPos canonical(WorldFold fold, int chunkX, int chunkZ) {
        return fold.fold(new ChunkPos(chunkX, chunkZ));
    }

    public static long lockKey(SchedulingManager schedulingManager, long chunkKey) {
        WorldFold fold = ((FoldSource) schedulingManager).exotic_worlds$wrappedFold();
        if (fold == null) {
            return chunkKey;
        }

        return fold.foldChunkKey(chunkKey);
    }

    public static LockToken[] lockTokens(SchedulingManager schedulingManager, LockToken[] tokens) {
        if (((FoldSource) schedulingManager).exotic_worlds$wrappedFold() == null) {
            return tokens;
        }

        return withoutRepeats(tokens);
    }

    static LockToken[] withoutRepeats(LockToken[] tokens) {
        return new ObjectLinkedOpenHashSet<>(tokens).toArray(LockToken[]::new);
    }

    private C2meSeamFold() {
    }
}

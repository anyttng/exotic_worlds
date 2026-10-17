package com.exoticworlds.compat.c2me;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.TransformerSource;
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
        WorldFold transformer = ((TransformerSource) context.theChunkSystem()).toroidal$wrappedTransformer();
        return foldingInitializer(transformer, centerX, centerZ, initializer);
    }

    private static <T> StaticCache2D.Initializer<T> foldingInitializer(
            @Nullable WorldFold transformer,
            int centerX,
            int centerZ,
            StaticCache2D.Initializer<T> initializer) {
        if (transformer == null) {
            return initializer;
        }

        return (slotX, slotZ) -> {
            ChunkPos slot = canonicalSlot(transformer, centerX, centerZ, slotX, slotZ);
            return initializer.get(slot.x(), slot.z());
        };
    }

    private static ChunkPos canonicalSlot(
            WorldFold transformer, int centerX, int centerZ, int slotX, int slotZ) {
        if (slotX == centerX && slotZ == centerZ) {
            return new ChunkPos(slotX, slotZ);
        }

        return canonical(transformer, slotX, slotZ);
    }

    public static ChunkPos canonical(WorldFold transformer, int chunkX, int chunkZ) {
        return transformer.fold(new ChunkPos(chunkX, chunkZ));
    }

    public static long lockKey(SchedulingManager schedulingManager, long chunkKey) {
        WorldFold transformer = ((TransformerSource) schedulingManager).toroidal$wrappedTransformer();
        if (transformer == null) {
            return chunkKey;
        }

        return transformer.foldChunkKey(chunkKey);
    }

    public static LockToken[] lockTokens(SchedulingManager schedulingManager, LockToken[] tokens) {
        if (((TransformerSource) schedulingManager).toroidal$wrappedTransformer() == null) {
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

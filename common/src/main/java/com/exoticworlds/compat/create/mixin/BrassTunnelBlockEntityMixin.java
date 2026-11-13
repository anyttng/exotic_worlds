package com.exoticworlds.compat.create.mixin;

import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.zurrtum.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import com.exoticworlds.core.WorldLoopAttachments;

@Mixin(BrassTunnelBlockEntity.class)
public abstract class BrassTunnelBlockEntityMixin {
    @WrapOperation(method = { "grabAllStacksOfGroup", "gatherValidOutputs" },
            at = @At(value = "INVOKE",
                    target = "Lcom/zurrtum/create/content/logistics/tunnel/BrassTunnelBlockEntity;getAdjacent(Z)Lcom/zurrtum/create/content/logistics/tunnel/BrassTunnelBlockEntity;"),
            require = 2,
            allow = 2)
    private @Nullable BrassTunnelBlockEntity exotic_worlds$closeTheGroupChain(BrassTunnelBlockEntity walker,
            boolean leftSide, Operation<BrassTunnelBlockEntity> original,
            @Share("visitedTunnels") LocalRef<Set<BrassTunnelBlockEntity>> visitedRef,
            @Share("visitedTunnelsResolved") LocalBooleanRef resolved) {
        BrassTunnelBlockEntity adjacent = original.call(walker, leftSide);
        if (adjacent == null) {
            return null;
        }

        if (!resolved.get()) {
            if (WorldLoopAttachments.wrappedFoldOfReader(walker.getLevel()) != null) {
                Set<BrassTunnelBlockEntity> visited = new HashSet<>();
                visited.add(walker);
                visitedRef.set(visited);
            }

            resolved.set(true);
        }

        Set<BrassTunnelBlockEntity> visited = visitedRef.get();
        if (visited == null) {
            return adjacent;
        }

        return visited.add(adjacent) ? adjacent : null;
    }
}

package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;

@Mixin(ChunkTrackingView.Positioned.class)
public abstract class ChunkTrackingViewPositionedMixin implements TransformerHolder {
    @Unique
    private WorldFold exotic_worlds$transformer = WorldFolds.NOOP;

    @Override
    public WorldFold exotic_worlds$transformer() {
        return this.exotic_worlds$transformer;
    }

    @Override
    public void exotic_worlds$setTransformer(WorldFold transformer) {
        this.exotic_worlds$transformer = transformer;
    }

    @ModifyReturnValue(method = "contains(IIZ)Z", at = @At("RETURN"))
    private boolean exotic_worlds$containsWrapped(boolean original, int chunkX, int chunkZ, boolean includeNeighbors) {
        if (!this.exotic_worlds$transformer.isWrapped()) {
            return original;
        }

        ChunkPos center = ((ChunkTrackingView.Positioned) (Object) this).center();
        int viewDistance = ((ChunkTrackingView.Positioned) (Object) this).viewDistance();

        ChunkPos unwrapped = this.exotic_worlds$transformer.nearestCopy(center, new ChunkPos(chunkX, chunkZ));

        return ChunkTrackingView.isWithinDistance(
                center.x(), center.z(), viewDistance, unwrapped.x(), unwrapped.z(), includeNeighbors);
    }

    @ModifyArg(method = "forEach", at = @At(value = "INVOKE", target = InjectionTargets.CONSUMER_ACCEPT))
    private Object exotic_worlds$foldVisited(Object pos) {
        return this.exotic_worlds$transformer.fold((ChunkPos) pos);
    }
}

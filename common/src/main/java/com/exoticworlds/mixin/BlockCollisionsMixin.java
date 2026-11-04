package com.exoticworlds.mixin;

import java.util.function.BiFunction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(BlockCollisions.class)
public abstract class BlockCollisionsMixin {
    @Unique
    private static final String GET_CHUNK =
            "Lnet/minecraft/world/level/BlockCollisions;getChunk(II)Lnet/minecraft/world/level/BlockGetter;";

    @Unique
    private WorldFold exotic_worlds$fold = WorldFolds.NOOP;

    @Unique
    private long exotic_worlds$foldedNode;

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/world/phys/shapes/CollisionContext;Lnet/minecraft/world/phys/AABB;ZLjava/util/function/BiFunction;)V",
            at = @At("RETURN"))
    private void exotic_worlds$resolveFold(CollisionGetter collisionGetter, CollisionContext context, AABB box,
            boolean onlySuffocatingBlocks, BiFunction<BlockPos.MutableBlockPos, VoxelShape, ?> resultProvider,
            CallbackInfo ci) {
        this.exotic_worlds$fold = WorldLoopAttachments.foldIfLevel(collisionGetter);
    }

    // Must stay above the Z handler: Mixin runs them in declaration order, and the Z handler reads the node kept here.
    @ModifyArg(method = "computeNext", at = @At(value = "INVOKE", target = GET_CHUNK), index = 0)
    private int exotic_worlds$chunkXThroughSeam(int x, int z) {
        this.exotic_worlds$foldedNode = this.exotic_worlds$fold.foldBlockNode(BlockPos.asLong(x, 0, z));
        return BlockPos.getX(this.exotic_worlds$foldedNode);
    }

    @ModifyArg(method = "computeNext", at = @At(value = "INVOKE", target = GET_CHUNK), index = 1)
    private int exotic_worlds$chunkZThroughSeam(int x, int z) {
        return BlockPos.getZ(this.exotic_worlds$foldedNode);
    }
}

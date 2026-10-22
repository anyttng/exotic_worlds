package com.exoticworlds.mixin;

import java.util.function.BiFunction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(BlockCollisions.class)
public abstract class BlockCollisionsMixin {
    @Unique
    private WorldFold exotic_worlds$transformer = WorldFolds.NOOP;

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/world/phys/shapes/CollisionContext;Lnet/minecraft/world/phys/AABB;ZLjava/util/function/BiFunction;)V",
            at = @At("RETURN"))
    private void exotic_worlds$resolveTransformer(CollisionGetter collisionGetter, CollisionContext context, AABB box,
            boolean onlySuffocatingBlocks, BiFunction<BlockPos.MutableBlockPos, VoxelShape, ?> resultProvider,
            CallbackInfo ci) {
        this.exotic_worlds$transformer = WorldLoopAttachments.transformerIfLevel(collisionGetter);
    }

    @WrapOperation(
            method = "computeNext",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/BlockCollisions;getChunk(II)Lnet/minecraft/world/level/BlockGetter;"))
    private BlockGetter exotic_worlds$chunkThroughSeam(BlockCollisions<?> self, int x, int z, Operation<BlockGetter> original) {
        long folded = this.exotic_worlds$transformer.foldBlockNode(BlockPos.asLong(x, 0, z));
        return original.call(self, BlockPos.getX(folded), BlockPos.getZ(folded));
    }
}

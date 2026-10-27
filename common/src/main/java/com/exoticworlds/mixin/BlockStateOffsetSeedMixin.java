package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.BlockOffsetSeed;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateOffsetSeedMixin {
    @Shadow
    protected abstract BlockState asState();

    @WrapOperation(
            method = "getOffset",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$OffsetFunction;evaluate(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$seedFromCanonical(BlockBehaviour.OffsetFunction function, BlockState state, BlockPos pos,
            Operation<Vec3> original) {
        return original.call(function, state, BlockOffsetSeed.seedPosition(pos));
    }

    @WrapMethod(method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    private VoxelShape exotic_worlds$seedShape(BlockGetter level, BlockPos pos, CollisionContext context,
            Operation<VoxelShape> original) {
        BlockOffsetSeed scope = BlockOffsetSeed.enter(level, this.asState());
        try {
            return original.call(level, pos, context);
        } finally {
            BlockOffsetSeed.exit(scope);
        }
    }

    @WrapMethod(method = InjectionTargets.BLOCK_STATE_BASE_GET_COLLISION_SHAPE)
    private VoxelShape exotic_worlds$seedCollisionShape(BlockGetter level, BlockPos pos, CollisionContext context,
            Operation<VoxelShape> original) {
        BlockOffsetSeed scope = BlockOffsetSeed.enter(level, this.asState());
        try {
            return original.call(level, pos, context);
        } finally {
            BlockOffsetSeed.exit(scope);
        }
    }

    @WrapMethod(method = "getEntityInsideCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    private VoxelShape exotic_worlds$seedEntityInsideShape(BlockGetter level, BlockPos pos, Entity entity,
            Operation<VoxelShape> original) {
        BlockOffsetSeed scope = BlockOffsetSeed.enter(level, this.asState());
        try {
            return original.call(level, pos, entity);
        } finally {
            BlockOffsetSeed.exit(scope);
        }
    }

    @WrapMethod(method = "getBlockSupportShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    private VoxelShape exotic_worlds$seedSupportShape(BlockGetter level, BlockPos pos, Operation<VoxelShape> original) {
        BlockOffsetSeed scope = BlockOffsetSeed.enter(level, this.asState());
        try {
            return original.call(level, pos);
        } finally {
            BlockOffsetSeed.exit(scope);
        }
    }

    @WrapMethod(method = "getVisualShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    private VoxelShape exotic_worlds$seedVisualShape(BlockGetter level, BlockPos pos, CollisionContext context,
            Operation<VoxelShape> original) {
        BlockOffsetSeed scope = BlockOffsetSeed.enter(level, this.asState());
        try {
            return original.call(level, pos, context);
        } finally {
            BlockOffsetSeed.exit(scope);
        }
    }

    @WrapMethod(method = "getInteractionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    private VoxelShape exotic_worlds$seedInteractionShape(BlockGetter level, BlockPos pos, Operation<VoxelShape> original) {
        BlockOffsetSeed scope = BlockOffsetSeed.enter(level, this.asState());
        try {
            return original.call(level, pos);
        } finally {
            BlockOffsetSeed.exit(scope);
        }
    }
}

package com.exoticworlds.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
    public abstract boolean hasOffsetFunction();

    @WrapOperation(
            method = "getOffset",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$OffsetFunction;evaluate(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$seedFromCanonical(BlockBehaviour.OffsetFunction function, BlockState state, BlockPos pos,
            Operation<Vec3> original) {
        return original.call(function, state, BlockOffsetSeed.seedPosition(pos));
    }

    @WrapMethod(method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    private VoxelShape toroidal$seedShape(BlockGetter level, BlockPos pos, CollisionContext context,
            Operation<VoxelShape> original) {
        return this.hasOffsetFunction()
                ? toroidal$seeded(level, () -> original.call(level, pos, context))
                : original.call(level, pos, context);
    }

    @WrapMethod(method = InjectionTargets.BLOCK_STATE_BASE_GET_COLLISION_SHAPE)
    private VoxelShape toroidal$seedCollisionShape(BlockGetter level, BlockPos pos, CollisionContext context,
            Operation<VoxelShape> original) {
        return this.hasOffsetFunction()
                ? toroidal$seeded(level, () -> original.call(level, pos, context))
                : original.call(level, pos, context);
    }

    @WrapMethod(method = "getEntityInsideCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    private VoxelShape toroidal$seedEntityInsideShape(BlockGetter level, BlockPos pos, Entity entity,
            Operation<VoxelShape> original) {
        return this.hasOffsetFunction()
                ? toroidal$seeded(level, () -> original.call(level, pos, entity))
                : original.call(level, pos, entity);
    }

    @WrapMethod(method = "getBlockSupportShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    private VoxelShape toroidal$seedSupportShape(BlockGetter level, BlockPos pos, Operation<VoxelShape> original) {
        return this.hasOffsetFunction()
                ? toroidal$seeded(level, () -> original.call(level, pos))
                : original.call(level, pos);
    }

    @WrapMethod(method = "getVisualShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    private VoxelShape toroidal$seedVisualShape(BlockGetter level, BlockPos pos, CollisionContext context,
            Operation<VoxelShape> original) {
        return this.hasOffsetFunction()
                ? toroidal$seeded(level, () -> original.call(level, pos, context))
                : original.call(level, pos, context);
    }

    @WrapMethod(method = "getInteractionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    private VoxelShape toroidal$seedInteractionShape(BlockGetter level, BlockPos pos, Operation<VoxelShape> original) {
        return this.hasOffsetFunction()
                ? toroidal$seeded(level, () -> original.call(level, pos))
                : original.call(level, pos);
    }

    @Unique
    private static VoxelShape toroidal$seeded(BlockGetter level, Supplier<VoxelShape> shape) {
        try (BlockOffsetSeed _ = BlockOffsetSeed.seededBy(level)) {
            return shape.get();
        }
    }
}

package com.exoticworlds.compat.create.mixin;

import java.util.function.BiFunction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.contraptions.glue.SuperGlueHandler;
import com.exoticworlds.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(SuperGlueHandler.class)
public class SuperGlueHandlerMixin {
    @ModifyVariable(method = "glueInOffHandAppliesOnBlockPlace", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static BlockPos exotic_worlds$canonicalizeThePlacedBlock(BlockPos placed,
            @Local(argsOnly = true) ServerLevel world) {
        return CreateSeamFold.canonical(world, placed);
    }

    @ModifyArg(method = "glueInOffHandAppliesOnBlockPlace",
            at = @At(value = "INVOKE",
                    target = "Lcom/zurrtum/create/catnip/levelWrappers/RayTraceLevel;<init>("
                            + "Lnet/minecraft/world/level/LevelAccessor;Ljava/util/function/BiFunction;)V"),
            index = 1)
    private static BiFunction<BlockPos, BlockState, BlockState> exotic_worlds$traceAgainstTheCanonicalBlock(
            BiFunction<BlockPos, BlockState, BlockState> stateGetter, @Local(argsOnly = true) ServerLevel world) {
        return (tracePos, state) -> stateGetter.apply(CreateSeamFold.canonical(world, tracePos), state);
    }

    @WrapOperation(method = "glueInOffHandAppliesOnBlockPlace",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;equals(Ljava/lang/Object;)Z"))
    private static boolean exotic_worlds$namesThePlacedBlock(BlockPos hitNeighbor, Object placed,
            Operation<Boolean> original, @Local(argsOnly = true) ServerLevel world) {
        return original.call(CreateSeamFold.canonical(world, hitNeighbor), placed);
    }
}

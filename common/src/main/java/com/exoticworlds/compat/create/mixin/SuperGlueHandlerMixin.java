package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.glue.SuperGlueHandler;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.CreateSeamFold;

import java.util.function.BiFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = SuperGlueHandler.class, remap = false)
public class SuperGlueHandlerMixin {
    @ModifyVariable(method = "glueInOffHandAppliesOnBlockPlace", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static BlockPos exotic_worlds$canonicalizeThePlacedBlock(BlockPos placed,
            @Local(argsOnly = true) Player placer) {
        return CreateSeamFold.canonicalOnServer(placer.level(), placed);
    }

    @ModifyArg(method = "glueInOffHandAppliesOnBlockPlace",
            at = @At(value = "INVOKE",
                    target = "Lnet/createmod/catnip/levelWrappers/RayTraceLevel;<init>(Lnet/minecraft/world/level/LevelAccessor;Ljava/util/function/BiFunction;)V"),
            index = 1)
    private static BiFunction<BlockPos, BlockState, BlockState> exotic_worlds$traceAgainstTheCanonicalBlock(
            BiFunction<BlockPos, BlockState, BlockState> stateGetter, @Local(argsOnly = true) Player placer) {
        return (tracePos, state) -> stateGetter.apply(CreateSeamFold.canonicalOnServer(placer.level(), tracePos),
                state);
    }

    @WrapOperation(method = "glueInOffHandAppliesOnBlockPlace",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_EQUALS))
    private static boolean exotic_worlds$namesThePlacedBlock(BlockPos hitNeighbor, Object placed,
            Operation<Boolean> original, @Local(argsOnly = true) Player placer) {
        return original.call(CreateSeamFold.canonicalOnServer(placer.level(), hitNeighbor), placed);
    }
}

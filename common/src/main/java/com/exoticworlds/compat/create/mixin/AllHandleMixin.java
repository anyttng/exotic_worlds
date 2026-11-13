package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.AllHandle;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.CreateSeamFold;
import com.exoticworlds.engine.seam.SeamRange;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(AllHandle.class)
public class AllHandleMixin {
    private static final int BLOCK_GATES = 3;
    private static final int POINT_GATES = 3;

    @WrapOperation(
            method = {"onBlockEntityConfiguration", "lambda$onCurvedTrackDestroy$0", "onClipboardEdit"},
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_CLOSER_THAN),
            require = BLOCK_GATES)
    private static boolean exotic_worlds$blockReachThroughSeam(BlockPos block, Vec3i other, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) ServerGamePacketListenerImpl listener) {
        return original.call(block, SeamRange.toward(listener.player, block, other), distance);
    }

    @WrapOperation(
            method = {"onTrainRelocation", "onControlsInput"},
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_CLOSER_THAN),
            require = POINT_GATES)
    private static boolean exotic_worlds$pointReachThroughSeam(Vec3 point, Position other, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) ServerGamePacketListenerImpl listener) {
        return original.call(point, SeamRange.toward(listener.player, point, other), distance);
    }

    @ModifyExpressionValue(method = "lambda$onChainConveyorConnection$0",
            at = @At(value = "INVOKE",
                    target = "Lcom/zurrtum/create/infrastructure/packet/c2s/ChainConveyorConnectionPacket;"
                            + "targetPos()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos exotic_worlds$targetTowardTheConveyor(BlockPos targetPos,
            @Local(argsOnly = true) BlockEntity conveyor) {
        return CreateSeamFold.nearestCopy(conveyor.getLevel(), conveyor.getBlockPos(), targetPos);
    }

    @WrapOperation(method = "onPackagePortPlacement",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_CLOSER_THAN))
    private static boolean exotic_worlds$targetTowardThePort(Vec3 target, Position port, double range,
            Operation<Boolean> original, @Local(argsOnly = true) ServerGamePacketListenerImpl listener) {
        if (!(port instanceof Vec3 anchor)) {
            return original.call(target, port, range);
        }

        return original.call(CreateSeamFold.nearestCopy(listener.player.level(), anchor, target), port, range);
    }
}

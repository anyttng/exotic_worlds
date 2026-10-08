package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.mekanism.MekanismSeam;
import com.exoticworlds.engine.level.CurrentServer;

import mekanism.common.content.transporter.TransporterManager;
import mekanism.common.content.transporter.TransporterStack;
import mekanism.common.util.WorldUtils;

import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;

@Mixin(value = TransporterManager.class, remap = false)
public class TransporterManagerMixin {
    private static final int LAST_TRANSPORTER_INDEX = 1;

    @WrapOperation(method = "predictFlowing",
            at = @At(value = "INVOKE",
                    target = "Lmekanism/common/content/transporter/TransporterStack;getSideOfDest()"
                            + "Lnet/minecraft/core/Direction;"))
    private static Direction toroidal$sideOfDestination(TransporterStack stack, Operation<Direction> original,
            @Local(argsOnly = true) GlobalPos position) {
        MinecraftServer server = CurrentServer.get();
        if (server == null || !stack.hasPath()) {
            return original.call(stack);
        }

        long dest = stack.getDest();
        long lastTransporter = MekanismSeam.nearestCopy(server.getLevel(position.dimension()), dest,
                stack.getPath().getLong(LAST_TRANSPORTER_INDEX));
        return WorldUtils.sideDifference(lastTransporter, dest);
    }
}

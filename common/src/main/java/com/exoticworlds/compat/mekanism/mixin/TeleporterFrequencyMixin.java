package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.level.CurrentServer;

import mekanism.common.content.teleporter.TeleporterFrequency;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;

@Mixin(value = TeleporterFrequency.class, remap = false)
public class TeleporterFrequencyMixin {
    @WrapOperation(
            method = "getClosestCoords",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR),
            require = 2,
            expect = 2)
    private double exotic_worlds$distanceTheShortWayRound(BlockPos from, Vec3i to, Operation<Double> original,
            @Local(argsOnly = true) GlobalPos source, @Local(name = "iterCoord") GlobalPos candidate) {
        MinecraftServer server = CurrentServer.get();
        if (server == null || source.dimension() != candidate.dimension()) {
            return original.call(from, to);
        }

        return WorldLoopAttachments.foldOfReader(server.getLevel(source.dimension()))
                .sqrDistance(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());
    }
}

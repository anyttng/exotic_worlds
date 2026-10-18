package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.trains.track.TrackPlacement;
import com.exoticworlds.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Mixin(value = TrackPlacement.class, remap = false)
public class TrackPlacementMixin {
    @ModifyExpressionValue(
            method = "tryConnect",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/track/TrackPlacement$ConnectingFrom;pos()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos exotic_worlds$foldStoredBlock(BlockPos storedPos, Level level, Player player, BlockPos clickedPos,
            BlockState clickedState, ItemStack stack, boolean girder, boolean maximiseTurn) {
        return CreateSeamFold.nearestCopy(level, clickedPos, storedPos);
    }

    @ModifyExpressionValue(
            method = "tryConnect",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/track/TrackPlacement$ConnectingFrom;end()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 exotic_worlds$foldStoredEnd(Vec3 storedEnd, Level level, Player player, BlockPos clickedPos,
            BlockState clickedState, ItemStack stack, boolean girder, boolean maximiseTurn) {
        return CreateSeamFold.nearestCopy(level, Vec3.atCenterOf(clickedPos), storedEnd);
    }
}

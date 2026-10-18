package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.redstoneRequester.AutoRequestData;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(value = AutoRequestData.class, remap = false)
public class AutoRequestDataMixin {
    @WrapOperation(method = "readFromItem",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_SUBTRACT))
    private static BlockPos exotic_worlds$foldKeeperDelta(BlockPos target, Vec3i placed, Operation<BlockPos> original,
            Level level, Player player, BlockPos position, ItemStack itemStack) {
        return CreateSeamFold.foldDelta(level, placed, target, original.call(target, placed));
    }
}

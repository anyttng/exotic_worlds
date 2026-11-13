package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.zurrtum.create.content.logistics.factoryBoard.PanelSlot;
import com.exoticworlds.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Mixin(FactoryPanelBlock.class)
public class FactoryPanelBlockMixin {
    private static final String GET_TARGETED_SLOT =
            "Lcom/zurrtum/create/content/logistics/factoryBoard/FactoryPanelBlock;getTargetedSlot"
                    + "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/phys/Vec3;)"
                    + "Lcom/zurrtum/create/content/logistics/factoryBoard/PanelSlot;";

    @WrapOperation(method = "tryDestroySubPanelFirst",
            at = @At(value = "INVOKE", target = GET_TARGETED_SLOT))
    private PanelSlot exotic_worlds$foldBreakAim(BlockPos pos, BlockState state, Vec3 location,
            Operation<PanelSlot> original, BlockState brokenState, Level level, BlockPos brokenPos, Player player) {
        return original.call(pos, state, exotic_worlds$towardPanel(level, pos, location));
    }

    @WrapOperation(method = "setPlacedBy",
            at = @At(value = "INVOKE", target = GET_TARGETED_SLOT))
    private PanelSlot exotic_worlds$foldPlaceAim(BlockPos pos, BlockState state, Vec3 location,
            Operation<PanelSlot> original, Level level, BlockPos placedPos, BlockState placedState,
            LivingEntity placer, ItemStack stack) {
        return original.call(pos, state, exotic_worlds$towardPanel(level, pos, location));
    }

    @Unique
    private static Vec3 exotic_worlds$towardPanel(Level level, BlockPos pos, Vec3 location) {
        return CreateSeamFold.nearestCopy(level, Vec3.atCenterOf(pos), location);
    }
}

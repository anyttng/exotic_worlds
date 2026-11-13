package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.AllDataComponents;
import com.zurrtum.create.content.trains.track.TrackTargetingBlockItem;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

@Mixin(TrackTargetingBlockItem.class)
public class TrackTargetingBlockItemMixin {
    @WrapOperation(method = "useOn",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.ITEM_STACK_GET))
    private Object exotic_worlds$foldSelectedTrack(ItemStack stack, DataComponentType<?> component,
            Operation<Object> original, UseOnContext context) {
        Object value = original.call(stack, component);
        if (component != AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS
                || !(value instanceof BlockPos selected)) {
            return value;
        }

        return CreateSeamFold.nearestCopy(context.getLevel(), context.getClickedPos(), selected);
    }
}

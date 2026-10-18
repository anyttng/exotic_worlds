package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.client.engine.ComponentReadFold;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;

@Mixin(DataComponentHolder.class)
public interface DataComponentHolderMixin {
    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private Object exotic_worlds$seatNamedPosition(Object stored,
            @Local(argsOnly = true) DataComponentType<?> type) {
        return ComponentReadFold.seated(type, stored);
    }

    @ModifyReturnValue(method = "getOrDefault", at = @At("RETURN"))
    private Object exotic_worlds$seatNamedPositionOrDefault(Object stored,
            @Local(argsOnly = true) DataComponentType<?> type) {
        return ComponentReadFold.seated(type, stored);
    }
}

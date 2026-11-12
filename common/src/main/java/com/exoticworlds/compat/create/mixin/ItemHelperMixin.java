package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.foundation.item.ItemHelper;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

@Mixin(ItemHelper.class)
public class ItemHelperMixin {
    @ModifyArg(method = "getInventoryCache", at = @At(value = "INVOKE", target = InjectionTargets.MAP_PUT), index = 0)
    private static Object exotic_worlds$cacheUnderCanonicalKey(Object pos, @Local(argsOnly = true) ServerLevel level) {
        return WorldLoopAttachments.foldOf(level).fold((BlockPos) pos);
    }
}

package com.exoticworlds.compat.mekanism.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.mekanism.RadiationLevelFrame;
import com.exoticworlds.core.WorldLoopAttachments;

import mekanism.common.lib.radiation.RadiationLevelData;
import mekanism.common.lib.radiation.RadiationManager;

import net.minecraft.world.level.Level;

@Mixin(value = RadiationManager.class, remap = false)
public class RadiationManagerMixin {
    @ModifyReturnValue(
            method = {
                "getData(Lnet/minecraft/world/level/Level;)Lmekanism/common/lib/radiation/RadiationLevelData;",
                "getOrCreateData(Lnet/minecraft/world/level/Level;)Lmekanism/common/lib/radiation/RadiationLevelData;"
            },
            at = @At("RETURN"),
            require = 2)
    private static @Nullable RadiationLevelData exotic_worlds$bindLevelFold(@Nullable RadiationLevelData data,
            @Local(argsOnly = true) Level level) {
        if (data != null) {
            ((RadiationLevelFrame) (Object) data).exotic_worlds$bind(WorldLoopAttachments.foldOf(level));
        }

        return data;
    }
}

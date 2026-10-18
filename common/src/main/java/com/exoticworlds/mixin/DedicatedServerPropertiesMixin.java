package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.gen.ServerLevelType;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.HolderLookup;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.world.level.levelgen.WorldDimensions;

@Mixin(DedicatedServerProperties.class)
public class DedicatedServerPropertiesMixin {
    @ModifyReturnValue(method = "createDimensions", at = @At("RETURN"))
    private WorldDimensions exotic_worlds$keepChosenShape(WorldDimensions created,
            @Local(argsOnly = true) HolderLookup.Provider registries) {
        return ServerLevelType.keepChosenShape(created, ((SettingsAccessor) (Object) this).exotic_worlds$getProperties(),
                registries);
    }
}

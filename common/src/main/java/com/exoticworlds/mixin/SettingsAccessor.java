package com.exoticworlds.mixin;

import java.util.Properties;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.server.dedicated.Settings;

@Mixin(Settings.class)
public interface SettingsAccessor {
    @Accessor("properties")
    Properties toroidal$getProperties();
}

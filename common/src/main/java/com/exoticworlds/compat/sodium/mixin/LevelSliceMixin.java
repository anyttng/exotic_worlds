package com.exoticworlds.compat.sodium.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.accessors.RegionLevelSource;

import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

@Mixin(LevelSlice.class)
public class LevelSliceMixin implements RegionLevelSource {
    @Shadow
    @Final
    private ClientLevel level;

    @Override
    public Level toroidal$regionLevel() {
        return this.level;
    }
}

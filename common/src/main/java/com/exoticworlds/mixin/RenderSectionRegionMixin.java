package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.accessors.RegionLevelSource;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.world.level.Level;

@Mixin(RenderSectionRegion.class)
public class RenderSectionRegionMixin implements RegionLevelSource {
    @Shadow
    @Final
    private ClientLevel level;

    @Override
    public Level toroidal$regionLevel() {
        return this.level;
    }
}

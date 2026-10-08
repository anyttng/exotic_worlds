package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.accessors.RegionLevelSource;

import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.world.level.Level;

@Mixin(RenderChunkRegion.class)
public class RenderChunkRegionMixin implements RegionLevelSource {
    @Shadow
    @Final
    protected Level level;

    @Override
    public Level toroidal$regionLevel() {
        return this.level;
    }
}

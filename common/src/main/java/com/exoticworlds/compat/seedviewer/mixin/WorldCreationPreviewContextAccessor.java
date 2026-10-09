package com.exoticworlds.compat.seedviewer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.acenia.seedviewer.client.screen.createworld.WorldCreationPreviewContext;
import net.acenia.seedviewer.client.worldgen.BiomeSampler;

@Mixin(WorldCreationPreviewContext.class)
public interface WorldCreationPreviewContextAccessor {
    @Accessor("biomeSampler")
    BiomeSampler toroidal$biomeSampler();
}

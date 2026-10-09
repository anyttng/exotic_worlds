package com.exoticworlds.compat.distanthorizons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.ChunkLightStorage;

@Mixin(ChunkWrapper.class)
public interface ChunkWrapperAccessor {
    @Accessor("blockLightStorage")
    ChunkLightStorage toroidal$blockLightStorage();

    @Accessor("skyLightStorage")
    ChunkLightStorage toroidal$skyLightStorage();
}

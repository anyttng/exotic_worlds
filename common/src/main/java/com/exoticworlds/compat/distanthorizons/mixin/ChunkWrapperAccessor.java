package com.exoticworlds.compat.distanthorizons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.exoticworlds.compat.distanthorizons.DhInjectionTargets;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.ChunkLightStorage;

@Mixin(targets = {DhInjectionTargets.CHUNK_WRAPPER_NEOFORGE, DhInjectionTargets.CHUNK_WRAPPER}, remap = false)
public interface ChunkWrapperAccessor {
    @Accessor("blockLightStorage")
    ChunkLightStorage exotic_worlds$blockLightStorage();

    @Accessor("blockLightStorage")
    void exotic_worlds$setBlockLightStorage(ChunkLightStorage storage);

    @Accessor("skyLightStorage")
    ChunkLightStorage exotic_worlds$skyLightStorage();

    @Accessor("skyLightStorage")
    void exotic_worlds$setSkyLightStorage(ChunkLightStorage storage);
}

package com.exoticworlds.compat.distanthorizons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.exoticworlds.compat.distanthorizons.DhInjectionTargets;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.ChunkLightStorage;

@Mixin(targets = {DhInjectionTargets.CHUNK_WRAPPER_NEOFORGE, DhInjectionTargets.CHUNK_WRAPPER}, remap = false)
public interface ChunkWrapperAccessor {
    @Accessor("blockLightStorage")
    ChunkLightStorage toroidal$blockLightStorage();

    @Accessor("blockLightStorage")
    void toroidal$setBlockLightStorage(ChunkLightStorage storage);

    @Accessor("skyLightStorage")
    ChunkLightStorage toroidal$skyLightStorage();

    @Accessor("skyLightStorage")
    void toroidal$setSkyLightStorage(ChunkLightStorage storage);
}

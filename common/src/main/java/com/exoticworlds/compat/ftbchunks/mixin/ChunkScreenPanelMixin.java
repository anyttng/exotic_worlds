package com.exoticworlds.compat.ftbchunks.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.compat.ftbchunks.FtbChunksFold;
import com.exoticworlds.compat.ftbchunks.FtbChunksInjectionTargets;

import dev.ftb.mods.ftbchunks.client.gui.ChunkScreenPanel;
import dev.ftb.mods.ftblibrary.math.XZ;

@Mixin(value = ChunkScreenPanel.class, remap = false)
public abstract class ChunkScreenPanelMixin {
    @ModifyArg(method = {"mouseReleased", "removeAllClaims"},
            at = @At(value = "INVOKE", target = FtbChunksInjectionTargets.REQUEST_CHUNK_CHANGE_PACKET_INIT),
            index = 1)
    private Set<XZ> toroidal$foldRequestedChunks(Set<XZ> chunks) {
        return FtbChunksFold.foldedChunks(chunks);
    }
}

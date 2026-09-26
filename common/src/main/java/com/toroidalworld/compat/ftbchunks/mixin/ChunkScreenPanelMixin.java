package com.toroidalworld.compat.ftbchunks.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.toroidalworld.compat.ftbchunks.FtbChunksFold;

import dev.ftb.mods.ftbchunks.client.gui.map.ChunkScreenPanel;
import dev.ftb.mods.ftblibrary.math.XZ;

@Mixin(value = ChunkScreenPanel.class, remap = false)
public abstract class ChunkScreenPanelMixin {
    @ModifyArg(method = {"mouseReleased", "removeAllClaims"},
            at = @At(value = "INVOKE",
                    target = "Ldev/ftb/mods/ftbchunks/net/RequestChunkChangePacket;<init>("
                            + "Ldev/ftb/mods/ftbchunks/api/event/ChunkChangeEvent$Operation;"
                            + "Ljava/util/Set;ZLjava/util/Optional;)V"),
            index = 1)
    private Set<XZ> toroidal$foldRequestedChunks(Set<XZ> chunks) {
        return FtbChunksFold.foldedChunks(chunks);
    }
}

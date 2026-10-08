package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.entity.EntitySectionStorage;

@Mixin(EntitySectionStorage.class)
public class EntitySectionStorageMixin implements LevelBindable {
    @Unique
    private @Nullable ServerLevel toroidal$level;

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$level = level;
    }

    @ModifyVariable(method = {"getOrCreateSection", "getSection", "remove"}, at = @At("HEAD"), argsOnly = true)
    private long toroidal$physicalSection(long sectionKey) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.toroidal$level);
        return transformer == null ? sectionKey : transformer.foldSectionNode(sectionKey);
    }

    @ModifyVariable(
            method = {"getExistingSectionPositionsInChunk", "getExistingSectionsInChunk"},
            at = @At("HEAD"),
            argsOnly = true)
    private long toroidal$physicalChunk(long chunkKey) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.toroidal$level);
        return transformer == null ? chunkKey : transformer.foldChunkKey(chunkKey);
    }
}

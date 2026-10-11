package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.level.BoundLevelFold;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.entity.EntitySectionStorage;

@Mixin(EntitySectionStorage.class)
public class EntitySectionStorageMixin implements LevelBindable {
    @Unique
    private final BoundLevelFold toroidal$bound = new BoundLevelFold();

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$bound.bind(level);
    }

    @ModifyVariable(method = {"getOrCreateSection", "getSection", "remove"}, at = @At("HEAD"), argsOnly = true)
    private long toroidal$physicalSection(long sectionKey) {
        WorldFold transformer = this.toroidal$bound.wrapped();
        return transformer == null ? sectionKey : transformer.foldSectionNode(sectionKey);
    }

    @ModifyVariable(
            method = {"getExistingSectionPositionsInChunk", "getExistingSectionsInChunk"},
            at = @At("HEAD"),
            argsOnly = true)
    private long toroidal$physicalChunk(long chunkKey) {
        WorldFold transformer = this.toroidal$bound.wrapped();
        return transformer == null ? chunkKey : transformer.foldChunkKey(chunkKey);
    }
}

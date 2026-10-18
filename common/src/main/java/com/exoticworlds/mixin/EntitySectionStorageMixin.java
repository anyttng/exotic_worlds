package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.engine.level.BoundLevelFold;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.entity.EntitySectionStorage;

@Mixin(EntitySectionStorage.class)
public class EntitySectionStorageMixin implements LevelBindable {
    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Override
    public void exotic_worlds$bindLevel(ServerLevel level) {
        this.exotic_worlds$bound.bind(level);
    }

    @ModifyVariable(method = {"getOrCreateSection", "getSection", "remove"}, at = @At("HEAD"), argsOnly = true)
    private long exotic_worlds$physicalSection(long sectionKey) {
        return this.exotic_worlds$bound.fold().foldSectionNode(sectionKey);
    }

    @ModifyVariable(
            method = {"getExistingSectionPositionsInChunk", "getExistingSectionsInChunk"},
            at = @At("HEAD"),
            argsOnly = true)
    private long exotic_worlds$physicalChunk(long chunkKey) {
        return this.exotic_worlds$bound.fold().foldChunkKey(chunkKey);
    }
}

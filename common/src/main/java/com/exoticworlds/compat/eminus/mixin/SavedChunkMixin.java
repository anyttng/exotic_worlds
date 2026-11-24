package com.exoticworlds.compat.eminus.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.compat.eminus.SavedChunkOwner;

import net.minecraft.world.level.LevelReader;

@Pseudo
@Mixin(targets = "com.eminus.ingest.saved.SavedChunk", remap = false)
public class SavedChunkMixin implements SavedChunkOwner {
    @Unique
    private @Nullable LevelReader exotic_worlds$owner;

    @Override
    public @Nullable LevelReader exotic_worlds$getOwner() {
        return exotic_worlds$owner;
    }

    @Override
    public void exotic_worlds$setOwner(@Nullable LevelReader owner) {
        exotic_worlds$owner = owner;
    }
}

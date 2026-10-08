package com.exoticworlds.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.storage.SectionStorage;

@Mixin(SectionStorage.class)
public interface SectionStorageAccessor {
    @Accessor("levelHeightAccessor")
    LevelHeightAccessor toroidal$getLevelHeightAccessor();

    @Invoker("getOrLoad")
    Optional<?> toroidal$getOrLoad(long sectionPos);
}

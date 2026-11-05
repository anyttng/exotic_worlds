package com.exoticworlds.mixin;

import java.util.Map;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
    @Accessor("executor")
    Executor exotic_worlds$executor();

    @Accessor("storageSource")
    LevelStorageSource.LevelStorageAccess exotic_worlds$storageSource();

    @Accessor("levels")
    Map<ResourceKey<Level>, ServerLevel> exotic_worlds$levels();
}

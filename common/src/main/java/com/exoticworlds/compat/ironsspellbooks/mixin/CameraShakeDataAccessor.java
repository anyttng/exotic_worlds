package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import io.redspace.ironsspellbooks.api.util.CameraShakeData;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = CameraShakeData.class, remap = false)
public interface CameraShakeDataAccessor {
    @Invoker("<init>")
    static CameraShakeData exotic_worlds$create(ResourceKey<Level> dimension, int id, int duration, Vec3 origin,
            float radius) {
        throw new AssertionError();
    }

    @Accessor("duration")
    int exotic_worlds$getDuration();

    @Accessor("radius")
    float exotic_worlds$getRadius();

    @Accessor("tickCount")
    int exotic_worlds$getTickCount();

    @Accessor("tickCount")
    void exotic_worlds$setTickCount(int tickCount);

    @Accessor("id")
    int exotic_worlds$getId();

    @Accessor("origin")
    Vec3 exotic_worlds$getOrigin();

    @Accessor("dimension")
    ResourceKey<Level> exotic_worlds$getDimension();
}

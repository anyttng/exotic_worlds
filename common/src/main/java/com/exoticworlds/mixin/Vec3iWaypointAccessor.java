package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.Vec3i;

@Mixin(targets = "net.minecraft.world.waypoints.TrackedWaypoint$Vec3iWaypoint")
public interface Vec3iWaypointAccessor {
    @Accessor("vector")
    Vec3i exotic_worlds$getVector();

    @Accessor("vector")
    void exotic_worlds$setVector(Vec3i vector);
}

package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.waypoints.TrackedWaypoint;

@Mixin(TrackedWaypoint.class)
public interface TrackedWaypointAccessor {
    @Invoker("read")
    static TrackedWaypoint exotic_worlds$read(ByteBuf buf) {
        throw new AssertionError();
    }
}

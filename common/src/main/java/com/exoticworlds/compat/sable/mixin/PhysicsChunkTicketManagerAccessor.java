package com.exoticworlds.compat.sable.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicket;
import dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicketManager;

import net.minecraft.core.SectionPos;

@Mixin(value = PhysicsChunkTicketManager.class, remap = false)
public interface PhysicsChunkTicketManagerAccessor {
    @Accessor("physicsChunks")
    Map<SectionPos, PhysicsChunkTicket> exotic_worlds$physicsChunks();
}

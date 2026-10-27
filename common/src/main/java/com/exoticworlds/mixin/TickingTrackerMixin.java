package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.accessors.FoldCache;

import net.minecraft.server.level.TickingTracker;

@Mixin(TickingTracker.class)
public class TickingTrackerMixin {
    @ModifyVariable(method = "addTicket(JLnet/minecraft/server/level/Ticket;)V", at = @At("HEAD"), argsOnly = true)
    private long exotic_worlds$foldAddedTicketKey(long key) {
        return this.exotic_worlds$foldTicketKey(key);
    }

    @ModifyVariable(method = "removeTicket(JLnet/minecraft/server/level/Ticket;)V", at = @At("HEAD"), argsOnly = true)
    private long exotic_worlds$foldRemovedTicketKey(long key) {
        return this.exotic_worlds$foldTicketKey(key);
    }

    @Unique
    private long exotic_worlds$foldTicketKey(long key) {
        return ((FoldCache) (Object) this).exotic_worlds$fold().foldChunkKey(key);
    }
}

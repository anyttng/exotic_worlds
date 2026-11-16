package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.engine.level.BoundLevelFold;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.TicketStorage;

@Mixin(TicketStorage.class)
public class TicketStorageMixin implements LevelBindable {
    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Override
    public void exotic_worlds$bindLevel(ServerLevel level) {
        this.exotic_worlds$bound.bind(level);
    }

    @ModifyVariable(
            method = {"addTicket(JLnet/minecraft/server/level/Ticket;)Z",
                    "removeTicket(JLnet/minecraft/server/level/Ticket;)Z"},
            at = @At("HEAD"), argsOnly = true, require = 2)
    private long exotic_worlds$foldTicketKey(long key) {
        return this.exotic_worlds$bound.fold().foldChunkKey(key);
    }
}

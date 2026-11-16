package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.engine.level.BoundLevelFold;

import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TickingTracker;

@Mixin(DistanceManager.class)
public class DistanceManagerMixin implements LevelBindable {
    @Shadow
    @Final
    private DistanceManager.ChunkTicketTracker ticketTracker;

    @Shadow
    @Final
    private TickingTracker tickingTicketsTracker;

    @Shadow
    @Final
    private DistanceManager.FixedPlayerDistanceChunkTracker naturalSpawnChunkCounter;

    @Shadow
    @Final
    private DistanceManager.PlayerTicketTracker playerTicketManager;

    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Override
    public void exotic_worlds$bindLevel(ServerLevel level) {
        this.exotic_worlds$bound.bind(level);
        ((LevelBindable) this.ticketTracker).exotic_worlds$bindLevel(level);
        ((LevelBindable) this.tickingTicketsTracker).exotic_worlds$bindLevel(level);
        ((LevelBindable) this.naturalSpawnChunkCounter).exotic_worlds$bindLevel(level);
        ((LevelBindable) this.playerTicketManager).exotic_worlds$bindLevel(level);
    }

    @ModifyVariable(
            method = {"addTicket(JLnet/minecraft/server/level/Ticket;)V",
                    "removeTicket(JLnet/minecraft/server/level/Ticket;)V"},
            at = @At("HEAD"), argsOnly = true, require = 2)
    private long exotic_worlds$foldTicketKey(long key) {
        return this.exotic_worlds$bound.fold().foldChunkKey(key);
    }

    @ModifyVariable(method = {"inEntityTickingRange", "inBlockTickingRange"}, at = @At("HEAD"), argsOnly = true,
            require = 2)
    private long exotic_worlds$tickingOnPhysicalChunk(long chunkKey) {
        return this.exotic_worlds$bound.fold().foldChunkKey(chunkKey);
    }
}

package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.accessors.LevelBindRegistry;
import com.exoticworlds.engine.level.BoundLevelFold;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.TicketStorage;

@Mixin(TicketStorage.class)
public class TicketStorageMixin implements LevelBindable, LevelBindRegistry {
    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Unique
    private final List<LevelBindable> exotic_worlds$registeredBindables = new ArrayList<>();

    @Override
    public void exotic_worlds$registerBindable(LevelBindable bindable) {
        this.exotic_worlds$registeredBindables.add(bindable);
    }

    @Override
    public void exotic_worlds$bindLevel(ServerLevel level) {
        this.exotic_worlds$bound.bind(level);
        for (LevelBindable bindable : this.exotic_worlds$registeredBindables) {
            bindable.exotic_worlds$bindLevel(level);
        }
    }

    @ModifyVariable(method = "addTicket(JLnet/minecraft/server/level/Ticket;)Z", at = @At("HEAD"), argsOnly = true)
    private long exotic_worlds$foldAddedTicketKey(long key) {
        return this.exotic_worlds$foldKey(key);
    }

    @ModifyVariable(method = "removeTicket(JLnet/minecraft/server/level/Ticket;)Z", at = @At("HEAD"), argsOnly = true)
    private long exotic_worlds$foldRemovedTicketKey(long key) {
        return this.exotic_worlds$foldKey(key);
    }

    @Unique
    private long exotic_worlds$foldKey(long key) {
        return this.exotic_worlds$bound.fold().foldChunkKey(key);
    }
}

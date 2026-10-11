package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.accessors.LevelBindRegistry;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.level.BoundLevelFold;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.TicketStorage;

@Mixin(TicketStorage.class)
public class TicketStorageMixin implements LevelBindable, LevelBindRegistry {
    @Unique
    private final BoundLevelFold toroidal$bound = new BoundLevelFold();

    @Unique
    private final List<LevelBindable> toroidal$registeredBindables = new ArrayList<>();

    @Override
    public void toroidal$registerBindable(LevelBindable bindable) {
        this.toroidal$registeredBindables.add(bindable);
    }

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$bound.bind(level);
        for (LevelBindable bindable : this.toroidal$registeredBindables) {
            bindable.toroidal$bindLevel(level);
        }
    }

    @ModifyVariable(method = "addTicket(JLnet/minecraft/server/level/Ticket;)Z", at = @At("HEAD"), argsOnly = true)
    private long toroidal$foldAddedTicketKey(long key) {
        return this.toroidal$foldKey(key);
    }

    @ModifyVariable(method = "removeTicket(JLnet/minecraft/server/level/Ticket;)Z", at = @At("HEAD"), argsOnly = true)
    private long toroidal$foldRemovedTicketKey(long key) {
        return this.toroidal$foldKey(key);
    }

    @Unique
    private long toroidal$foldKey(long key) {
        WorldFold transformer = this.toroidal$bound.wrapped();
        return transformer == null ? key : transformer.foldChunkKey(key);
    }
}

package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.ClientBoundsHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

import net.minecraft.client.multiplayer.ClientLevel;

@Mixin(ClientLevel.class)
public class ClientLevelMixin implements ClientBoundsHolder {
    @Unique
    private WorldFold exotic_worlds$clientBounds = WorldFolds.NOOP;

    @Override
    public WorldFold exotic_worlds$clientBounds() {
        return this.exotic_worlds$clientBounds;
    }

    @Override
    public void exotic_worlds$setClientBounds(WorldFold fold) {
        this.exotic_worlds$clientBounds = fold;
    }
}

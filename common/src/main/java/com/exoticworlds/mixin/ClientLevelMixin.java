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
    private WorldFold toroidal$clientBounds = WorldFolds.NOOP;

    @Override
    public WorldFold toroidal$clientBounds() {
        return this.toroidal$clientBounds;
    }

    @Override
    public void toroidal$setClientBounds(WorldFold transformer) {
        this.toroidal$clientBounds = transformer;
    }
}

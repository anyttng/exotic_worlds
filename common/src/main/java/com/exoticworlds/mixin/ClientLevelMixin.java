package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.ClientBoundsHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

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

    @WrapMethod(method = "getPrecipitationAt")
    private Biome.Precipitation exotic_worlds$bindPrecipitationFold(
            BlockPos pos, Operation<Biome.Precipitation> original) {
        return GenerationFoldContext.withFold(
                WorldLoopAttachments.noiseFoldOf((Level) (Object) this), () -> original.call(pos));
    }
}

package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.ClientBoundsHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

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

    @WrapMethod(method = "getPrecipitationAt")
    private Biome.Precipitation toroidal$bindPrecipitationTransformer(
            BlockPos pos, Operation<Biome.Precipitation> original) {
        return GenerationTransformerContext.withTransformer(
                WorldLoopAttachments.noiseTransformerOf((Level) (Object) this), () -> original.call(pos));
    }
}

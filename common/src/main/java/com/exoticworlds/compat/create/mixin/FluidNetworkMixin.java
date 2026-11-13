package com.exoticworlds.compat.create.mixin;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.zurrtum.create.catnip.math.BlockFace;
import com.zurrtum.create.content.fluids.FluidNetwork;
import com.zurrtum.create.content.fluids.FluidTransportBehaviour;
import com.exoticworlds.compat.create.CanonicalPositionKeys;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(FluidNetwork.class)
public class FluidNetworkMixin {
    @Shadow
    Set<BlockPos> visited;

    @Shadow
    Map<BlockPos, WeakReference<FluidTransportBehaviour>> cache;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$canonicalizeKeys(Level world, BlockFace location, Supplier<?> sourceSupplier,
            CallbackInfo ci) {
        this.visited = CanonicalPositionKeys.set(world, this.visited);
        this.cache = CanonicalPositionKeys.map(world, this.cache);
    }
}

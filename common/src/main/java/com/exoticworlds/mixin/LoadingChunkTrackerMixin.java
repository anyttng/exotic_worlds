package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.accessors.LevelBindRegistry;

import net.minecraft.server.level.DistanceManager;
import net.minecraft.world.level.TicketStorage;

@Mixin(targets = "net.minecraft.server.level.LoadingChunkTracker")
public class LoadingChunkTrackerMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$registerForBind(DistanceManager distanceManager, TicketStorage ticketStorage,
            CallbackInfo ci) {
        ((LevelBindRegistry) ticketStorage).toroidal$registerBindable((LevelBindable) (Object) this);
    }
}

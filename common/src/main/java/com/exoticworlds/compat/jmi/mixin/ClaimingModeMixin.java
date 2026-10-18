package com.exoticworlds.compat.jmi.mixin;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.jmi.JmiFold;
import com.exoticworlds.compat.journeymap.SingleCopyOverlay;

import net.minecraft.world.level.ChunkPos;

import journeymap.api.v2.client.display.Displayable;
import journeymap.api.v2.client.model.MapPolygonWithHoles;

@Mixin(targets = "me.frankv.jmi.compat.ftbchunks.claimingmode.ClaimingMode", remap = false)
public abstract class ClaimingModeMixin {
    @Unique
    private final Set<ChunkPos> exotic_worlds$outline = new HashSet<>();

    @Inject(method = "createClaimingAreaOverlays", at = @At("HEAD"))
    private void exotic_worlds$clearOutline(CallbackInfo ci) {
        exotic_worlds$outline.clear();
    }

    @WrapOperation(method = "createClaimingAreaOverlays",
            at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private boolean exotic_worlds$foldWindowChunk(Set<ChunkPos> area, Object chunk, Operation<Boolean> original) {
        ChunkPos folded = JmiFold.foldChunk((ChunkPos) chunk);
        exotic_worlds$outline.add(JmiFold.seatNearPlayer(folded));
        return original.call(area, folded);
    }

    @WrapOperation(method = "createClaimingAreaOverlays",
            at = @At(value = "INVOKE",
                    target = "Ljourneymap/api/v2/client/util/PolygonHelper;createChunksPolygon(Ljava/util/Collection;I)"
                            + "Ljava/util/List;"))
    private List<MapPolygonWithHoles> exotic_worlds$outlineTheWholeWindow(Collection<ChunkPos> area, int y,
            Operation<List<MapPolygonWithHoles>> original) {
        return original.call(exotic_worlds$outline, y);
    }

    @WrapOperation(method = "createClaimingAreaOverlays",
            at = @At(value = "INVOKE",
                    target = "Lme/frankv/jmi/util/OverlayHelper;showOverlay(Ljourneymap/api/v2/client/display/Displayable;)V"))
    private void exotic_worlds$showTheFrameOnce(Displayable frame, Operation<Void> original) {
        ((SingleCopyOverlay) frame).exotic_worlds$drawOnce();
        original.call(frame);
    }
}

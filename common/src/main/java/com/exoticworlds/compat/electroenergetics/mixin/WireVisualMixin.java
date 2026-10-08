package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.george_vi.electroenergetics.client.WireVisual;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.exoticworlds.compat.electroenergetics.WireCopies;
import com.exoticworlds.compat.electroenergetics.WireCopyHolder;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

@Mixin(value = WireVisual.class, remap = false)
public abstract class WireVisualMixin implements WireCopyHolder {
    @Unique
    private int toroidal$end = WireCopies.FIRST_END;

    @Override
    public int toroidal$end() {
        return this.toroidal$end;
    }

    @Override
    public void toroidal$setEnd(int end) {
        this.toroidal$end = end;
    }

    @ModifyExpressionValue(method = "recreateInstances", at = @At(value = "INVOKE",
            target = "Lcom/george_vi/electroenergetics/foundation/nodes/InWorldNodeConnection;"
                    + "isFullyLoaded(Lnet/minecraft/world/level/Level;)Z"))
    private boolean toroidal$secondEndOnlyWhenParted(boolean loaded, @Local(argsOnly = true) ClientLevel level,
            @Local(name = "pos1") Vec3 pos1, @Local(name = "pos2") Vec3 pos2) {
        return loaded && (this.toroidal$end == WireCopies.FIRST_END
                || WireCopies.parted(WorldLoopAttachments.transformerOfReader(level), pos1, pos2));
    }

    @Inject(method = "recreateInstances", at = @At(value = "INVOKE",
            target = "Ldev/engine_room/flywheel/api/visualization/VisualizationContext;renderOrigin()"
                    + "Lnet/minecraft/core/Vec3i;"))
    private void toroidal$seatOnItsEnd(ClientLevel level, float partialTick, boolean light, CallbackInfo ci,
            @Local(name = "pos1") LocalRef<Vec3> pos1, @Local(name = "pos2") LocalRef<Vec3> pos2) {
        WireCopies.Span span = WireCopies.from(WorldLoopAttachments.transformerOfReader(level), pos1.get(),
                pos2.get(), this.toroidal$end);
        pos1.set(span.pos1());
        pos2.set(span.pos2());
    }
}

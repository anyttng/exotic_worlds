package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.george_vi.electroenergetics.client.WireEffect;
import com.george_vi.electroenergetics.content.railway_electrification.catenary.CatenaryConnection;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.exoticworlds.compat.electroenergetics.CatenaryCopies;
import com.exoticworlds.compat.electroenergetics.WireCopies;
import com.exoticworlds.compat.electroenergetics.WireCopyHolder;

import dev.engine_room.flywheel.api.visual.EffectVisual;
import net.minecraft.world.level.LevelAccessor;

@Mixin(value = WireEffect.class, remap = false)
public abstract class WireEffectMixin implements WireCopyHolder {
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

    @Shadow
    public abstract LevelAccessor level();

    @ModifyArg(method = "visualize", index = 1, at = @At(value = "INVOKE",
            target = "Lcom/george_vi/electroenergetics/client/CatenaryVisual;<init>(Ldev/engine_room/flywheel/api/"
                    + "visualization/VisualizationContext;Lcom/george_vi/electroenergetics/content/"
                    + "railway_electrification/catenary/CatenaryConnection;Lcom/george_vi/electroenergetics/simulation/"
                    + "WireType;)V"))
    private CatenaryConnection toroidal$catenaryFromItsEnd(CatenaryConnection line) {
        return CatenaryCopies.from(this.level(), line, this.toroidal$end);
    }

    @ModifyReturnValue(method = "visualize", at = @At("RETURN"))
    private EffectVisual<?> toroidal$visualFromItsEnd(EffectVisual<?> visual) {
        if (visual instanceof WireCopyHolder holder) {
            holder.toroidal$setEnd(this.toroidal$end);
        }

        return visual;
    }
}

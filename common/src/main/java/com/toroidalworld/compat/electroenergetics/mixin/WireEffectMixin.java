package com.toroidalworld.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.client.WireEffect;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.toroidalworld.compat.electroenergetics.WireCopies;
import com.toroidalworld.compat.electroenergetics.WireCopyHolder;

import dev.engine_room.flywheel.api.visual.EffectVisual;

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

    @ModifyReturnValue(method = "visualize", at = @At("RETURN"))
    private EffectVisual<?> toroidal$visualFromItsEnd(EffectVisual<?> visual) {
        if (visual instanceof WireCopyHolder holder) {
            holder.toroidal$setEnd(this.toroidal$end);
        }

        return visual;
    }
}

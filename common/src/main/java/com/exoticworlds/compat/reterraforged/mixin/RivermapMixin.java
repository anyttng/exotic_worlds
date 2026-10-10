package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.reterraforged.RtfLap;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.rivermap.Rivermap;

// A continent's rivers are one network around its canonical centre: a point is carved on the copy nearest that
// centre, where every copy of the point lands, so the network needs no lap of its own and runs on the open lattice.
@Mixin(value = Rivermap.class, remap = false)
public abstract class RivermapMixin {
    @Shadow
    private int x;

    @Shadow
    private int z;

    @WrapMethod(method = "apply")
    private void toroidal$seatOnCenter(Cell cell, float x, float z, Operation<Void> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(cell, x, z);
            return;
        }

        float seatedX = (float) frame.seatX(x, z, this.x, this.z);
        float seatedZ = (float) frame.seatZ(z, this.z);
        try (RtfLap.Frame.Scope open = frame.open()) {
            original.call(cell, seatedX, seatedZ);
        }
    }
}

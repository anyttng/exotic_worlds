package com.exoticworlds.compat.ultraterraforged.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.terraforged.RtfLap;
import com.exoticworlds.compat.ultraterraforged.LapCarrier;
import com.exoticworlds.core.WorldFold;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.Rivermap;

// A point is carved and warped on the copy nearest the network's canonical center, where every copy of the point lands.
@Mixin(value = Rivermap.class, remap = false)
public abstract class RivermapMixin {
    @Shadow
    private int x;

    @Shadow
    private int z;

    @WrapMethod(method = "get(IILcom/pandaismyname1/ultraterraforged/world/worldgen/rivermap/Rivermap;"
            + "Lcom/pandaismyname1/ultraterraforged/world/worldgen/heightmap/Heightmap;)"
            + "Lcom/pandaismyname1/ultraterraforged/world/worldgen/rivermap/Rivermap;")
    private static Rivermap exotic_worlds$bindNetwork(int x, int z, @Nullable Rivermap instance, Heightmap heightmap,
            Operation<Rivermap> original) {
        WorldFold fold = LapCarrier.foldOf(heightmap.levels());
        if (fold == null) {
            return original.call(x, z, instance, heightmap);
        }

        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold)) {
            return original.call(x, z, instance, heightmap);
        }
    }

    @WrapMethod(method = "apply")
    private void exotic_worlds$seatOnCenter(Cell cell, float x, float z, Operation<Void> original) {
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

    @WrapMethod(method = "riverX")
    private float exotic_worlds$warpXOnCenter(float x, float z, Operation<Float> original) {
        return exotic_worlds$warpOnCenter(x, z, original);
    }

    @WrapMethod(method = "riverZ")
    private float exotic_worlds$warpZOnCenter(float x, float z, Operation<Float> original) {
        return exotic_worlds$warpOnCenter(x, z, original);
    }

    @Unique
    private float exotic_worlds$warpOnCenter(float x, float z, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(x, z);
        }

        float seatedX = (float) frame.seatX(x, z, this.x, this.z);
        float seatedZ = (float) frame.seatZ(z, this.z);
        try (RtfLap.Frame.Scope open = frame.open()) {
            return original.call(seatedX, seatedZ);
        }
    }
}

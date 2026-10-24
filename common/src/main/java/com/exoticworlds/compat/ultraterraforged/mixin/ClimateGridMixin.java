package com.exoticworlds.compat.ultraterraforged.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.terraforged.SiteLattice;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.ClimateGrid;

@Mixin(value = ClimateGrid.class, remap = false)
public abstract class ClimateGridMixin {
    @Unique
    private static final String SPACING =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/landform/ClimateGrid;spacing:F";

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Shadow
    @Final
    private float spacing;

    @Shadow
    private float corner(int gridX, int gridZ, Heightmap heightmap) {
        throw new AssertionError();
    }

    @WrapMethod(method = "get")
    private float exotic_worlds$closeOnLap(float x, float z, Heightmap heightmap, Operation<Float> original) {
        SiteLattice lattice = SiteLattice.bound(this.spacing);
        if (lattice == null) {
            return original.call(x, z, heightmap);
        }

        return lattice.interpolate(x, z, (cellX, cellZ) -> corner(cellX, cellZ, heightmap));
    }

    @ModifyExpressionValue(method = "corner",
            at = @At(value = "FIELD", target = SPACING, opcode = Opcodes.GETFIELD, ordinal = X_READ))
    private float exotic_worlds$snapX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyExpressionValue(method = "corner",
            at = @At(value = "FIELD", target = SPACING, opcode = Opcodes.GETFIELD, ordinal = Z_READ))
    private float exotic_worlds$snapZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }
}

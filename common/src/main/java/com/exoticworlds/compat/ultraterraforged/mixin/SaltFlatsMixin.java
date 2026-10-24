package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.terraforged.SiteLattice;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.SaltFlats;

@Mixin(value = SaltFlats.class, remap = false)
public abstract class SaltFlatsMixin {
    @Unique
    private static final float GRID = 440.0F;

    @Unique
    private static final float BLEND = 36.0F;

    @Unique
    private static final float MARGIN = 174.0F;

    @Unique
    private static final float FREE = 92.0F;

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @ModifyConstant(method = "apply", constant = @Constant(floatValue = GRID, ordinal = X_READ))
    private float exotic_worlds$cellX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "apply", constant = @Constant(floatValue = GRID, ordinal = Z_READ))
    private float exotic_worlds$cellZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyConstant(method = "apply", constant = @Constant(floatValue = BLEND))
    private float exotic_worlds$blend(float blend) {
        return SiteLattice.scaledOn(blend, GRID);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = FREE))
    private float exotic_worlds$free(float free) {
        return SiteLattice.scaledOn(free, GRID);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = MARGIN))
    private float exotic_worlds$margin(float margin) {
        return SiteLattice.scaledOn(margin, GRID);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = X_READ))
    private float exotic_worlds$originX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = Z_READ))
    private float exotic_worlds$originZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyVariable(method = "find", at = @At("STORE"), name = "radius")
    private float exotic_worlds$radius(float radius) {
        return SiteLattice.scaledOn(radius, GRID);
    }
}

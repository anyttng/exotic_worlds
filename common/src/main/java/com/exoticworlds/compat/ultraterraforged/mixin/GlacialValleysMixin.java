package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.terraforged.SiteLattice;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.GlacialValleys;

@Mixin(value = GlacialValleys.class, remap = false)
public abstract class GlacialValleysMixin {
    @Unique
    private static final float CIRQUE_GRID = 176.0F;

    @Unique
    private static final float CIRQUE_MARGIN = 42.0F;

    @Unique
    private static final int X_READ = 0;

    @Unique
    private static final int Z_READ = 1;

    @Unique
    private static final int FREE_READ = 0;

    @Unique
    private static final int ORIGIN_X_READ = 1;

    @Unique
    private static final int ORIGIN_Z_READ = 2;

    @ModifyConstant(method = "cirque", constant = @Constant(floatValue = CIRQUE_GRID, ordinal = X_READ))
    private float exotic_worlds$cirqueCellX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "cirque", constant = @Constant(floatValue = CIRQUE_GRID, ordinal = Z_READ))
    private float exotic_worlds$cirqueCellZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyConstant(method = "findCirque", constant = @Constant(floatValue = CIRQUE_MARGIN))
    private float exotic_worlds$cirqueMargin(float margin) {
        return SiteLattice.scaledOn(margin, CIRQUE_GRID);
    }

    @ModifyConstant(method = "findCirque", constant = @Constant(floatValue = CIRQUE_GRID, ordinal = FREE_READ))
    private float exotic_worlds$cirqueFree(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @ModifyConstant(method = "findCirque", constant = @Constant(floatValue = CIRQUE_GRID, ordinal = ORIGIN_X_READ))
    private float exotic_worlds$cirqueOriginX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "findCirque", constant = @Constant(floatValue = CIRQUE_GRID, ordinal = ORIGIN_Z_READ))
    private float exotic_worlds$cirqueOriginZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyVariable(method = "findCirque", at = @At("STORE"), name = "radius")
    private float exotic_worlds$cirqueRadius(float radius) {
        return SiteLattice.scaledOn(radius, CIRQUE_GRID);
    }
}

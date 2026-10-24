package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.terraforged.SiteLattice;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.Tors;

@Mixin(value = Tors.class, remap = false)
public abstract class TorsMixin {
    @Unique
    private static final float GRID = 128.0F;

    @Unique
    private static final float MARGIN = 48.0F;

    @Unique
    private static final float FREE = 32.0F;

    @Unique
    private static final float SEARCH_RADIUS = 16.0F;

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

    @ModifyVariable(method = "apply", at = @At("STORE"), name = "mainRadius")
    private float exotic_worlds$mainRadius(float radius) {
        return SiteLattice.scaledOn(radius, GRID);
    }

    @ModifyConstant(method = "findSite", constant = @Constant(floatValue = FREE))
    private float exotic_worlds$free(float free) {
        return SiteLattice.scaledOn(free, GRID);
    }

    @ModifyConstant(method = "findSite", constant = @Constant(floatValue = MARGIN))
    private float exotic_worlds$margin(float margin) {
        return SiteLattice.scaledOn(margin, GRID);
    }

    @ModifyConstant(method = "findSite", constant = @Constant(floatValue = SEARCH_RADIUS))
    private float exotic_worlds$search(float search) {
        return SiteLattice.scaledOn(search, GRID);
    }

    @ModifyConstant(method = "findSite", constant = @Constant(floatValue = GRID, ordinal = X_READ))
    private float exotic_worlds$originX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "findSite", constant = @Constant(floatValue = GRID, ordinal = Z_READ))
    private float exotic_worlds$originZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }
}

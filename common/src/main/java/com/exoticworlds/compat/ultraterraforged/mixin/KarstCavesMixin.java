package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.terraforged.SiteLattice;
import com.exoticworlds.compat.ultraterraforged.CaveCells;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.cave.CaveCarving;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.SiteCache;

@Mixin(targets = "com.pandaismyname1.ultraterraforged.world.worldgen.cave.KarstCaves", remap = false)
public abstract class KarstCavesMixin {
    @Unique
    private static final float GRID = 80.0F;

    @Unique
    private static final String CHAMBER = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/cave/KarstCaves;"
            + "chamber(IILcom/pandaismyname1/ultraterraforged/world/worldgen/cave/CaveCarving;)"
            + "Lcom/pandaismyname1/ultraterraforged/world/worldgen/cave/KarstCaves$Chamber;";

    @Unique
    private static final String CHAMBER_X =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/cave/KarstCaves$Chamber;x()F";

    @Unique
    private static final String CHAMBER_Z =
            "Lcom/pandaismyname1/ultraterraforged/world/worldgen/cave/KarstCaves$Chamber;z()F";

    @Unique
    private static final int ORIGIN_X_READ = 0;

    @Unique
    private static final int FREE_X_READ = 1;

    @Unique
    private static final int ORIGIN_Z_READ = 2;

    @Unique
    private static final int FREE_Z_READ = 3;

    @Unique
    private static final int BASE_CHAMBER = 0;

    @Unique
    private static final int NEXT_CHAMBER = 1;

    @Unique
    private static final int CELL_X = 0;

    @Unique
    private static final String SITE = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/cave/KarstCaves$Chamber;"
            + "<init>(FFFFFIZZ)V";

    @Unique
    private static final int SITE_X = 0;

    @Unique
    private static final int SITE_Z = 2;

    @ModifyVariable(method = "carve", at = @At("STORE"), name = "gridX")
    private int exotic_worlds$pointX(int pointX, @Local(argsOnly = true) CaveCarving carving) {
        return CaveCells.pointX(GRID, carving.minX, pointX);
    }

    @ModifyVariable(method = "carve", at = @At("STORE"), name = "gridZ")
    private int exotic_worlds$pointZ(int pointZ, @Local(argsOnly = true) CaveCarving carving) {
        return CaveCells.pointZ(GRID, carving.minZ, pointZ);
    }

    @ModifyArg(method = "carve", at = @At(value = "INVOKE", target = CHAMBER, ordinal = BASE_CHAMBER), index = CELL_X)
    private int exotic_worlds$baseCopy(int cellX, @Local(name = "gz") int cellZ,
            @Local(argsOnly = true) CaveCarving carving, @Local(name = "gridX") int pointX) {
        int copyX = CaveCells.copyX(GRID, cellX, cellZ, carving, pointX);
        CaveCells.enter(GRID, carving, copyX, cellZ);
        return copyX;
    }

    @ModifyExpressionValue(method = "carve", at = @At(value = "INVOKE", target = CHAMBER_X))
    private float exotic_worlds$frameX(float x, @Local(argsOnly = true) CaveCarving carving) {
        return CaveCells.frameX(carving, x);
    }

    @ModifyExpressionValue(method = "carve", at = @At(value = "INVOKE", target = CHAMBER_Z))
    private float exotic_worlds$frameZ(float z, @Local(argsOnly = true) CaveCarving carving) {
        return CaveCells.frameZ(carving, z);
    }

    @Inject(method = "carve", at = @At("RETURN"))
    private void exotic_worlds$leave(CaveCarving carving, CallbackInfo ci) {
        CaveCells.leave(carving);
    }

    @ModifyArg(method = "carve", at = @At(value = "INVOKE", target = CHAMBER, ordinal = NEXT_CHAMBER), index = CELL_X)
    private int exotic_worlds$nextCopy(int nextX, @Local(name = "gx") int cellX, @Local(name = "gz") int cellZ,
            @Local(argsOnly = true) CaveCarving carving, @Local(name = "gridX") int pointX) {
        return CaveCells.copyX(GRID, cellX, cellZ, carving, pointX) + nextX - cellX;
    }

    @WrapOperation(method = "chamber", at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.SITE_CACHE_GET))
    private Object exotic_worlds$site(SiteCache<?> cache, int copyX, int copyZ, SiteCache.Finder<?> finder,
            Operation<Object> original) {
        return original.call(cache, copyX, copyZ, CaveCells.seated(GRID, finder));
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = ORIGIN_X_READ))
    private float exotic_worlds$originX(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.X);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = FREE_X_READ))
    private float exotic_worlds$freeX(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = ORIGIN_Z_READ))
    private float exotic_worlds$originZ(float spacing) {
        return SiteLattice.spacingOn(spacing, Direction.Axis.Z);
    }

    @ModifyConstant(method = "find", constant = @Constant(floatValue = GRID, ordinal = FREE_Z_READ))
    private float exotic_worlds$freeZ(float spacing) {
        return SiteLattice.sharedOn(spacing);
    }

    @ModifyVariable(method = "find", at = @At("STORE"), name = "radius")
    private float exotic_worlds$radius(float radius) {
        return SiteLattice.scaledOn(radius, GRID);
    }

    @ModifyArg(method = "find", at = @At(value = "INVOKE", target = SITE), index = SITE_X)
    private float exotic_worlds$seatX(float x) {
        return CaveCells.seatX(x);
    }

    @ModifyArg(method = "find", at = @At(value = "INVOKE", target = SITE), index = SITE_Z)
    private float exotic_worlds$seatZ(float z) {
        return CaveCells.seatZ(z);
    }
}

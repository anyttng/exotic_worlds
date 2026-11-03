package com.exoticworlds.compat.ultraterraforged.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.exoticworlds.compat.ultraterraforged.DrumlinLattice;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;

import com.pandaismyname1.ultraterraforged.world.worldgen.landform.Drumlins;

@Mixin(value = Drumlins.class, remap = false)
public abstract class DrumlinsMixin {
    @Unique
    private static final int X_ARGUMENT = 0;

    @Unique
    private static final int Z_ARGUMENT = 1;

    @Unique
    private static final String DRUMLIN = "drumlin(FF)F";

    @Unique
    private static final String LATTICE = "drumlinLattice";

    @Shadow
    @Final
    private float cosFlow;

    @Shadow
    @Final
    private float sinFlow;

    @WrapOperation(method = "apply", at = @At(value = "INVOKE",
            target = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/landform/Drumlins;drumlin(FF)F"))
    private float exotic_worlds$drumlin(Drumlins drumlins, float u, float v, Operation<Float> original,
            @Local(argsOnly = true, ordinal = X_ARGUMENT) float x, @Local(argsOnly = true, ordinal = Z_ARGUMENT) float z) {
        DrumlinLattice lattice = DrumlinLattice.bound(this.cosFlow, this.sinFlow);
        return lattice == null
                ? original.call(drumlins, u, v)
                : original.call(drumlins, lattice.along(x, z), lattice.across(x, z));
    }

    @WrapMethod(method = DRUMLIN)
    private float exotic_worlds$bindLattice(float u, float v, Operation<Float> original,
            @Share(LATTICE) LocalRef<@Nullable DrumlinLattice> lattice) {
        lattice.set(DrumlinLattice.bound(this.cosFlow, this.sinFlow));
        return original.call(u, v);
    }

    @ModifyConstant(method = DRUMLIN, constant = @Constant(floatValue = DrumlinLattice.CELL_WIDTH))
    private float exotic_worlds$rowWidth(float width, @Share(LATTICE) LocalRef<@Nullable DrumlinLattice> lattice) {
        DrumlinLattice bound = lattice.get();
        return bound == null ? width : bound.rowWidth();
    }

    @ModifyConstant(method = DRUMLIN, constant = @Constant(floatValue = DrumlinLattice.CELL_LENGTH))
    private float exotic_worlds$cellLength(float length, @Share(LATTICE) LocalRef<@Nullable DrumlinLattice> lattice) {
        DrumlinLattice bound = lattice.get();
        return bound == null ? length : bound.cellLength();
    }

    @ModifyVariable(method = DRUMLIN, at = @At("STORE"), name = "shift")
    private float exotic_worlds$rowShift(float shift, @Local(name = "r") int row,
            @Share(LATTICE) LocalRef<@Nullable DrumlinLattice> lattice) {
        DrumlinLattice bound = lattice.get();
        return bound == null ? shift : row * bound.rowShift();
    }

    @WrapOperation(method = DRUMLIN,
            at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.LANDFORM_RANDOM))
    private float exotic_worlds$foldedCell(int seed, int column, int row, int purpose, Operation<Float> original,
            @Share(LATTICE) LocalRef<@Nullable DrumlinLattice> lattice) {
        DrumlinLattice bound = lattice.get();
        return bound == null
                ? original.call(seed, column, row, purpose)
                : original.call(seed, bound.foldColumn(column, row), bound.foldRow(row), purpose);
    }
}

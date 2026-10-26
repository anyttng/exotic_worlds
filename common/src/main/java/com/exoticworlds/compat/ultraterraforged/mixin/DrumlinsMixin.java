package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.ultraterraforged.DrumlinLattice;

import com.pandaismyname1.ultraterraforged.world.worldgen.landform.Drumlins;

@Mixin(value = Drumlins.class, remap = false)
public abstract class DrumlinsMixin {
    @Unique
    private static final int X_ARGUMENT = 0;

    @Unique
    private static final int Z_ARGUMENT = 1;

    @Shadow
    @Final
    private int seed;

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
        return lattice == null ? original.call(drumlins, u, v) : lattice.drumlin(this.seed, x, z);
    }
}

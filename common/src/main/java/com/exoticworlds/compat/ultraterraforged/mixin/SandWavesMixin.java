package com.exoticworlds.compat.ultraterraforged.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.exoticworlds.compat.terraforged.LapWave;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.SandWaves;

@Mixin(value = SandWaves.class, remap = false)
public abstract class SandWavesMixin {
    @Unique
    private static final float WAVELENGTH = 18.0F;

    @Shadow
    @Final
    private float cosCurrent;

    @Shadow
    @Final
    private float sinCurrent;

    @ModifyExpressionValue(method = "apply", at = @At(value = "FIELD",
            target = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/landform/SandWaves;cosCurrent:F",
            opcode = Opcodes.GETFIELD))
    private float exotic_worlds$currentX(float cos) {
        return LapWave.directionOn(Direction.Axis.X, cos, this.sinCurrent, WAVELENGTH);
    }

    @ModifyExpressionValue(method = "apply", at = @At(value = "FIELD",
            target = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/landform/SandWaves;sinCurrent:F",
            opcode = Opcodes.GETFIELD))
    private float exotic_worlds$currentZ(float sin) {
        return LapWave.directionOn(Direction.Axis.Z, this.cosCurrent, sin, WAVELENGTH);
    }
}

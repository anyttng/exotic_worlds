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
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.Dunes;

@Mixin(value = Dunes.class, remap = false)
public abstract class DunesMixin {
    @Unique
    private static final float WAVELENGTH = 56.0F;

    @Shadow
    @Final
    private float cosWind;

    @Shadow
    @Final
    private float sinWind;

    @ModifyExpressionValue(method = "apply", at = @At(value = "FIELD",
            target = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/landform/Dunes;cosWind:F",
            opcode = Opcodes.GETFIELD))
    private float exotic_worlds$windX(float cos) {
        return LapWave.directionOn(Direction.Axis.X, cos, this.sinWind, WAVELENGTH);
    }

    @ModifyExpressionValue(method = "apply", at = @At(value = "FIELD",
            target = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/landform/Dunes;sinWind:F",
            opcode = Opcodes.GETFIELD))
    private float exotic_worlds$windZ(float sin) {
        return LapWave.directionOn(Direction.Axis.Z, this.cosWind, sin, WAVELENGTH);
    }
}

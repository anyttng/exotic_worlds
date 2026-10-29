package com.exoticworlds.compat.ultraterraforged.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.exoticworlds.compat.ultraterraforged.CaveFrame;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;

import com.pandaismyname1.ultraterraforged.world.worldgen.cave.CaveCarving;

@Mixin(value = CaveCarving.class, remap = false)
public abstract class CaveCarvingMixin implements CaveFrame {
    @Unique
    private static final String POS_SET =
            "Lnet/minecraft/core/BlockPos$MutableBlockPos;set(III)Lnet/minecraft/core/BlockPos$MutableBlockPos;";

    @Unique
    private static final int POS_X = 0;

    @Unique
    private static final int POS_Z = 2;

    @Unique
    private int exotic_worlds$shiftX;

    @Unique
    private int exotic_worlds$shiftZ;

    @Override
    public int exotic_worlds$shiftX() {
        return this.exotic_worlds$shiftX;
    }

    @Override
    public int exotic_worlds$shiftZ() {
        return this.exotic_worlds$shiftZ;
    }

    @Override
    public void exotic_worlds$shift(int x, int z) {
        this.exotic_worlds$shiftX = x;
        this.exotic_worlds$shiftZ = z;
    }

    @ModifyExpressionValue(method = {"ground", "reaches", "hollow", "line", "contains"},
            at = @At(value = "FIELD", target = UltraTerraForgedInjectionTargets.CAVE_CARVING_MIN_X,
                    opcode = Opcodes.GETFIELD))
    private int exotic_worlds$frameMinX(int minX) {
        return minX - this.exotic_worlds$shiftX;
    }

    @ModifyExpressionValue(method = {"ground", "reaches", "hollow", "line", "contains"},
            at = @At(value = "FIELD", target = UltraTerraForgedInjectionTargets.CAVE_CARVING_MIN_Z,
                    opcode = Opcodes.GETFIELD))
    private int exotic_worlds$frameMinZ(int minZ) {
        return minZ - this.exotic_worlds$shiftZ;
    }

    @ModifyArg(method = {"clear", "get", "set", "line"},
            at = @At(value = "INVOKE", target = POS_SET, remap = true), index = POS_X)
    private int exotic_worlds$chunkX(int x) {
        return x + this.exotic_worlds$shiftX;
    }

    @ModifyArg(method = {"clear", "get", "set", "line"},
            at = @At(value = "INVOKE", target = POS_SET, remap = true), index = POS_Z)
    private int exotic_worlds$chunkZ(int z) {
        return z + this.exotic_worlds$shiftZ;
    }
}

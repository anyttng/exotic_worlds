package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.redspace.ironsspellbooks.entity.spells.FangSwirlEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = FangSwirlEntity.class, remap = false)
public abstract class FangSwirlEntityMixin {
    @Unique
    private static final String START_POS =
            "Lio/redspace/ironsspellbooks/entity/spells/FangSwirlEntity;startPos:Lnet/minecraft/world/phys/Vec3;";

    // The first read is the null test, which a seat would dereference.
    @ModifyExpressionValue(method = "tick()V", at = {
            @At(value = "FIELD", opcode = Opcodes.GETFIELD, ordinal = 1, target = START_POS),
            @At(value = "FIELD", opcode = Opcodes.GETFIELD, ordinal = 2, target = START_POS),
            @At(value = "FIELD", opcode = Opcodes.GETFIELD, ordinal = 3, target = START_POS),
            @At(value = "FIELD", opcode = Opcodes.GETFIELD, ordinal = 4, target = START_POS)})
    private Vec3 exotic_worlds$startBesideSwirl(Vec3 startPos) {
        return SeamSteering.nearestCopy((Entity) (Object) this, startPos);
    }
}

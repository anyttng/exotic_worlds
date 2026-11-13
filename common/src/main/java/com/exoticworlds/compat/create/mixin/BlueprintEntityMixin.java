package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.zurrtum.create.content.equipment.blueprint.BlueprintEntity;
import com.exoticworlds.compat.create.CreateSeamFold;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(BlueprintEntity.class)
public class BlueprintEntityMixin {
    @ModifyVariable(method = "skipAttackInteraction", at = @At("STORE"), ordinal = 0)
    private Vec3 exotic_worlds$foldAttackRay(Vec3 eyePos, Entity source) {
        Entity blueprint = (Entity) (Object) this;
        return CreateSeamFold.nearestCopy(blueprint.level(), blueprint.position(), eyePos);
    }
}

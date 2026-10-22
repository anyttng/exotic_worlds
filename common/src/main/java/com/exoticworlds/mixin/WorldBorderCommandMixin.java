package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.WorldBorderCommand;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

@Mixin(WorldBorderCommand.class)
public class WorldBorderCommandMixin {
    @ModifyVariable(method = "setCenter", at = @At("HEAD"), argsOnly = true)
    private static Vec2 exotic_worlds$storeCenterInsideBounds(Vec2 center,
            @Local(argsOnly = true) CommandSourceStack source) {
        Vec3 folded = WorldLoopAttachments.transformerOf(source.getLevel()).fold(new Vec3(center.x, 0.0, center.y));
        return new Vec2((float) folded.x, (float) folded.z);
    }
}

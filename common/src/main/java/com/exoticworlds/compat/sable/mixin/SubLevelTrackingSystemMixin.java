package com.exoticworlds.compat.sable.mixin;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.core.JomlVectors;
import com.exoticworlds.core.WorldLoopAttachments;

import dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@Mixin(value = SubLevelTrackingSystem.class, remap = false)
public class SubLevelTrackingSystemMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @WrapMethod(method = "shouldLoad")
    private boolean toroidal$trackTheShortWayRound(Player player, Vector3dc entityPosition, Operation<Boolean> original) {
        Vec3 raw = JomlVectors.read(entityPosition);
        Vec3 nearest = WorldLoopAttachments.transformerOf(this.level).nearestCopy(player.position(), raw);
        return original.call(player, nearest == raw ? entityPosition : JomlVectors.write(nearest, new Vector3d()));
    }
}

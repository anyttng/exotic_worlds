package com.exoticworlds.compat.ironsspellbooks.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.spells.fire.WallOfFireSpell;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = WallOfFireSpell.class, remap = false)
public abstract class WallOfFireSpellMixin {
    @ModifyExpressionValue(method = "addAnchor(Lio/redspace/ironsspellbooks/spells/fire/WallOfFireSpell$FireWallData;"
            + "Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;"
            + "Lio/redspace/ironsspellbooks/capabilities/magic/RecastInstance;)V",
            at = @At(value = "INVOKE", ordinal = 0,
                    target = "Lio/redspace/ironsspellbooks/spells/fire/WallOfFireSpell;setOnGround("
                            + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/Level;)"
                            + "Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$anchorBesidePrevious(Vec3 anchor,
            @Local(argsOnly = true) WallOfFireSpell.FireWallData wall, @Local(argsOnly = true) Level level) {
        List<Vec3> anchors = wall.anchorPoints;
        return anchors.isEmpty()
                ? anchor
                : NearestCopy.toward(WorldLoopAttachments.wrappedFoldOf(level), anchors.getLast(), anchor);
    }
}

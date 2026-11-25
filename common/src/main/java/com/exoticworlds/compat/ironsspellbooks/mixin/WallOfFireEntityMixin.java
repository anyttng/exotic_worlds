package com.exoticworlds.compat.ironsspellbooks.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;

import io.redspace.ironsspellbooks.entity.spells.wall_of_fire.WallOfFireEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = WallOfFireEntity.class, remap = false)
public abstract class WallOfFireEntityMixin {
    @Shadow
    protected List<Vec3> anchorPoints;

    @Inject(method = "readSpawnData",
            at = @At(value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/entity/spells/wall_of_fire/WallOfFireEntity;"
                            + "createShield()V"))
    private void exotic_worlds$seatAnchorsBesideTheWall(CallbackInfo ci) {
        Entity wall = (Entity) (Object) this;
        WorldFold fold = WorldLoopAttachments.wrappedFoldOfReader(wall.level());
        Vec3 position = wall.position();
        this.anchorPoints.replaceAll(anchor -> NearestCopy.toward(fold, position, anchor));
    }
}

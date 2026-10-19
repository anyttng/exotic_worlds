package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.exoticworlds.engine.level.BoundLevelFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(PokemonPastureBlockEntity.Tethering.class)
public class TetherBoxMixin implements LevelBindable {
    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Override
    public void exotic_worlds$bindLevel(ServerLevel level) {
        this.exotic_worlds$bound.bind(level);
    }

    @WrapOperation(method = "canRoamTo", at = @At(value = "INVOKE", target = InjectionTargets.AABB_CONTAINS))
    private boolean exotic_worlds$roamThroughSeam(AABB box, Vec3 target, Operation<Boolean> original) {
        return original.call(FoldedBoxQuery.toward(this.exotic_worlds$bound.wrapped(), target, box), target);
    }
}

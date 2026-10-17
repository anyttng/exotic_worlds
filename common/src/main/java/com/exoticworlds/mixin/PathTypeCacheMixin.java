package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.engine.level.BoundLevelFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.PathTypeCache;

@Mixin(PathTypeCache.class)
public class PathTypeCacheMixin implements LevelBindable {
    @Unique
    private final BoundLevelFold toroidal$bound = new BoundLevelFold();

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$bound.bind(level);
    }

    @WrapOperation(
            method = {
                    "getOrCompute(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/pathfinder/PathType;",
                    "invalidate(Lnet/minecraft/core/BlockPos;)V" },
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_AS_LONG),
            require = 2)
    private long toroidal$keyThePhysicalBlock(BlockPos pos, Operation<Long> original) {
        return this.toroidal$bound.fold().foldBlockNode(original.call(pos));
    }
}

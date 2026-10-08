package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.PathTypeCache;

@Mixin(PathTypeCache.class)
public class PathTypeCacheMixin implements LevelBindable {
    @Unique
    private @Nullable ServerLevel toroidal$level;

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$level = level;
    }

    @WrapOperation(
            method = {
                    "getOrCompute(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/pathfinder/PathType;",
                    "invalidate(Lnet/minecraft/core/BlockPos;)V" },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;asLong()J"),
            require = 2)
    private long toroidal$keyThePhysicalBlock(BlockPos pos, Operation<Long> original) {
        long key = original.call(pos);
        ServerLevel level = this.toroidal$level;
        return level == null ? key : WorldLoopAttachments.transformerOf(level).foldBlockNode(key);
    }
}

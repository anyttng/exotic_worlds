package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.mekanism.RadiationBoxFrame;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

@Mixin(targets = "mekanism.common.lib.collection.IndexedCuboidMap$CenteredBoundingBox", remap = false)
public class CenteredBoundingBoxMixin implements RadiationBoxFrame {
    @Shadow
    @Final
    private long center;

    @Shadow
    @Final
    private int minX;

    @Shadow
    @Final
    private int minZ;

    @Shadow
    @Final
    private int maxX;

    @Shadow
    @Final
    private int maxZ;

    @Unique
    private WorldFold exotic_worlds$fold = WorldFolds.NOOP;

    @Override
    public void exotic_worlds$setFold(WorldFold fold) {
        this.exotic_worlds$fold = fold;
    }

    @Override
    public int exotic_worlds$minX() {
        return this.minX;
    }

    @Override
    public int exotic_worlds$minZ() {
        return this.minZ;
    }

    @Override
    public int exotic_worlds$maxX() {
        return this.maxX;
    }

    @Override
    public int exotic_worlds$maxZ() {
        return this.maxZ;
    }

    @ModifyVariable(method = "isInside(Lnet/minecraft/core/Vec3i;)Z", at = @At("HEAD"), argsOnly = true)
    private Vec3i exotic_worlds$copyNearestCenter(Vec3i vector) {
        BlockPos target = vector instanceof BlockPos pos ? pos : new BlockPos(vector);
        BlockPos copy = this.exotic_worlds$fold.nearestCopy(BlockPos.of(this.center), target);
        return copy.equals(target) ? vector : copy;
    }
}

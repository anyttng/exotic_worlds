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
    private WorldFold toroidal$fold = WorldFolds.NOOP;

    @Override
    public void toroidal$setFold(WorldFold fold) {
        this.toroidal$fold = fold;
    }

    @Override
    public int toroidal$minX() {
        return this.minX;
    }

    @Override
    public int toroidal$minZ() {
        return this.minZ;
    }

    @Override
    public int toroidal$maxX() {
        return this.maxX;
    }

    @Override
    public int toroidal$maxZ() {
        return this.maxZ;
    }

    @ModifyVariable(method = "isInside(Lnet/minecraft/core/Vec3i;)Z", at = @At("HEAD"), argsOnly = true)
    private Vec3i toroidal$copyNearestCentre(Vec3i vector) {
        BlockPos target = vector instanceof BlockPos pos ? pos : new BlockPos(vector);
        BlockPos copy = this.toroidal$fold.nearestCopy(BlockPos.of(this.center), target);
        return copy.equals(target) ? vector : copy;
    }
}

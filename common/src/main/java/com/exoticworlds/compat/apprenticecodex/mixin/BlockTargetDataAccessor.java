package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import jp.aquafactory.apprenticecodex.utility.BlockTargetData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

@Mixin(value = BlockTargetData.class, remap = false)
public interface BlockTargetDataAccessor {
    @Invoker("<init>")
    static BlockTargetData exotic_worlds$create() {
        throw new AssertionError();
    }

    @Accessor("hasTarget")
    boolean exotic_worlds$hasTarget();

    @Accessor("hitBlockPos")
    BlockPos exotic_worlds$getHitBlockPos();

    @Accessor("hitFace")
    Direction exotic_worlds$getHitFace();

    @Accessor("hitX")
    double exotic_worlds$getHitX();

    @Accessor("hitY")
    double exotic_worlds$getHitY();

    @Accessor("hitZ")
    double exotic_worlds$getHitZ();

    @Accessor("placePos")
    BlockPos exotic_worlds$getPlacePos();

    @Accessor("placeFacing")
    Direction exotic_worlds$getPlaceFacing();

    @Invoker("setTarget")
    void exotic_worlds$setTarget(BlockPos hitBlockPos, Direction hitFace, Vec3 hitLocation, BlockPos placePos,
            Direction placeFacing);
}

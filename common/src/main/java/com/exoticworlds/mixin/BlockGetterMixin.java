package com.exoticworlds.mixin;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.engine.level.CurrentRay;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

@Mixin(BlockGetter.class)
public interface BlockGetterMixin {
    @WrapMethod(method = "traverseBlocks(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;"
            + "Ljava/lang/Object;Ljava/util/function/BiFunction;Ljava/util/function/Function;)Ljava/lang/Object;")
    private static Object exotic_worlds$bindRay(Vec3 from, Vec3 to, Object context,
            BiFunction<Object, BlockPos, Object> tester, Function<Object, Object> onFail, Operation<Object> original) {
        CurrentRay outer = CurrentRay.enter(from, to);
        try {
            return original.call(from, to, context, tester, onFail);
        } finally {
            CurrentRay.exit(outer);
        }
    }
}

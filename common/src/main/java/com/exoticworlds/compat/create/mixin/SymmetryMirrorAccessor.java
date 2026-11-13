package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.zurrtum.create.infrastructure.component.SymmetryMirror;

import net.minecraft.world.phys.Vec3;

@Mixin(SymmetryMirror.class)
public interface SymmetryMirrorAccessor {
    @Invoker("create")
    static SymmetryMirror exotic_worlds$create(Integer orientationIndex, Vec3 position, String type, Boolean enable) {
        throw new AssertionError();
    }
}

package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import jp.aquafactory.apprenticecodex.spell.lightningarrow.LightningArrowEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;

@Mixin(value = LightningArrowEntity.class, remap = false)
public interface LightningArrowEntityAccessor {
    @Accessor("FLIGHT")
    static EntityDataAccessor<CompoundTag> exotic_worlds$getFlight() {
        throw new AssertionError();
    }
}

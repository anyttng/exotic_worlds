package com.exoticworlds.compat.apprenticecodex;

import com.exoticworlds.compat.apprenticecodex.mixin.LightningArrowEntityAccessor;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import jp.aquafactory.apprenticecodex.spell.lightningarrow.LightningArrowEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

final class EntityDataRewriters {
    private static final String ORIGIN_X_KEY = "X";
    private static final String ORIGIN_Y_KEY = "Y";
    private static final String ORIGIN_Z_KEY = "Z";

    static void register() {
        PacketTranslator.registerEntityDataRewriter(LightningArrowEntity.class,
                LightningArrowEntityAccessor::exotic_worlds$getFlight, EntityDataRewriters::flight);
    }

    static CompoundTag flight(CompoundTag tag, TranslationContext context, Vec3 arrow) {
        if (tag.isEmpty()) {
            return tag;
        }

        Vec3 origin = new Vec3(tag.getDouble(ORIGIN_X_KEY), tag.getDouble(ORIGIN_Y_KEY), tag.getDouble(ORIGIN_Z_KEY));
        Vec3 seated = NearestCopy.toward(context.fold(), arrow, origin);
        if (seated.equals(origin)) {
            return tag;
        }

        CompoundTag moved = tag.copy();
        moved.putDouble(ORIGIN_X_KEY, seated.x);
        moved.putDouble(ORIGIN_Y_KEY, seated.y);
        moved.putDouble(ORIGIN_Z_KEY, seated.z);
        return moved;
    }

    private EntityDataRewriters() {
    }
}

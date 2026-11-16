package com.exoticworlds.engine.net;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;

class OwnedEntityDataRewriterTest {
    private static final float VALUE = 1.0F;

    private static final class Holder implements SyncedDataHolder {
        static final EntityDataAccessor<Float> NAMED = SynchedEntityData.defineId(Holder.class,
                EntityDataSerializers.FLOAT);
        static final EntityDataAccessor<Float> SIBLING = SynchedEntityData.defineId(Holder.class,
                EntityDataSerializers.FLOAT);

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        }

        @Override
        public void onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>> values) {
        }
    }

    @BeforeAll
    static void registerTheNamedAccessor() {
        PacketTranslator.registerEntityDataRewriter(AbstractMinecart.class, () -> Holder.NAMED,
                (value, context, anchor) -> value);
    }

    @Test
    void theNamedAccessorTakesItsRewriterOnTheOwnerAndItsSubclasses() {
        SynchedEntityData.DataValue<Float> item = SynchedEntityData.DataValue.create(Holder.NAMED, VALUE);
        PacketTranslator.EntityDataRewriter<Object> found =
                PacketTranslator.entityDataRewriterFor(AbstractMinecart.class, item);
        Float held = item.value();
        assertSame(held, found.rewrite(held, null, null));
        assertSame(found, PacketTranslator.entityDataRewriterFor(Minecart.class, item));
    }

    @Test
    void aSiblingOfTheSameSerializerTakesNone() {
        SynchedEntityData.DataValue<Float> item = SynchedEntityData.DataValue.create(Holder.SIBLING, VALUE);
        assertNull(PacketTranslator.entityDataRewriterFor(AbstractMinecart.class, item));
    }

    @Test
    void aClassOutsideTheOwnerOrAnUnknownEntityTakesNone() {
        SynchedEntityData.DataValue<Float> item = SynchedEntityData.DataValue.create(Holder.NAMED, VALUE);
        assertNull(PacketTranslator.entityDataRewriterFor(Entity.class, item));
        assertNull(PacketTranslator.entityDataRewriterFor(null, item));
    }
}

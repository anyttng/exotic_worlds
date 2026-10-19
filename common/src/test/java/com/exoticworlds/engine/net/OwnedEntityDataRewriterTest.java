package com.exoticworlds.engine.net;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;

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

    private static final PacketRewriters.EntityDataRewriter<Float> REWRITER = (value, context, anchor) -> value;

    private static PacketRewriters rewriters() {
        PacketRewriters rewriters = new PacketRewriters();
        rewriters.registerEntityData(Projectile.class, () -> Holder.NAMED, REWRITER);
        return rewriters;
    }

    @Test
    void theNamedAccessorTakesItsRewriterOnTheOwnerAndItsSubclasses() {
        PacketRewriters rewriters = rewriters();
        SynchedEntityData.DataValue<Float> item = SynchedEntityData.DataValue.create(Holder.NAMED, VALUE);
        PacketRewriters.EntityDataRewriter<Object> found = rewriters.entityDataFor(Projectile.class, item);
        Float held = item.value();
        assertSame(held, found.rewrite(held, null, null));
        assertSame(found, rewriters.entityDataFor(Arrow.class, item));
    }

    @Test
    void aSiblingOfTheSameSerializerTakesNone() {
        SynchedEntityData.DataValue<Float> item = SynchedEntityData.DataValue.create(Holder.SIBLING, VALUE);
        assertNull(rewriters().entityDataFor(Projectile.class, item));
    }

    @Test
    void aClassOutsideTheOwnerOrAnUnknownEntityTakesNone() {
        PacketRewriters rewriters = rewriters();
        SynchedEntityData.DataValue<Float> item = SynchedEntityData.DataValue.create(Holder.NAMED, VALUE);
        assertNull(rewriters.entityDataFor(Entity.class, item));
        assertNull(rewriters.entityDataFor(null, item));
    }
}

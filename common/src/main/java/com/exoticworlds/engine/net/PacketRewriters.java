package com.exoticworlds.engine.net;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.StartupRegistry;
import com.google.common.base.Suppliers;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class PacketRewriters {
    public interface ParticleRewriter<P extends ParticleOptions> {
        ParticleOptions rewrite(P particle, TranslationContext context, Vec3 clientOrigin);
    }

    public interface EntityDataRewriter<T> {
        T rewrite(T value, TranslationContext context, Vec3 anchor);
    }

    private final StartupRegistry<Class<?>, BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload>>
            clientboundPayloads = new StartupRegistry<>("Clientbound payload rewriters");

    private final StartupRegistry<Class<?>, BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload>>
            serverboundPayloads = new StartupRegistry<>("Serverbound payload rewriters");

    private final StartupRegistry<Class<?>, ParticleRewriter<ParticleOptions>> particles =
            new StartupRegistry<>("Particle rewriters");

    private final StartupRegistry<EntityDataSerializer<?>, EntityDataRewriter<Object>> entityData =
            new StartupRegistry<>("Entity data rewriters");

    private record OwnedAccessor(Class<? extends Entity> owner, Supplier<EntityDataAccessor<?>> accessor) {
        boolean names(Class<?> entityType, SynchedEntityData.DataValue<?> item) {
            EntityDataAccessor<?> held = accessor.get();
            return owner.isAssignableFrom(entityType) && held.id() == item.id()
                    && held.serializer() == item.serializer();
        }
    }

    private final StartupRegistry<OwnedAccessor, EntityDataRewriter<Object>> ownedEntityData =
            new StartupRegistry<>("Owned entity data rewriters");

    <P extends CustomPacketPayload> void registerClientboundPayload(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        clientboundPayloads.register(payloadType, castingRewriter(payloadType, payloadRewriter));
    }

    <P extends CustomPacketPayload> void registerServerboundPayload(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        serverboundPayloads.register(payloadType, castingRewriter(payloadType, payloadRewriter));
    }

    <P extends ParticleOptions> void registerParticle(Class<P> particleType, ParticleRewriter<P> particleRewriter) {
        particles.register(particleType,
                (particle, context, clientOrigin) -> particleRewriter.rewrite(particleType.cast(particle), context, clientOrigin));
    }

    @SuppressWarnings("unchecked")
    <T> void registerEntityData(EntityDataSerializer<T> serializer, EntityDataRewriter<T> dataRewriter) {
        entityData.register(serializer, (value, context, anchor) -> dataRewriter.rewrite((T) value, context, anchor));
    }

    @SuppressWarnings("unchecked")
    <T> void registerEntityData(Class<? extends Entity> owner, Supplier<EntityDataAccessor<T>> accessor,
            EntityDataRewriter<T> dataRewriter) {
        Supplier<EntityDataAccessor<?>> held = Suppliers.memoize(accessor::get);
        ownedEntityData.register(new OwnedAccessor(owner, held),
                (value, context, anchor) -> dataRewriter.rewrite((T) value, context, anchor));
    }

    @Nullable BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload> clientboundPayloadFor(
            CustomPacketPayload payload) {
        return clientboundPayloads.entries().get(payload.getClass());
    }

    @Nullable BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload> serverboundPayloadFor(
            CustomPacketPayload payload) {
        return serverboundPayloads.entries().get(payload.getClass());
    }

    @Nullable ParticleRewriter<ParticleOptions> particleFor(ParticleOptions particle) {
        return particles.entries().get(particle.getClass());
    }

    @Nullable EntityDataRewriter<Object> entityDataFor(@Nullable Class<?> entityType,
            SynchedEntityData.DataValue<?> item) {
        if (entityType != null) {
            for (Map.Entry<OwnedAccessor, EntityDataRewriter<Object>> entry : ownedEntityData.entries().entrySet()) {
                if (entry.getKey().names(entityType, item)) {
                    return entry.getValue();
                }
            }
        }

        return entityData.entries().get(item.serializer());
    }

    private static <P extends CustomPacketPayload> BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload>
            castingRewriter(Class<P> payloadType, BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        return (payload, context) -> payloadRewriter.apply(payloadType.cast(payload), context);
    }
}

package com.exoticworlds.compat.ultimatemapatlases;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.RecordComponent;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.nanaky.ultimate_map_atlases.networking.C2S2COpenAtlasScreenPacket;

public final class AtlasLecternTranslation {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence GATE = ModPresence.gate(LOGGER,
                    "[ew/ultimatemapatlases-compat] gate lectern_open_present")
            .probing("net/nanaky/moonlight/api/platform/network/ChannelHandler$PacketPayload.class",
                    "net/nanaky/ultimate_map_atlases/networking/C2S2COpenAtlasScreenPacket.class")
            .requiring(UltimateMapAtlasesMixinPlugin.ULTIMATE_MAP_ATLASES)
            .build();

    public static void register() {
        if (GATE.admitted()) {
            Rewriter.register();
        }
    }

    static final class Rewriter {
        private static final String PAYLOAD_CLASS = "net.nanaky.moonlight.api.platform.network.ChannelHandler$PacketPayload";

        private static final String REGISTRATION_COMPONENT = "registration";

        private static final String MESSAGE_COMPONENT = "message";

        private static final MethodType ACCESSOR_TYPE = MethodType.methodType(Object.class, Object.class);

        private static final MethodType CONSTRUCTOR_TYPE =
                MethodType.methodType(CustomPacketPayload.class, Object.class, Object.class);

        private final MethodHandle registration;

        private final MethodHandle message;

        private final MethodHandle payload;

        private final MethodHandle source;

        private final MethodHandle hand;

        private final MethodHandle lectern;

        private final MethodHandle pinOnly;

        private final AtomicBoolean refusalLogged = new AtomicBoolean();

        Rewriter(Class<?> payloadType) throws ReflectiveOperationException {
            MethodHandles.Lookup payloads = MethodHandles.privateLookupIn(payloadType, MethodHandles.lookup());
            RecordComponent registrationComponent = componentOf(payloadType, REGISTRATION_COMPONENT);
            RecordComponent messageComponent = componentOf(payloadType, MESSAGE_COMPONENT);
            this.registration = payloads.unreflect(registrationComponent.getAccessor()).asType(ACCESSOR_TYPE);
            this.message = payloads.unreflect(messageComponent.getAccessor()).asType(ACCESSOR_TYPE);
            this.payload = payloads.findConstructor(payloadType, MethodType.methodType(void.class,
                            registrationComponent.getType(), messageComponent.getType()))
                    .asType(CONSTRUCTOR_TYPE);

            Class<C2S2COpenAtlasScreenPacket> open = C2S2COpenAtlasScreenPacket.class;
            MethodHandles.Lookup messages = MethodHandles.privateLookupIn(open, MethodHandles.lookup());
            this.source = messages.findGetter(open, "source", C2S2COpenAtlasScreenPacket.OpenSource.class);
            this.hand = messages.findGetter(open, "hand", InteractionHand.class);
            this.lectern = messages.findGetter(open, "lecternPos", BlockPos.class);
            this.pinOnly = messages.findGetter(open, "pinOnly", boolean.class);
        }

        static void register() {
            Class<? extends CustomPacketPayload> payloadType;
            Rewriter rewriter;
            try {
                payloadType = Class.forName(PAYLOAD_CLASS, false, Rewriter.class.getClassLoader())
                        .asSubclass(CustomPacketPayload.class);
                rewriter = new Rewriter(payloadType);
            } catch (ReflectiveOperationException | ClassCastException refused) {
                LOGGER.warn("Ultimate Map Atlases' open-screen payload cannot be reached ({}); a lectern's atlas"
                        + " past the world edge opens nothing", refused.toString());
                return;
            }

            PacketTranslator.registerClientboundPayloadRewriter(payloadType, rewriter::toClient);
        }

        CustomPacketPayload toClient(CustomPacketPayload sent, TranslationContext context) {
            try {
                Object carried = (Object) this.message.invokeExact((Object) sent);
                if (!(carried instanceof C2S2COpenAtlasScreenPacket open)) {
                    return sent;
                }

                @Nullable BlockPos canonical = (BlockPos) this.lectern.invokeExact(open);
                if (canonical == null) {
                    return sent;
                }

                BlockPos seated = context.toClient(canonical);
                if (seated.equals(canonical)) {
                    return sent;
                }

                C2S2COpenAtlasScreenPacket seatedOpen = new C2S2COpenAtlasScreenPacket(
                        (C2S2COpenAtlasScreenPacket.OpenSource) this.source.invokeExact(open),
                        (InteractionHand) this.hand.invokeExact(open), seated, (boolean) this.pinOnly.invokeExact(open));
                return (CustomPacketPayload) this.payload.invokeExact(
                        (Object) this.registration.invokeExact((Object) sent), (Object) seatedOpen);
            } catch (Error error) {
                throw error;
            } catch (Throwable refusal) {
                if (this.refusalLogged.compareAndSet(false, true)) {
                    LOGGER.warn("Ultimate Map Atlases' open-screen payload refused its seated lectern ({}); it reaches"
                            + " the client unseated", refusal.toString());
                }

                return sent;
            }
        }

        private static RecordComponent componentOf(Class<?> payloadType, String name) throws NoSuchFieldException {
            RecordComponent @Nullable [] components = payloadType.getRecordComponents();
            if (components != null) {
                for (RecordComponent component : components) {
                    if (component.getName().equals(name)) {
                        return component;
                    }
                }
            }

            throw new NoSuchFieldException(payloadType.getName() + "." + name);
        }
    }

    private AtlasLecternTranslation() {
    }
}

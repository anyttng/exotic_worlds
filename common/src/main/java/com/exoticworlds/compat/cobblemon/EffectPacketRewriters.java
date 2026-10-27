package com.exoticworlds.compat.cobblemon;

import java.util.Objects;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.net.messages.client.effect.PokeSnackBlockParticlesPacket;
import com.cobblemon.mod.common.net.messages.client.effect.SaccharineLogBlockParticlesPacket;
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormParticlePacket;
import com.cobblemon.mod.common.net.messages.client.sound.UnvalidatedPlaySoundS2CPacket;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

final class EffectPacketRewriters {
    static void register() {
        PacketTranslator.registerClientboundPayloadRewriter(UnvalidatedPlaySoundS2CPacket.class,
                EffectPacketRewriters::seatedSound);
        PacketTranslator.registerClientboundPayloadRewriter(SpawnSnowstormParticlePacket.class,
                EffectPacketRewriters::seatedSnowstorm);
        PacketTranslator.registerClientboundPayloadRewriter(PokeSnackBlockParticlesPacket.class,
                (payload, context) -> seatedBlockParticles(payload, payload.getBlockPos(), payload.getEntityPos(),
                        context, PokeSnackBlockParticlesPacket::new));
        PacketTranslator.registerClientboundPayloadRewriter(SaccharineLogBlockParticlesPacket.class,
                (payload, context) -> seatedBlockParticles(payload, payload.getBlockPos(), payload.getEntityPos(),
                        context, SaccharineLogBlockParticlesPacket::new));
        PacketTranslator.registerEntityDataRewriter(EmptyPokeBallEntity.class,
                () -> EmptyPokeBallEntity.Companion.getHIT_TARGET_POSITION(),
                (target, context, anchor) -> context.fold().nearestCopy(anchor, target));
    }

    static CustomPacketPayload seatedSound(UnvalidatedPlaySoundS2CPacket payload, TranslationContext context) {
        Vec3 position = new Vec3(payload.getX(), payload.getY(), payload.getZ());
        Vec3 seated = context.nearestCopy(position);
        return seated.equals(position)
                ? payload
                : new UnvalidatedPlaySoundS2CPacket(payload.getSound(), payload.getCategory(), seated.x, seated.y,
                        seated.z, payload.getVolume(), payload.getPitch());
    }

    static CustomPacketPayload seatedSnowstorm(SpawnSnowstormParticlePacket payload, TranslationContext context) {
        Vec3 seated = context.nearestCopy(payload.getPosition());
        return seated.equals(payload.getPosition())
                ? payload
                : new SpawnSnowstormParticlePacket(payload.getEffectId(), seated);
    }

    static <P extends CustomPacketPayload> CustomPacketPayload seatedBlockParticles(P payload, BlockPos block,
            @Nullable BlockPos entity, TranslationContext context,
            BiFunction<BlockPos, @Nullable BlockPos, P> copy) {
        BlockPos seatedBlock = context.nearestCopy(block);
        BlockPos seatedEntity = entity == null ? null : context.nearestCopy(entity);
        return seatedBlock.equals(block) && Objects.equals(seatedEntity, entity)
                ? payload
                : copy.apply(seatedBlock, seatedEntity);
    }

    private EffectPacketRewriters() {
    }
}

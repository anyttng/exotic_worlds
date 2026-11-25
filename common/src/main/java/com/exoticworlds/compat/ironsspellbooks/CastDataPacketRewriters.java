package com.exoticworlds.compat.ironsspellbooks;

import com.exoticworlds.compat.ironsspellbooks.mixin.CameraShakeDataAccessor;
import com.exoticworlds.compat.ironsspellbooks.mixin.CastingMobAimingDataAccessor;
import com.exoticworlds.compat.ironsspellbooks.mixin.SyncCastingMobAimingDataPacketAccessor;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import io.redspace.ironsspellbooks.api.util.CameraShakeData;
import io.redspace.ironsspellbooks.network.SyncCameraShakePacket;
import io.redspace.ironsspellbooks.network.casting.SyncCastingMobAimingDataPacket;
import io.redspace.ironsspellbooks.spells.CastingMobAimingData;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

final class CastDataPacketRewriters {
    static void register() {
        PacketTranslator.registerClientboundPayloadRewriter(SyncCameraShakePacket.class,
                CastDataPacketRewriters::cameraShake);
        PacketTranslator.registerClientboundPayloadRewriter(SyncCastingMobAimingDataPacket.class,
                CastDataPacketRewriters::mobAim);
    }

    static CustomPacketPayload cameraShake(SyncCameraShakePacket packet, TranslationContext context) {
        CameraShakeDataAccessor shake = (CameraShakeDataAccessor) packet.data();
        if (!shake.exotic_worlds$getDimension().equals(context.dimension())) {
            return packet;
        }

        Vec3 origin = shake.exotic_worlds$getOrigin();
        Vec3 seated = context.nearestCopy(origin);
        if (seated.equals(origin)) {
            return packet;
        }

        CameraShakeData seatedShake = CameraShakeDataAccessor.exotic_worlds$create(shake.exotic_worlds$getDimension(),
                shake.exotic_worlds$getId(), shake.exotic_worlds$getDuration(), seated,
                shake.exotic_worlds$getRadius());
        ((CameraShakeDataAccessor) seatedShake).exotic_worlds$setTickCount(shake.exotic_worlds$getTickCount());
        return new SyncCameraShakePacket(seatedShake, packet.remove());
    }

    static CustomPacketPayload mobAim(SyncCastingMobAimingDataPacket packet, TranslationContext context) {
        SyncCastingMobAimingDataPacketAccessor sync = (SyncCastingMobAimingDataPacketAccessor) packet;
        CastingMobAimingDataAccessor aim = (CastingMobAimingDataAccessor) (Object) sync.exotic_worlds$getAimingData();
        Vec3 aimPosition = aim.exotic_worlds$getAimPosition();
        Vec3 lastAimPosition = aim.exotic_worlds$getLastAimPosition();
        Vec3 seatedAim = context.nearestCopy(aimPosition);
        Vec3 seatedLastAim = NearestCopy.toward(context.fold(), seatedAim, lastAimPosition);
        if (seatedAim.equals(aimPosition) && seatedLastAim.equals(lastAimPosition)) {
            return packet;
        }

        CastingMobAimingData seatedData = CastingMobAimingDataAccessor.exotic_worlds$create();
        CastingMobAimingDataAccessor seatedAccess = (CastingMobAimingDataAccessor) (Object) seatedData;
        seatedAccess.exotic_worlds$setAimPosition(seatedAim);
        seatedAccess.exotic_worlds$setLastAimPosition(seatedLastAim);
        return new SyncCastingMobAimingDataPacket(sync.exotic_worlds$getEntityId(), seatedData);
    }

    private CastDataPacketRewriters() {
    }
}

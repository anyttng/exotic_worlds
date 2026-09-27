package com.toroidalworld.engine.net;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.toroidalworld.ToroidalWorld;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PositionRowsPayload(Map<ResourceLocation, List<TagPositions.TagPosition>> blockEntities,
        Set<ResourceLocation> components) implements CustomPacketPayload {
    public static final Type<PositionRowsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, "position_rows"));

    public static final StreamCodec<ByteBuf, PositionRowsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(PositionRows.SUBJECTS_CODEC), PositionRowsPayload::blockEntities,
            ByteBufCodecs.fromCodec(PositionRows.IDS_CODEC), PositionRowsPayload::components,
            PositionRowsPayload::new);

    @Override
    public Type<PositionRowsPayload> type() {
        return TYPE;
    }
}

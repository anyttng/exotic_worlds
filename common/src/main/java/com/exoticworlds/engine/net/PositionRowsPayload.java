package com.exoticworlds.engine.net;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exoticworlds.ExoticWorlds;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PositionRowsPayload(Map<Identifier, List<TagPositions.TagPosition>> blockEntities,
        Set<Identifier> components) implements CustomPacketPayload {
    public static final Type<PositionRowsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, "position_rows"));

    public static final StreamCodec<ByteBuf, PositionRowsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(PositionRows.SUBJECTS_CODEC), PositionRowsPayload::blockEntities,
            ByteBufCodecs.fromCodec(PositionRows.IDS_CODEC), PositionRowsPayload::components,
            PositionRowsPayload::new);

    @Override
    public Type<PositionRowsPayload> type() {
        return TYPE;
    }
}

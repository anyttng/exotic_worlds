package com.exoticworlds.engine.noise;

import java.util.List;

import com.exoticworlds.ExoticWorlds;

import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class SeamNotice {
    public static void hintTo(ServerPlayer player) {
        if (!Commands.LEVEL_GAMEMASTERS.check(player.permissions())) {
            return;
        }

        MinecraftServer server = player.level().getServer();
        SeamNoticeRecord record = SeamNoticeRecord.of(server);
        List<SeamVerdict> unseen = PeriodicityCheck.verdictsOf(server).stream()
                .filter(SeamVerdict::broken)
                .filter(verdict -> !record.contains(verdict.fingerprint()))
                .toList();
        if (unseen.isEmpty()) {
            return;
        }

        unseen.forEach(verdict -> player.sendSystemMessage(SeamMessages.notice(verdict)));
        record.add(unseen.stream().map(SeamVerdict::fingerprint).toList());
        SeamDetailsPending.of(server).mark(player.getUUID());
        ExoticWorlds.LOGGER.info("[ew/edge] notice player={} shown={}", player.getName().getString(),
                unseen.stream().map(SeamVerdict::fingerprint).toList());
    }

    private SeamNotice() {
    }
}

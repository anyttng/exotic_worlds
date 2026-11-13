package com.exoticworlds.compat.create;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.zurrtum.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.zurrtum.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.zurrtum.create.content.logistics.factoryBoard.ServerFactoryPanelBehaviour;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class CreateFactoryPanelFold {
    public static void canonicalise(ServerLevel level, ServerFactoryPanelBehaviour panel) {
        WorldFold fold = WorldLoopAttachments.foldOfReader(level);
        canonicalisePanels(fold, panel.targetedBy);
        canonicaliseLinks(fold, panel.targetedByLinks);
        canonicaliseTargets(fold, panel.targeting);
    }

    static FactoryPanelPosition canonical(WorldFold fold, FactoryPanelPosition position) {
        BlockPos canonical = fold.fold(position.pos());
        return canonical.equals(position.pos()) ? position : new FactoryPanelPosition(canonical, position.slot());
    }

    private static void canonicalisePanels(WorldFold fold,
            Map<FactoryPanelPosition, FactoryPanelConnection> connections) {
        if (connections.isEmpty()) {
            return;
        }

        Map<FactoryPanelPosition, FactoryPanelConnection> folded = new LinkedHashMap<>(connections.size());
        boolean moved = false;
        for (FactoryPanelConnection connection : connections.values()) {
            FactoryPanelPosition from = canonical(fold, connection.from);
            moved |= !from.equals(connection.from);
            connection.from = from;
            folded.put(from, connection);
        }

        if (moved) {
            connections.clear();
            connections.putAll(folded);
        }
    }

    private static void canonicaliseLinks(WorldFold fold, Map<BlockPos, FactoryPanelConnection> connections) {
        if (connections.isEmpty()) {
            return;
        }

        Map<BlockPos, FactoryPanelConnection> folded = new LinkedHashMap<>(connections.size());
        boolean moved = false;
        for (FactoryPanelConnection connection : connections.values()) {
            FactoryPanelPosition from = canonical(fold, connection.from);
            moved |= !from.equals(connection.from);
            connection.from = from;
            folded.put(from.pos(), connection);
        }

        if (moved) {
            connections.clear();
            connections.putAll(folded);
        }
    }

    private static void canonicaliseTargets(WorldFold fold, Set<FactoryPanelPosition> targets) {
        if (targets.isEmpty()) {
            return;
        }

        Set<FactoryPanelPosition> folded = new LinkedHashSet<>(targets.size());
        boolean moved = false;
        for (FactoryPanelPosition target : targets) {
            FactoryPanelPosition canonical = canonical(fold, target);
            moved |= !canonical.equals(target);
            folded.add(canonical);
        }

        if (moved) {
            targets.clear();
            targets.addAll(folded);
        }
    }

    private CreateFactoryPanelFold() {
    }
}

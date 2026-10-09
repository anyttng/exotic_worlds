package com.exoticworlds.compat.xaero;

import org.slf4j.Logger;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.client.engine.ClientFrame;
import com.exoticworlds.compat.ClientShapes;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class XaeroFold {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile Object[] coordReadoutDisplays;

    public static BlockPos foldWorldNodeSpawn(BlockPos spawn) {
        ToroidalShape shape = ClientShapes.current();
        if (shape == null || spawn == null) {
            return spawn;
        }

        return shape.fold(spawn);
    }

    public static Vec3 nearestToCamera(double blockX, double blockZ) {
        return ClientFrame.nearestToCamera(new Vec3(blockX, 0.0, blockZ));
    }

    public static BlockPos foldInfoDisplayPos(Object infoDisplay, BlockPos playerPos) {
        ToroidalShape shape = ClientShapes.current();
        if (shape == null || playerPos == null) {
            return playerPos;
        }

        Object[] targets = coordReadoutDisplays();
        boolean isCoordReadout = false;
        for (Object target : targets) {
            if (target == infoDisplay) {
                isCoordReadout = true;
                break;
            }
        }

        if (!isCoordReadout) {
            return playerPos;
        }

        return shape.fold(playerPos);
    }

    private static Object[] coordReadoutDisplays() {
        Object[] resolved = coordReadoutDisplays;
        if (resolved != null) {
            return resolved;
        }

        try {
            Class<?> displays = Class.forName("xaero.hud.minimap.info.BuiltInInfoDisplays");
            resolved = new Object[] {
                    displays.getField("COORDINATES").get(null),
                    displays.getField("OVERWORLD_COORDINATES").get(null),
                    displays.getField("CHUNK_COORDINATES").get(null),
            };
        } catch (ReflectiveOperationException e) {
            LOGGER.info("[xaero-compat] info_fold_targets_missing error={}", e.toString());
            resolved = new Object[0];
        }

        coordReadoutDisplays = resolved;
        return resolved;
    }

    private XaeroFold() {
    }
}

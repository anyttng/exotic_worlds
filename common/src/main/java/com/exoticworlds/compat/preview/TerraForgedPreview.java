package com.exoticworlds.compat.preview;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.map.MapCopies;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.ToroidalShapeView;
import com.exoticworlds.shape.WorldShapes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public final class TerraForgedPreview {
    private static final int MIN_ZOOM = 1;

    private static final int SPAWN_LINE = 3;

    private static final String HOVER_SEPARATOR = ":";

    private static final String SPAWN_X_PREFIX = "x";

    private static final String SPAWN_Z_PREFIX = " z";

    public static int cappedZoom(PreviewHandle preview, int zoom) {
        CarriedShape shown = shown(preview);
        if (shown == null) {
            return zoom;
        }

        float ceiling = PreviewView.zoomCeiling(new ToroidalShapeView(shown.fold()), MapCopies.current(),
                preview.exotic_worlds$widget().getWidth());
        return Math.min(zoom, Math.max(MIN_ZOOM, Mth.floor(ceiling)));
    }

    public static int heldCenter(Direction.Axis axis, int center, int zoom) {
        ToroidalShape shape = new ToroidalShapeView(CreationShape.routerBuildFold());
        return Mth.floor(PreviewView.heldCenter(shape, MapCopies.current(), axis, center, zoom));
    }

    public static @Nullable PreviewView view(PreviewHandle preview) {
        CarriedShape shown = shown(preview);
        if (shown == null) {
            return null;
        }

        PreviewReadout readout = preview.exotic_worlds$readout();
        return new PreviewView(new ToroidalShapeView(shown.fold()), MapCopies.current(),
                readout.exotic_worlds$centerX(), readout.exotic_worlds$centerZ(), preview.exotic_worlds$zoom());
    }

    public static void foldHovered(PreviewHandle preview) {
        PreviewReadout readout = preview.exotic_worlds$readout();
        PreviewView view = view(preview);
        if (view == null || readout.exotic_worlds$hovered().isEmpty()) {
            return;
        }

        BlockPos inside = view.inside(readout.exotic_worlds$hoveredX(), readout.exotic_worlds$hoveredZ());
        readout.exotic_worlds$hover(inside.getX() + HOVER_SEPARATOR + inside.getZ());
    }

    public static void showCustomSpawn(PreviewHandle preview) {
        SpawnPoint spawn = customSpawn(preview);
        if (spawn == null) {
            return;
        }

        BlockPos pin = SpawnPick.pinOf(spawn);
        preview.exotic_worlds$readout().exotic_worlds$legend()[SPAWN_LINE] =
                SPAWN_X_PREFIX + pin.getX() + SPAWN_Z_PREFIX + pin.getZ();
    }

    public static @Nullable List<BlockPos> customSpawnCopies(PreviewHandle preview) {
        SpawnPoint spawn = customSpawn(preview);
        PreviewView view = view(preview);
        return spawn == null || view == null ? null : view.blockCopiesOf(SpawnPick.pinOf(spawn));
    }

    public static boolean picksSpawn(PreviewHandle preview) {
        return wrapped(preview) != null;
    }

    public static boolean pickSpawn(PreviewHandle preview) {
        CarriedShape shown = wrapped(preview);
        if (shown == null) {
            return false;
        }

        PreviewReadout readout = preview.exotic_worlds$readout();
        if (!readout.exotic_worlds$hovered().isEmpty()) {
            BlockPos picked = new BlockPos(readout.exotic_worlds$hoveredX(), 0, readout.exotic_worlds$hoveredZ());
            moveSpawn(preview, SpawnPick.at(new ToroidalShapeView(shown.fold()), picked));
        }
        return true;
    }

    public static boolean resetOnMarker(PreviewHandle preview, double mouseX, double mouseY) {
        AbstractWidget widget = preview.exotic_worlds$widget();
        if (customSpawn(preview) == null || !widget.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        for (MarkerBox marker : preview.exotic_worlds$readout().exotic_worlds$markers()) {
            if (marker.contains(mouseX, mouseY)) {
                playClickSound(widget);
                moveSpawn(preview, SpawnPoint.DEFAULT);
                return true;
            }
        }

        return false;
    }

    public static void playClickSound(AbstractWidget widget) {
        widget.playDownSound(Minecraft.getInstance().getSoundManager());
    }

    private static void moveSpawn(PreviewHandle preview, SpawnPoint spawn) {
        if (WorldShapes.moveSpawn(spawn)) {
            preview.exotic_worlds$page().exotic_worlds$reshow();
        }
    }

    private static @Nullable SpawnPoint customSpawn(PreviewHandle preview) {
        CarriedShape shown = wrapped(preview);
        if (shown == null) {
            return null;
        }

        SpawnPoint spawn = SpawnPoint.of(shown.fold());
        return spawn.isCustom() ? spawn : null;
    }

    private static @Nullable CarriedShape wrapped(PreviewHandle preview) {
        CarriedShape shown = shown(preview);
        return shown != null && shown.fold().isWrapped() ? shown : null;
    }

    private static @Nullable CarriedShape shown(PreviewHandle preview) {
        return preview.exotic_worlds$page().exotic_worlds$shown();
    }

    private TerraForgedPreview() {
    }
}

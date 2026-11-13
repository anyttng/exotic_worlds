package com.exoticworlds.compat.freeterraforged.preview;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.map.MapCopies;
import com.exoticworlds.compat.preview.CreationShape;
import com.exoticworlds.compat.preview.PreviewView;
import com.exoticworlds.compat.preview.SpawnPick;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.ToroidalShapeView;
import com.exoticworlds.shape.WorldShapes;

import etcodehome.freeterraforged.client.gui.screen.presetconfig.IPreviewHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public final class FreeTerraForgedPreview {
    private static final int MIN_ZOOM = 1;

    private static final int SPAWN_LINE = 3;

    private static final String HOVER_SEPARATOR = ":";

    private static final String SPAWN_X_PREFIX = "x";

    private static final String SPAWN_Z_PREFIX = " z";

    public static PreviewReadout readout(IPreviewHandler preview) {
        return (PreviewReadout) (Object) preview.state();
    }

    public static int cappedZoom(IPreviewHandler preview, int zoom) {
        CarriedShape shown = shown(preview);
        if (shown == null) {
            return zoom;
        }

        float ceiling = PreviewView.zoomCeiling(new ToroidalShapeView(shown.fold()), MapCopies.current(),
                preview.widget().getWidth());
        return Math.min(zoom, Math.max(MIN_ZOOM, Mth.floor(ceiling)));
    }

    public static int heldCenter(Direction.Axis axis, int center, int zoom) {
        ToroidalShape shape = new ToroidalShapeView(CreationShape.routerBuildFold());
        return Mth.floor(PreviewView.heldCenter(shape, MapCopies.current(), axis, center, zoom));
    }

    public static @Nullable PreviewView view(IPreviewHandler preview) {
        CarriedShape shown = shown(preview);
        if (shown == null) {
            return null;
        }

        PreviewReadout readout = readout(preview);
        return new PreviewView(new ToroidalShapeView(shown.fold()), MapCopies.current(),
                readout.exotic_worlds$centerX(), readout.exotic_worlds$centerZ(), preview.getZoom());
    }

    public static void foldHovered(IPreviewHandler preview) {
        PreviewReadout readout = readout(preview);
        PreviewView view = view(preview);
        if (view == null || readout.exotic_worlds$hovered().isEmpty()) {
            return;
        }

        BlockPos inside = view.inside(readout.exotic_worlds$hoveredX(), readout.exotic_worlds$hoveredZ());
        readout.exotic_worlds$hover(inside.getX() + HOVER_SEPARATOR + inside.getZ());
    }

    public static void showCustomSpawn(IPreviewHandler preview) {
        SpawnPoint spawn = customSpawn(preview);
        if (spawn == null) {
            return;
        }

        BlockPos pin = SpawnPick.pinOf(spawn);
        readout(preview).exotic_worlds$legend()[SPAWN_LINE] = SPAWN_X_PREFIX + pin.getX() + SPAWN_Z_PREFIX + pin.getZ();
    }

    public static @Nullable List<BlockPos> customSpawnCopies(IPreviewHandler preview) {
        SpawnPoint spawn = customSpawn(preview);
        PreviewView view = view(preview);
        return spawn == null || view == null ? null : view.blockCopiesOf(SpawnPick.pinOf(spawn));
    }

    public static boolean picksSpawn(IPreviewHandler preview) {
        return wrapped(preview) != null;
    }

    public static boolean pickSpawn(IPreviewHandler preview) {
        CarriedShape shown = wrapped(preview);
        if (shown == null) {
            return false;
        }

        PreviewReadout readout = readout(preview);
        if (!readout.exotic_worlds$hovered().isEmpty()) {
            BlockPos picked = new BlockPos(readout.exotic_worlds$hoveredX(), 0, readout.exotic_worlds$hoveredZ());
            moveSpawn(preview, SpawnPick.at(new ToroidalShapeView(shown.fold()), picked));
        }
        return true;
    }

    public static boolean resetOnMarker(IPreviewHandler preview, double mouseX, double mouseY) {
        if (customSpawn(preview) == null || !preview.widget().isMouseOver(mouseX, mouseY)) {
            return false;
        }

        for (MarkerBox marker : readout(preview).exotic_worlds$markers()) {
            if (marker.contains(mouseX, mouseY)) {
                preview.playClickSound();
                moveSpawn(preview, SpawnPoint.DEFAULT);
                return true;
            }
        }

        return false;
    }

    private static void moveSpawn(IPreviewHandler preview, SpawnPoint spawn) {
        if (WorldShapes.moveSpawn(spawn)) {
            ((ShownShape) preview.page()).exotic_worlds$reshow();
        }
    }

    private static @Nullable SpawnPoint customSpawn(IPreviewHandler preview) {
        CarriedShape shown = wrapped(preview);
        if (shown == null) {
            return null;
        }

        SpawnPoint spawn = SpawnPoint.of(shown.fold());
        return spawn.isCustom() ? spawn : null;
    }

    private static @Nullable CarriedShape wrapped(IPreviewHandler preview) {
        CarriedShape shown = shown(preview);
        return shown != null && shown.fold().isWrapped() ? shown : null;
    }

    private static @Nullable CarriedShape shown(IPreviewHandler preview) {
        return ((ShownShape) preview.page()).exotic_worlds$shown();
    }

    private FreeTerraForgedPreview() {
    }
}

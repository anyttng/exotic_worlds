package com.exoticworlds.compat.simpleatlas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class AtlasCopies {
    public record Offset(float x, float y) {
    }

    public record Layout(float originX, float originY, float width, float height, int minBlockX, int minBlockZ,
            float pixelsPerBlock) {
        boolean contains(double x, double y) {
            return x >= this.originX && x < this.originX + this.width && y >= this.originY
                    && y < this.originY + this.height;
        }

        boolean meets(Offset offset, AtlasView area) {
            float x = this.originX + offset.x();
            float y = this.originY + offset.y();
            return x + this.width > area.contentX() && x < area.contentX() + area.contentWidth()
                    && y + this.height > area.contentY() && y < area.contentY() + area.contentHeight();
        }
    }

    public static final Offset BASE = new Offset(0.0F, 0.0F);

    public static final List<Offset> BASE_ONLY = List.of(BASE);

    public static final int ANY_MOVE = 1;

    private static final Comparator<Offset> ROWS_THEN_COLUMNS =
            Comparator.comparingDouble(Offset::y).thenComparingDouble(Offset::x);

    public static List<Offset> visible(@Nullable ToroidalShape shape, Layout layout, AtlasView area, int moveBlocks) {
        if (shape == null) {
            return BASE_ONLY;
        }

        float pixelsPerBlock = layout.pixelsPerBlock();
        Vec3 anchor = new Vec3(layout.minBlockX(), 0.0, layout.minBlockZ());
        AABB reach = new AABB(
                anchor.x + (area.contentX() - layout.originX() - layout.width()) / pixelsPerBlock, 0.0,
                anchor.z + (area.contentY() - layout.originY() - layout.height()) / pixelsPerBlock,
                anchor.x + (area.contentX() + area.contentWidth() - layout.originX()) / pixelsPerBlock, 1.0,
                anchor.z + (area.contentY() + area.contentHeight() - layout.originY()) / pixelsPerBlock);
        List<Offset> offsets = new ArrayList<>();
        for (ToroidalShape.Oriented<Vec3> copy : shape.copiesInside(reach, anchor)) {
            long moveX = Math.round(copy.value().x - anchor.x);
            long moveZ = Math.round(copy.value().z - anchor.z);
            if (moveX % moveBlocks != 0 || moveZ % moveBlocks != 0) {
                continue;
            }

            Offset offset = new Offset(moveX * pixelsPerBlock, moveZ * pixelsPerBlock);
            if (layout.meets(offset, area)) {
                offsets.add(offset);
            }
        }

        offsets.sort(ROWS_THEN_COLUMNS);
        return offsets;
    }

    public static Offset under(double x, double y, List<Offset> copies, Layout layout) {
        if (layout.contains(x, y)) {
            return BASE;
        }

        for (Offset copy : copies) {
            if (layout.contains(x - copy.x(), y - copy.y())) {
                return copy;
            }
        }

        return BASE;
    }

    private AtlasCopies() {
    }
}

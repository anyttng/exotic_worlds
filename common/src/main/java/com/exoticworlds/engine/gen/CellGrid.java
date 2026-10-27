package com.exoticworlds.engine.gen;

final class CellGrid {
    static final int NO_CELL = -1;

    static final int AXES = 3;

    static final int AXIS_X = 0;

    static final int AXIS_Z = 1;

    static final int AXIS_Y = 2;

    private final int width;

    private final int layers;

    CellGrid(int width, int layers) {
        this.width = width;
        this.layers = layers;
    }

    int width() {
        return this.width;
    }

    int layers() {
        return this.layers;
    }

    int cells() {
        return this.width * this.width * this.layers;
    }

    int cell(int localX, int layer, int localZ) {
        return localX + localZ * this.width + layer * this.width * this.width;
    }

    int localX(int cell) {
        return cell % this.width;
    }

    int localZ(int cell) {
        return cell / this.width % this.width;
    }

    int layer(int cell) {
        return cell / (this.width * this.width);
    }

    boolean onSide(int cell) {
        int localX = this.localX(cell);
        int localZ = this.localZ(cell);
        return localX == 0 || localZ == 0 || localX == this.width - 1 || localZ == this.width - 1;
    }

    int neighbor(int cell, int axis, int step) {
        int localX = this.localX(cell);
        int localZ = this.localZ(cell);
        int layer = this.layer(cell);
        int nextX = axis == AXIS_X ? localX + step : localX;
        int nextZ = axis == AXIS_Z ? localZ + step : localZ;
        int nextLayer = axis == AXIS_Y ? layer + step : layer;
        if (nextX < 0 || nextZ < 0 || nextLayer < 0
                || nextX >= this.width || nextZ >= this.width || nextLayer >= this.layers) {
            return NO_CELL;
        }

        return this.cell(nextX, nextLayer, nextZ);
    }
}

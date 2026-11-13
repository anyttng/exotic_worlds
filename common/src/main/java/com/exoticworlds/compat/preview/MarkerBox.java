package com.exoticworlds.compat.preview;

public record MarkerBox(int tipX, int tipY) {
    public static MarkerBox ofBar(int minX, int barY, int maxX) {
        return new MarkerBox(minX + (maxX - minX - 1) / 2, barY);
    }

    public boolean contains(double x, double y) {
        return SpawnPin.covers(this.tipX, this.tipY, x, y);
    }
}

package com.exoticworlds.compat.betterend;

public interface LapIslands {
    void exotic_worlds$updatePositionsOnLap(EndTerrainLap lap, double x, double z, int maxHeight);

    float exotic_worlds$densityOnLap(double x, double y, double z);

    float exotic_worlds$densityOnLap(double x, double y, double z, float height);
}

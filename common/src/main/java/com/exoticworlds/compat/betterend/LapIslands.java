package com.exoticworlds.compat.betterend;

public interface LapIslands {
    void toroidal$updatePositionsOnLap(EndTerrainLap lap, double x, double z, int maxHeight);

    float toroidal$densityOnLap(double x, double y, double z);

    float toroidal$densityOnLap(double x, double y, double z, float height);
}

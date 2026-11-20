package com.exoticworlds.compat.naturaltemperature;

public interface WaveCells {
    double pointShare(long seed, long cell);

    double value(long seed, long cell);
}

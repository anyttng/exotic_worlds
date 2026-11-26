package com.exoticworlds.compat.ultraterraforged;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.terraforged.RtfClimateCompression;
import com.exoticworlds.compat.terraforged.RtfClimateScales;
import com.exoticworlds.compat.terraforged.RtfLap;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.ClimateScaleCompression;

import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;

// Every heightmap copy cache() hands a thread shares the context's Levels, so the fold rides there.
public interface LapCarrier {
    @SuppressWarnings("deprecation")
    static void carry(GeneratorContext context, WorldFold fold) {
        double compression = RtfClimateCompression.carry(fold,
                (RtfClimateScales) (Object) context.localHeightmap.get().climate().biomeNoise());
        ((LapCarrier) (Object) context.levels).exotic_worlds$carryFold(fold, compression);
        RtfLap.activate();
    }

    static @Nullable WorldFold getFold(Levels levels) {
        return ((LapCarrier) (Object) levels).exotic_worlds$getFold();
    }

    static double getClimateCompression(Levels levels) {
        LapCarrier carrier = (LapCarrier) (Object) levels;
        return carrier.exotic_worlds$getFold() == null
                ? ClimateScaleCompression.NO_COMPRESSION
                : carrier.exotic_worlds$getClimateCompression();
    }

    @Nullable WorldFold exotic_worlds$getFold();

    double exotic_worlds$getClimateCompression();

    void exotic_worlds$carryFold(WorldFold fold, double climateCompression);
}

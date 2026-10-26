package com.exoticworlds.compat.ultraterraforged;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.terraforged.RtfClimateCompression;
import com.exoticworlds.compat.terraforged.RtfClimateScales;
import com.exoticworlds.compat.terraforged.RtfLap;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ClimateScaleCompression;

import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;

// Every heightmap copy cache() hands a thread shares the context's Levels, so the fold rides there.
public interface LapCarrier {
    @SuppressWarnings("deprecation")
    static void carry(GeneratorContext context, WorldFold fold) {
        double compression = RtfClimateCompression.factor(fold,
                (RtfClimateScales) (Object) context.localHeightmap.get().climate().biomeNoise());
        RtfClimateCompression.remember(fold, compression);
        ((LapCarrier) (Object) context.levels).exotic_worlds$carryFold(fold, compression);
        RtfLap.activate();
    }

    static @Nullable WorldFold foldOf(Levels levels) {
        return ((LapCarrier) (Object) levels).exotic_worlds$fold();
    }

    static double climateCompressionOf(Levels levels) {
        LapCarrier carrier = (LapCarrier) (Object) levels;
        return carrier.exotic_worlds$fold() == null
                ? ClimateScaleCompression.NO_COMPRESSION
                : carrier.exotic_worlds$climateCompression();
    }

    @Nullable WorldFold exotic_worlds$fold();

    double exotic_worlds$climateCompression();

    void exotic_worlds$carryFold(WorldFold fold, double climateCompression);
}

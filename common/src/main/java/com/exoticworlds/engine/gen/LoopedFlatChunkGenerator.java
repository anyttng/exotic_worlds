package com.exoticworlds.engine.gen;

import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.FoldGenerators;
import com.exoticworlds.core.gen.ShapedChunkGenerator;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

public final class LoopedFlatChunkGenerator extends FlatLevelSource implements ShapedChunkGenerator {
    public static final MapCodec<LoopedFlatChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    FlatLevelGeneratorSettings.CODEC.fieldOf(SETTINGS_KEY).forGetter(LoopedFlatChunkGenerator::settings),
                    CARRIED_CODEC.forGetter(LoopedFlatChunkGenerator::getCarriedShape)
            ).apply(instance, instance.stable(LoopedFlatChunkGenerator::new)));

    private final CarriedShape carriedShape;

    public LoopedFlatChunkGenerator(FlatLevelGeneratorSettings settings, CarriedShape carriedShape) {
        super(settings);
        this.carriedShape = carriedShape;
        FoldGenerators.fileFold(this, carriedShape);
    }

    @Override
    public CarriedShape getCarriedShape() {
        return this.carriedShape;
    }

    @Override
    public ChunkGenerator getUnshaped() {
        return new FlatLevelSource(settings());
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }
}

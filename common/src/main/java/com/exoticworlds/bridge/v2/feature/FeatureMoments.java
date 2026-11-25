package com.exoticworlds.bridge.v2.feature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.feature.CarvedChunkMomentV2;
import com.exoticworlds.api.v2.feature.ChunkDataMomentV2;
import com.exoticworlds.api.v2.feature.FeatureV2;
import com.exoticworlds.api.v2.feature.LevelMomentV2;
import com.exoticworlds.api.v2.feature.LitChunkMomentV2;
import com.exoticworlds.api.v2.feature.NoiseMomentV2;
import com.exoticworlds.api.v2.feature.RandomStateMomentV2;
import com.exoticworlds.api.v2.feature.SpawnMomentV2;
import com.exoticworlds.api.v2.feature.StructureStartsMomentV2;
import com.exoticworlds.core.feature.CarvedChunkFeature;
import com.exoticworlds.core.feature.ChunkDataFeature;
import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.LevelFeature;
import com.exoticworlds.core.feature.LitChunkFeature;
import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseMoment;
import com.exoticworlds.core.feature.RandomStateFeature;
import com.exoticworlds.core.feature.RandomStateMoment;
import com.exoticworlds.core.feature.SpawnFeature;
import com.exoticworlds.core.feature.SpawnMoment;
import com.exoticworlds.core.feature.StructureStartsFeature;
import com.exoticworlds.core.gen.AddedStart;
import com.exoticworlds.core.gen.StructureStartSet;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.SpawnPoint;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

final class FeatureMoments {
    private final GenerationFeature owner;
    private final Map<Class<?>, GenerationFeature> adapters = new HashMap<>();

    FeatureMoments(GenerationFeature owner, FeatureV2 feature) {
        this.owner = owner;
        if (feature instanceof RandomStateMomentV2 moment) {
            this.adapters.put(RandomStateFeature.class, new AtRandomState(owner, moment));
        }

        if (feature instanceof StructureStartsMomentV2 moment) {
            this.adapters.put(StructureStartsFeature.class, new AtStructureStarts(owner, moment));
        }

        if (feature instanceof NoiseMomentV2 moment) {
            this.adapters.put(NoiseFeature.class, new AtNoise(owner, moment));
        }

        if (feature instanceof SpawnMomentV2 moment) {
            this.adapters.put(SpawnFeature.class, new AtSpawn(owner, moment));
        }

        if (feature instanceof LevelMomentV2 moment) {
            this.adapters.put(LevelFeature.class, new OnLevel(owner, moment));
        }

        if (feature instanceof CarvedChunkMomentV2 moment) {
            this.adapters.put(CarvedChunkFeature.class, new AtCarved(owner, moment));
        }

        if (feature instanceof LitChunkMomentV2 moment) {
            this.adapters.put(LitChunkFeature.class, new AtLit(owner, moment));
        }

        if (feature instanceof ChunkDataMomentV2 moment) {
            this.adapters.put(ChunkDataFeature.class, new AtChunkData(owner, moment));
        }
    }

    <F extends GenerationFeature> @Nullable F of(Class<F> moment) {
        GenerationFeature adapter = this.adapters.get(moment);
        if (adapter != null) {
            return moment.cast(adapter);
        }

        return moment.isInstance(this.owner) ? moment.cast(this.owner) : null;
    }

    private abstract static class Adapter implements GenerationFeature {
        final GenerationFeature owner;

        Adapter(GenerationFeature owner) {
            this.owner = owner;
        }

        @Override
        public final String getKey() {
            return this.owner.getKey();
        }

        @Override
        public final boolean isOn(ShapeOptions options) {
            return this.owner.isOn(options);
        }
    }

    private static final class AtRandomState extends Adapter implements RandomStateFeature {
        private final RandomStateMomentV2 moment;

        AtRandomState(GenerationFeature owner, RandomStateMomentV2 moment) {
            super(owner);
            this.moment = moment;
        }

        @Override
        public void atRandomState(RandomStateMoment state) {
            this.moment.atRandomState(new RandomStateView(state, this.owner));
        }
    }

    private static final class AtStructureStarts extends Adapter implements StructureStartsFeature {
        private final StructureStartsMomentV2 moment;

        AtStructureStarts(GenerationFeature owner, StructureStartsMomentV2 moment) {
            super(owner);
            this.moment = moment;
        }

        @Override
        public List<AddedStart> startsIn(StructureStartSet set) {
            return this.moment.atStructureStarts(new StructureStartsView(set)).stream()
                    .map(added -> new AddedStart(added.structure(), added.chunk()))
                    .toList();
        }
    }

    private static final class AtNoise extends Adapter implements NoiseFeature {
        private final NoiseMomentV2 moment;

        AtNoise(GenerationFeature owner, NoiseMomentV2 moment) {
            super(owner);
            this.moment = moment;
        }

        @Override
        public double[] layerFactors(NoiseMoment noise) {
            return this.moment.layerFactors(new NoiseView(noise));
        }

        @Override
        public double warpFactor(NoiseMoment noise) {
            return this.moment.warpFactor(new NoiseView(noise));
        }

        @Override
        public boolean isOffsetting(NoiseMoment noise) {
            return this.moment.isOffsetting(new NoiseView(noise));
        }
    }

    private static final class AtSpawn extends Adapter implements SpawnFeature {
        private final SpawnMomentV2 moment;

        AtSpawn(GenerationFeature owner, SpawnMomentV2 moment) {
            super(owner);
            this.moment = moment;
        }

        @Override
        public SpawnPoint spawnPointIn(SpawnMoment spawn) {
            return this.moment.spawnChunkIn(new SpawnView(spawn))
                    .map(chunk -> new SpawnPoint(chunk.x(), chunk.z()))
                    .orElse(SpawnPoint.DEFAULT);
        }
    }

    private static class OnLevel extends Adapter implements LevelFeature {
        private final LevelMomentV2 level;

        OnLevel(GenerationFeature owner, LevelMomentV2 level) {
            super(owner);
            this.level = level;
        }

        @Override
        public final boolean isInstalledOn(ServerLevel serverLevel) {
            return this.level.isInstalledOn(serverLevel);
        }
    }

    private static final class AtCarved extends OnLevel implements CarvedChunkFeature {
        private final CarvedChunkMomentV2 moment;

        AtCarved(GenerationFeature owner, CarvedChunkMomentV2 moment) {
            super(owner, moment);
            this.moment = moment;
        }

        @Override
        public void atCarved(ServerLevel level, ChunkAccess chunk) {
            this.moment.atCarved(new ChunkView(level, chunk));
        }
    }

    private static final class AtLit extends OnLevel implements LitChunkFeature {
        private final LitChunkMomentV2 moment;

        AtLit(GenerationFeature owner, LitChunkMomentV2 moment) {
            super(owner, moment);
            this.moment = moment;
        }

        @Override
        public void atLit(ServerLevel level, ChunkAccess chunk, StaticCache2D<GenerationChunkHolder> chunks,
                Executor mainThread) {
            this.moment.atLit(new LitChunkView(level, chunk, chunks, mainThread));
        }
    }

    private static final class AtChunkData extends OnLevel implements ChunkDataFeature {
        private final ChunkDataMomentV2 moment;

        AtChunkData(GenerationFeature owner, ChunkDataMomentV2 moment) {
            super(owner, moment);
            this.moment = moment;
        }

        @Override
        public String getDataKey() {
            return this.owner.getKey();
        }

        @Override
        public @Nullable CompoundTag getSavedData(ServerLevel level, ChunkPos pos) {
            return this.moment.atChunkSave(new ChunkSlotView(level, pos));
        }

        @Override
        public void restoreData(ServerLevel level, ChunkAccess chunk, CompoundTag data) {
            this.moment.atChunkLoad(new ChunkView(level, chunk), data);
        }

        @Override
        public void releaseData(ServerLevel level, ChunkPos pos) {
            this.moment.atChunkRelease(new ChunkSlotView(level, pos));
        }
    }
}

package com.exoticworlds.engine.noise;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.WorldGenTickAccess;

final class LapTwinRegion implements WorldGenLevel {
    static final int REACH = 1;
    static final int SIDE = 2 * REACH + 1;

    private static final Identifier REGION_RANDOM = Identifier.withDefaultNamespace("worldgen_region_random");
    private static final String DUMMY_ID = "DUMMY";
    private static final String ID_KEY = "id";
    private static final String X_KEY = "x";
    private static final String Y_KEY = "y";
    private static final String Z_KEY = "z";
    private static final int POST_PROCESS_SKIPPED = Block.UPDATE_KNOWN_SHAPE;

    private final ServerLevel level;
    private final ChunkAccess[] chunks;
    private final int centerX;
    private final int centerZ;
    private final FeatureRecording recording;
    private final RandomSource random;
    private final BiomeManager biomeManager;
    private final WorldGenTickAccess<Block> blockTicks = new WorldGenTickAccess<>(pos -> this.chunkFor(pos).getBlockTicks());
    private final WorldGenTickAccess<Fluid> fluidTicks = new WorldGenTickAccess<>(pos -> this.chunkFor(pos).getFluidTicks());
    private final Map<Long, ChunkAccess> pastTheReach = new HashMap<>();
    private final AtomicLong subTickCount = new AtomicLong();

    LapTwinRegion(ServerLevel level, RandomState randomState, ChunkPos seedChunk, ChunkAccess[] chunks,
            FeatureRecording recording) {
        this.level = level;
        this.chunks = chunks;
        ChunkPos center = chunks[SIDE * REACH + REACH].getPos();
        this.centerX = center.x();
        this.centerZ = center.z();
        this.recording = recording;
        this.random = randomState.getOrCreateRandomFactory(REGION_RANDOM).at(seedChunk.getWorldPosition());
        this.biomeManager = new BiomeManager(this, BiomeManager.obfuscateSeed(level.getSeed()));
    }

    ChunkAccess center() {
        return this.chunks[SIDE * REACH + REACH];
    }

    private @Nullable ChunkAccess chunkAt(int chunkX, int chunkZ) {
        int offsetX = chunkX - this.centerX;
        int offsetZ = chunkZ - this.centerZ;
        if (Math.abs(offsetX) > REACH || Math.abs(offsetZ) > REACH) {
            return null;
        }

        return this.chunks[(offsetZ + REACH) * SIDE + offsetX + REACH];
    }

    private ChunkAccess chunkFor(BlockPos pos) {
        return this.chunkOrPast(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    private ChunkAccess chunkOrPast(int chunkX, int chunkZ) {
        ChunkAccess chunk = this.chunkAt(chunkX, chunkZ);
        if (chunk != null) {
            return chunk;
        }

        this.recording.pastTheReach();
        return this.pastTheReach.computeIfAbsent(ChunkPos.pack(chunkX, chunkZ),
                _ -> new ProtoChunk(new ChunkPos(chunkX, chunkZ), UpgradeData.EMPTY, this.level,
                        this.level.palettedContainerFactory(), null));
    }

    private boolean withinReach(BlockPos pos) {
        boolean within = this.chunkAt(SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ())) != null;
        if (!within) {
            this.recording.pastTheReach();
        }

        return within;
    }

    @Override
    public long getSeed() {
        return this.level.getSeed();
    }

    @Override
    public boolean ensureCanWrite(BlockPos pos) {
        return this.withinReach(pos);
    }

    @Override
    public ServerLevel getLevel() {
        return this.level;
    }

    @Override
    public ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus targetStatus, boolean loadOrGenerate) {
        ChunkAccess chunk = this.chunkOrPast(chunkX, chunkZ);
        this.recording.took(chunk);
        return chunk;
    }

    @Override
    public boolean hasChunk(int chunkX, int chunkZ) {
        return this.chunkAt(chunkX, chunkZ) != null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.withinReach(pos) ? this.chunkFor(pos).getBlockState(pos) : Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.withinReach(pos) ? this.chunkFor(pos).getFluidState(pos) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public int getHeight(Heightmap.Types type, int x, int z) {
        ChunkAccess chunk = this.chunkAt(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        if (chunk == null) {
            this.recording.pastTheReach();
            return this.getMinY();
        }

        return chunk.getHeight(type, SectionPos.sectionRelative(x), SectionPos.sectionRelative(z)) + 1;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ) {
        ChunkAccess chunk = this.chunkAt(QuartPos.toSection(quartX), QuartPos.toSection(quartZ));
        return chunk != null
                ? chunk.getNoiseBiome(quartX, quartY, quartZ)
                : this.getUncachedNoiseBiome(quartX, quartY, quartZ);
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int quartX, int quartY, int quartZ) {
        return this.level.getUncachedNoiseBiome(quartX, quartY, quartZ);
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        if (!this.withinReach(pos)) {
            return null;
        }

        ChunkAccess chunk = this.chunkFor(pos);
        BlockEntity blockEntity = chunk.getBlockEntity(pos);
        if (blockEntity != null) {
            return blockEntity;
        }

        CompoundTag tag = chunk.getBlockEntityNbt(pos);
        BlockState state = chunk.getBlockState(pos);
        if (tag == null) {
            return null;
        }

        if (DUMMY_ID.equals(tag.getStringOr(ID_KEY, ""))) {
            blockEntity = state.getBlock() instanceof EntityBlock entityBlock ? entityBlock.newBlockEntity(pos, state) : null;
        } else {
            blockEntity = BlockEntity.loadStatic(pos, state, tag, this.level.registryAccess());
        }

        if (blockEntity != null) {
            chunk.setBlockEntity(blockEntity);
        }

        return blockEntity;
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState blockState, @Block.UpdateFlags int updateFlags, int updateLimit) {
        if (!this.withinReach(pos)) {
            return false;
        }

        ChunkAccess chunk = this.chunkFor(pos);
        BlockState oldState = chunk.setBlockState(pos, blockState, updateFlags);
        this.recording.wrote(pos, blockState);
        if (blockState.hasBlockEntity()) {
            CompoundTag tag = new CompoundTag();
            tag.putInt(X_KEY, pos.getX());
            tag.putInt(Y_KEY, pos.getY());
            tag.putInt(Z_KEY, pos.getZ());
            tag.putString(ID_KEY, DUMMY_ID);
            chunk.setBlockEntityNbt(tag);
        } else if (oldState != null && oldState.hasBlockEntity()) {
            chunk.removeBlockEntity(pos);
        }

        if ((updateFlags & POST_PROCESS_SKIPPED) == 0) {
            BlockPos postProcessPos = blockState.getPostProcessPos(this, pos);
            if (postProcessPos != null && this.withinReach(postProcessPos)) {
                this.chunkFor(postProcessPos).markPosForPostprocessing(postProcessPos);
            }
        }

        return true;
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropResources, @Nullable Entity breaker, int updateLimit) {
        return !this.getBlockState(pos).isAir()
                && this.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL, updateLimit);
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean movedByPiston) {
        return this.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        if (!this.withinReach(entity.blockPosition())) {
            return false;
        }

        this.chunkFor(entity.blockPosition()).addEntity(entity);
        return true;
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> predicate) {
        return predicate.test(this.getBlockState(pos));
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> predicate) {
        return predicate.test(this.getFluidState(pos));
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
        return new DifficultyInstance(this.level.getDifficulty(), this.level.getOverworldClockTime(), 0L,
                this.level.getMoonBrightness(pos));
    }

    @Override
    public RandomSource getRandom() {
        return this.random;
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return this.fluidTicks;
    }

    @Override
    public long nextSubTickCount() {
        return this.subTickCount.getAndIncrement();
    }

    @Override
    public EnvironmentAttributeReader environmentAttributes() {
        return EnvironmentAttributeReader.EMPTY;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Override
    public @Nullable Player getNearestPlayer(double x, double y, double z, double maxDist,
            @Nullable Predicate<Entity> predicate) {
        return null;
    }

    @Override
    public int getSkyDarken() {
        return 0;
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.level.getWorldBorder();
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.level.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.level.enabledFeatures();
    }

    @Override
    public LevelData getLevelData() {
        return this.level.getLevelData();
    }

    @Override
    public @Nullable MinecraftServer getServer() {
        return this.level.getServer();
    }

    @Override
    public ChunkSource getChunkSource() {
        return this.level.getChunkSource();
    }

    @Override
    public int getSeaLevel() {
        return this.level.getSeaLevel();
    }

    @Override
    public DimensionType dimensionType() {
        return this.level.dimensionType();
    }

    @Override
    public int getMinY() {
        return this.level.getMinY();
    }

    @Override
    public int getHeight() {
        return this.level.getHeight();
    }

    @Override
    public void playSound(@Nullable Entity except, BlockPos pos, SoundEvent sound, SoundSource source, float volume,
            float pitch) {
    }

    @Override
    public void addParticle(ParticleOptions particle, double x, double y, double z, double xd, double yd, double zd) {
    }

    @Override
    public void levelEvent(@Nullable Entity source, int type, BlockPos pos, int data) {
    }

    @Override
    public void gameEvent(Holder<GameEvent> gameEvent, Vec3 position, GameEvent.Context context) {
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> type, AABB bb,
            Predicate<? super T> selector) {
        return List.of();
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity except, AABB bb, @Nullable Predicate<? super Entity> selector) {
        return List.of();
    }

    @Override
    public List<Player> players() {
        return List.of();
    }
}

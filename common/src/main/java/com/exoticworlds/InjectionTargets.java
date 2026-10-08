package com.exoticworlds;

public final class InjectionTargets {
    public static final String AABB_INTERSECTS =
            "Lnet/minecraft/world/phys/AABB;intersects(Lnet/minecraft/world/phys/AABB;)Z";

    public static final String BIOME_SOURCE_GET_NOISE_BIOME =
            "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;";

    public static final String BLOCK_ENTITY_HANDLE_UPDATE_TAG =
            "Lnet/minecraft/world/level/block/entity/BlockEntity;handleUpdateTag(Lnet/minecraft/nbt/CompoundTag;"
                    + "Lnet/minecraft/core/HolderLookup$Provider;)V";

    public static final String BLOCK_ENTITY_LOAD_WITH_COMPONENTS =
            "Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/nbt/CompoundTag;"
                    + "Lnet/minecraft/core/HolderLookup$Provider;)V";

    public static final String BLOCK_POS_CLOSER_THAN =
            "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z";

    public static final String BLOCK_POS_CLOSER_TO_CENTER_THAN =
            "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z";

    public static final String BLOCK_POS_DIST_MANHATTAN =
            "Lnet/minecraft/core/BlockPos;distManhattan(Lnet/minecraft/core/Vec3i;)I";

    public static final String BLOCK_POS_DIST_SQR =
            "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D";

    public static final String BLOCK_POS_DIST_TO_CENTER_SQR =
            "Lnet/minecraft/core/BlockPos;distToCenterSqr(Lnet/minecraft/core/Position;)D";

    public static final String BLOCK_POS_EQUALS = "Lnet/minecraft/core/BlockPos;equals(Ljava/lang/Object;)Z";

    public static final String BLOCK_POS_OFFSET_PACKED =
            "Lnet/minecraft/core/BlockPos;offset(JLnet/minecraft/core/Direction;)J";

    public static final String BLOCK_POS_RELATIVE =
            "Lnet/minecraft/core/BlockPos;relative(Lnet/minecraft/core/Direction;)Lnet/minecraft/core/BlockPos;";

    public static final String BLOCK_POS_RELATIVE_BY =
            "Lnet/minecraft/core/BlockPos;relative(Lnet/minecraft/core/Direction;I)Lnet/minecraft/core/BlockPos;";

    public static final String BLOCK_POS_SUBTRACT =
            "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;";

    public static final String CHUNK_POS_AS_LONG = "Lnet/minecraft/world/level/ChunkPos;asLong(II)J";

    public static final String CHUNK_POS_AS_LONG_BLOCK =
            "Lnet/minecraft/world/level/ChunkPos;asLong(Lnet/minecraft/core/BlockPos;)J";

    public static final String DENSITY_FUNCTION_COMPUTE =
            "Lnet/minecraft/world/level/levelgen/DensityFunction;compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D";

    public static final String DISTANCE_PREDICATE_MATCHES =
            "Lnet/minecraft/advancements/critereon/DistancePredicate;matches(DDDDDD)Z";

    public static final String FUNCTION_CONTEXT_BLOCK_X =
            "Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;blockX()I";

    public static final String FUNCTION_CONTEXT_BLOCK_Z =
            "Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;blockZ()I";

    public static final String ENTITY_GET_BOUNDING_BOX =
            "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;";

    public static final String ENTITY_GET_EYE_POSITION =
            "Lnet/minecraft/world/entity/Entity;getEyePosition()Lnet/minecraft/world/phys/Vec3;";

    public static final String ENTITY_GET_X = "Lnet/minecraft/world/entity/Entity;getX()D";

    public static final String ENTITY_GET_Z = "Lnet/minecraft/world/entity/Entity;getZ()D";

    public static final String ENTITY_POSITION =
            "Lnet/minecraft/world/entity/Entity;position()Lnet/minecraft/world/phys/Vec3;";

    public static final String ITEM_STACK_GET =
            "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;";

    public static final String LEVEL_GET_BLOCK_STATE =
            "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;";

    public static final String LIVING_ENTITY_GET_X = "Lnet/minecraft/world/entity/LivingEntity;getX()D";

    public static final String LIVING_ENTITY_GET_Z = "Lnet/minecraft/world/entity/LivingEntity;getZ()D";

    public static final String MAP_PUT = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";

    public static final String MTH_ATAN2 = "Lnet/minecraft/util/Mth;atan2(DD)D";

    public static final String PATHFINDER_MOB_GET_RESTRICT_CENTER =
            "Lnet/minecraft/world/entity/PathfinderMob;getRestrictCenter()Lnet/minecraft/core/BlockPos;";

    public static final String PATH_GET_TARGET =
            "Lnet/minecraft/world/level/pathfinder/Path;getTarget()Lnet/minecraft/core/BlockPos;";

    public static final String PHANTOM_MOVE_TARGET_POINT =
            "Lnet/minecraft/world/entity/monster/Phantom;moveTargetPoint:Lnet/minecraft/world/phys/Vec3;";

    public static final String POSITIONAL_RANDOM_FACTORY_AT =
            "Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;at(III)Lnet/minecraft/util/RandomSource;";

    public static final String SET_CONTAINS = "Ljava/util/Set;contains(Ljava/lang/Object;)Z";

    public static final String STATIC_CACHE_2D_CREATE =
            "Lnet/minecraft/util/StaticCache2D;create(IIILnet/minecraft/util/StaticCache2D$Initializer;)Lnet/minecraft/util/StaticCache2D;";

    public static final String STREAM_MIN = "Ljava/util/stream/Stream;min(Ljava/util/Comparator;)Ljava/util/Optional;";

    public static final String STREAM_SORTED =
            "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;";

    public static final String VEC3_ADD =
            "Lnet/minecraft/world/phys/Vec3;add(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;";

    public static final String VEC3_ADD_SCALARS =
            "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;";

    public static final String VEC3_AT_BOTTOM_CENTER_OF =
            "Lnet/minecraft/world/phys/Vec3;atBottomCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;";

    public static final String VEC3_AT_CENTER_OF =
            "Lnet/minecraft/world/phys/Vec3;atCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;";

    public static final String VEC3_CLOSER_THAN =
            "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z";

    public static final String VEC3_DISTANCE_TO =
            "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D";

    public static final String VEC3_DISTANCE_TO_SQR =
            "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D";

    public static final String VEC3_DISTANCE_TO_SQR_XYZ = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(DDD)D";

    public static final String VEC3_INIT = "Lnet/minecraft/world/phys/Vec3;<init>(DDD)V";

    public static final String VEC3_NEW = "(DDD)Lnet/minecraft/world/phys/Vec3;";

    public static final String VEC3_SUBTRACT =
            "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;";

    private InjectionTargets() {
    }
}

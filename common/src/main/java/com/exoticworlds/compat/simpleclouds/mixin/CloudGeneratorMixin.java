package com.exoticworlds.compat.simpleclouds.mixin;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.simpleclouds.CloudShape;
import com.exoticworlds.compat.simpleclouds.SimpleCloudsShapes;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.nonamecrackers2.simpleclouds.api.common.cloud.spawning.CreateRegionFunction;
import dev.nonamecrackers2.simpleclouds.api.common.cloud.spawning.SpawnInfo;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudGenerator;
import dev.nonamecrackers2.simpleclouds.common.world.SpawnRegion;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = CloudGenerator.class, remap = false)
public abstract class CloudGeneratorMixin {
    @Unique
    private static final String CREATE_REGION =
            "createRegion(Ldev/nonamecrackers2/simpleclouds/api/common/cloud/spawning/SpawnInfo;FFFFLnet/minecraft/util/RandomSource;Z)Ljava/util/Optional;";

    @Shadow
    @Final
    protected CloudTypeSource cloudGetter;

    @WrapMethod(method = "getCloudAtPosition(FF)Ldev/nonamecrackers2/simpleclouds/common/cloud/region/CloudRegion;")
    private CloudRegion exotic_worlds$bindCloudAtPosition(float x, float z, Operation<CloudRegion> original) {
        return SimpleCloudsShapes.bound(this.exotic_worlds$level(), () -> original.call(x, z));
    }

    @WrapMethod(method = "getCloudsInRegion")
    private List<CloudRegion> exotic_worlds$bindCloudsInRegion(SpawnRegion region, Operation<List<CloudRegion>> original) {
        return SimpleCloudsShapes.bound(this.exotic_worlds$level(), () -> original.call(region));
    }

    @WrapMethod(method = "getRegionsThatOccupyCloud")
    private List<SpawnRegion> exotic_worlds$bindRegionsThatOccupyCloud(CloudRegion cloud,
            Operation<List<SpawnRegion>> original) {
        return SimpleCloudsShapes.bound(this.exotic_worlds$level(), () -> original.call(cloud));
    }

    @WrapMethod(method = "addCloud")
    private boolean exotic_worlds$bindAddCloud(CloudRegion region, CloudGenerator.Order order, Operation<Boolean> original) {
        return SimpleCloudsShapes.bound(this.exotic_worlds$level(), () -> original.call(region, order));
    }

    @WrapMethod(method = "tick")
    private void exotic_worlds$bindTick(@Nullable Level level, float speed, Operation<Void> original) {
        SimpleCloudsShapes.bound(this.exotic_worlds$level(), () -> original.call(level, speed));
    }

    @WrapMethod(method = "spawnCloud(Ljava/util/function/Supplier;IILnet/minecraft/world/level/Level;Ldev/nonamecrackers2/simpleclouds/api/common/cloud/spawning/CreateRegionFunction;)Ljava/util/Optional;")
    private Optional<CloudRegion> exotic_worlds$bindSpawnCloud(Supplier<SpawnInfo> infoGetter, int nextSpawnInterval,
            int maxRegions, Level level, CreateRegionFunction regionFunc, Operation<Optional<CloudRegion>> original) {
        return SimpleCloudsShapes.bound(this.exotic_worlds$level(),
                () -> original.call(infoGetter, nextSpawnInterval, maxRegions, level, regionFunc));
    }

    @WrapMethod(method = CREATE_REGION)
    private Optional<CloudRegion> exotic_worlds$bindCreateRegion(SpawnInfo info, float playerX, float playerZ, float x,
            float z, RandomSource random, boolean growTime, Operation<Optional<CloudRegion>> original) {
        return SimpleCloudsShapes.bound(this.exotic_worlds$level(),
                () -> original.call(info, playerX, playerZ, x, z, random, growTime));
    }

    @WrapMethod(method = "doInitialGen")
    private void exotic_worlds$bindInitialGen(int x, int z, Level level, boolean ignoreOtherRegions,
            Operation<Void> original) {
        SimpleCloudsShapes.bound(this.exotic_worlds$level(), () -> original.call(x, z, level, ignoreOtherRegions));
    }

    @WrapOperation(method = CREATE_REGION, at = @At(value = "INVOKE", target = "Lorg/joml/Vector2f;distance(FFFF)F"))
    private float exotic_worlds$seatSpacing(float x, float z, float regionX, float regionZ, Operation<Float> original) {
        CloudShape shape = SimpleCloudsShapes.current();
        if (shape == null) {
            return original.call(x, z, regionX, regionZ);
        }

        Vec3 seated = shape.shape().nearestCopy(new Vec3(x, 0.0, z), new Vec3(regionX, 0.0, regionZ));
        return original.call(x, z, (float) seated.x, (float) seated.z);
    }

    @Unique
    private @Nullable Level exotic_worlds$level() {
        return this.cloudGetter instanceof CloudManagerAccessor manager ? manager.exotic_worlds$level() : null;
    }
}

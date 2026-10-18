package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.engine.level.BoundLevelFold;
import com.exoticworlds.engine.seam.SeamSnap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;

@Mixin(PersistentEntitySectionManager.class)
public class EntitySectionManagerMixin<T extends EntityAccess> implements LevelBindable {
    @Shadow
    @Final
    private EntitySectionStorage<T> sectionStorage;

    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Override
    public void exotic_worlds$bindLevel(ServerLevel level) {
        this.exotic_worlds$bound.bind(level);
        ((LevelBindable) this.sectionStorage).exotic_worlds$bindLevel(level);
    }

    @Inject(
            method = {
                    "addEntity(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z",
                    "addEntityWithoutEvent(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z" },
            at = @At("HEAD"),
            require = 1)
    private void exotic_worlds$wrapJoiningEntity(EntityAccess entity, boolean loaded, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Entity actualEntity) {
            SeamSnap.intoBounds(actualEntity);
        }
    }

    @ModifyArg(
            method = {"canPositionTick", "areEntitiesLoaded"},
            at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"),
            index = 0,
            expect = 3)
    private long exotic_worlds$askThePhysicalChunk(long chunkKey) {
        return this.exotic_worlds$bound.fold().foldChunkKey(chunkKey);
    }
}

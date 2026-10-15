package com.exoticworlds.compat.sable.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.sable.SeamFrame;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.SeamEntitySearch;

import dev.ryanhcode.sable.util.SubLevelInclusiveLevelEntityGetter;

import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;

@Mixin(SubLevelInclusiveLevelEntityGetter.class)
public abstract class SubLevelInclusiveLevelEntityGetterMixin<T extends EntityAccess> {
    @Unique
    private static final String BOX_QUERY = "get(Lnet/minecraft/world/phys/AABB;Ljava/util/function/Consumer;)V";
    @Unique
    private static final String TYPED_BOX_QUERY = "get(Lnet/minecraft/world/level/entity/EntityTypeTest;"
            + "Lnet/minecraft/world/phys/AABB;Lnet/minecraft/util/AbortableIterationConsumer;)V";

    @Shadow
    @Final
    private Level level;

    @WrapMethod(method = BOX_QUERY)
    private void toroidal$frameOnBoxQuery(AABB box, Consumer<T> consumer, Operation<Void> original) {
        SeamFrame.with(this.level, box, AABB::getCenter, original, box, consumer);
    }

    @WrapMethod(method = TYPED_BOX_QUERY)
    private <U extends T> void toroidal$frameOnTypedBoxQuery(EntityTypeTest<T, U> typeTest, AABB box,
            AbortableIterationConsumer<U> consumer, Operation<Void> original) {
        SeamFrame.with(this.level, box, AABB::getCenter, original, typeTest, box, consumer);
    }

    @WrapOperation(method = BOX_QUERY, at = @At(value = "INVOKE", target = InjectionTargets.LEVEL_ENTITY_GETTER_GET))
    private void toroidal$delegateThroughSeam(LevelEntityGetter<T> delegate, AABB box, Consumer<T> consumer,
            Operation<Void> original) {
        SeamEntitySearch.forEach(toroidal$fold(), box, consumer,
                (piece, sink) -> original.call(delegate, piece, sink));
    }

    @WrapOperation(
            method = TYPED_BOX_QUERY,
            at = @At(value = "INVOKE", target = InjectionTargets.LEVEL_ENTITY_GETTER_GET_TYPED))
    private <U extends T> void toroidal$typedDelegateThroughSeam(LevelEntityGetter<T> delegate,
            EntityTypeTest<T, U> typeTest, AABB box, AbortableIterationConsumer<U> consumer,
            Operation<Void> original) {
        SeamEntitySearch.forEachUntilAborted(toroidal$fold(), box, consumer,
                (piece, sink) -> original.call(delegate, typeTest, piece, sink));
    }

    @Unique
    private WorldFold toroidal$fold() {
        return WorldLoopAttachments.transformerOf(this.level);
    }
}

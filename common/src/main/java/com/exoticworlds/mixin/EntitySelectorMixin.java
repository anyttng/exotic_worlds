package com.exoticworlds.mixin;

import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.exoticworlds.engine.seam.SeamCommandErrors;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(EntitySelector.class)
public class EntitySelectorMixin {
    @WrapOperation(
            method = {"findEntities", "findPlayers"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/commands/arguments/selector/EntitySelector;getPredicate(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/flag/FeatureFlagSet;)Ljava/util/function/Predicate;"))
    private Predicate<Entity> exotic_worlds$boxThroughSeam(EntitySelector selector, Vec3 pos, @Nullable AABB absoluteAabb,
            @Nullable FeatureFlagSet enabledFeatures, Operation<Predicate<Entity>> original,
            @Local(argsOnly = true) CommandSourceStack sender) {
        if (absoluteAabb == null || !exotic_worlds$searchesAWrappedLevel(sender)) {
            return original.call(selector, pos, absoluteAabb, enabledFeatures);
        }

        Predicate<Entity> withoutBox = original.call(selector, pos, null, enabledFeatures);
        return entity -> withoutBox.test(entity) && exotic_worlds$insideThroughSeam(entity, absoluteAabb);
    }

    @WrapOperation(
            method = {"findEntities", "findPlayers"},
            at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object exotic_worlds$refuseCenterOutsideWorld(Function<Object, Object> position, Object senderPosition,
            Operation<Object> original, @Local(argsOnly = true) CommandSourceStack sender)
            throws CommandSyntaxException {
        Object resolved = original.call(position, senderPosition);
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(sender.getLevel());
        if (fold == null || !(resolved instanceof Vec3 pos) || !(senderPosition instanceof Vec3 senderPos)) {
            return resolved;
        }

        if (pos.x != senderPos.x) {
            SeamCommandErrors.requireInsideWorld(fold, Direction.Axis.X, pos.x);
        }

        if (pos.z != senderPos.z) {
            SeamCommandErrors.requireInsideWorld(fold, Direction.Axis.Z, pos.z);
        }

        return resolved;
    }

    @Unique
    private static boolean exotic_worlds$searchesAWrappedLevel(CommandSourceStack sender) {
        Entity entity = sender.getEntity();
        return WorldLoopAttachments.wrappedFoldOf(sender.getLevel()) != null
                || entity != null && WorldLoopAttachments.wrappedFoldOf(entity.level()) != null;
    }

    @Unique
    private static boolean exotic_worlds$insideThroughSeam(Entity entity, AABB absoluteAabb) {
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(entity.level());
        AABB box = FoldedBoxQuery.toward(fold, entity.position(), absoluteAabb);
        return box.intersects(entity.getBoundingBox());
    }
}

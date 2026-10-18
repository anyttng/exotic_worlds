package com.exoticworlds.compat.electroenergetics.mixin;

import java.util.function.Predicate;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.bawnorton.mixinsquared.TargetHandler;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.electroenergetics.WireTemplates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

@Mixin(value = StructureTemplate.class, priority = 1300, remap = false)
public class StructureTemplateWireMixin {
    @Unique
    private static final String STRUCTURE_TEMPLATE_MIXIN = "com.george_vi.electroenergetics.mixins.StructureTemplateMixin";
    @Unique
    private static final String CAPTURE_HANDLER = "fillFromWorld";
    @Unique
    private static final String PLACE_HANDLER = "placeInWorld";

    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"))
    @TargetHandler(
            mixin = STRUCTURE_TEMPLATE_MIXIN,
            name = CAPTURE_HANDLER)
    private Stream<InWorldNode> exotic_worlds$testNodesInCaptureBox(Stream<InWorldNode> nodes,
            Predicate<InWorldNode> inBox, Operation<Stream<InWorldNode>> original,
            @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos origin,
            @Local(argsOnly = true) Vec3i size) {
        Predicate<InWorldNode> seated = node -> inBox.test(WireTemplates.inCaptureBox(level, origin, size, node));
        return original.call(nodes, seated);
    }

    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE",
                    target = "Lcom/george_vi/electroenergetics/foundation/BoundingBoxUtils;isIn"
                            + "(Lnet/minecraft/core/Vec3i;Lnet/minecraft/core/Vec3i;Lnet/minecraft/core/Vec3i;)Z"))
    @TargetHandler(
            mixin = STRUCTURE_TEMPLATE_MIXIN,
            name = CAPTURE_HANDLER)
    private boolean exotic_worlds$testInCaptureBox(Vec3i pos, Vec3i origin, Vec3i size, Operation<Boolean> original,
            @Local(argsOnly = true) Level level) {
        return original.call(WireTemplates.inCaptureBox(level, origin, size, pos), origin, size);
    }

    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_SUBTRACT))
    @TargetHandler(
            mixin = STRUCTURE_TEMPLATE_MIXIN,
            name = CAPTURE_HANDLER)
    private BlockPos exotic_worlds$relativeInCaptureBox(BlockPos pos, Vec3i origin, Operation<BlockPos> original,
            @Local(argsOnly = true) Level level, @Local(argsOnly = true) Vec3i size) {
        return original.call(WireTemplates.inCaptureBox(level, origin, size, pos), origin);
    }

    @ModifyExpressionValue(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_OFFSET))
    @TargetHandler(
            mixin = STRUCTURE_TEMPLATE_MIXIN,
            name = PLACE_HANDLER)
    private BlockPos exotic_worlds$placeCanonical(BlockPos placed, @Local(argsOnly = true) ServerLevelAccessor accessor) {
        return WireTemplates.placed(accessor, placed);
    }
}

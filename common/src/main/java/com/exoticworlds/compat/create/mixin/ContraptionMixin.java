package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.exoticworlds.compat.create.CatnipInjectionTargets;
import com.exoticworlds.compat.create.ChassisWalkFrame;
import com.exoticworlds.compat.create.CreateTranslation;
import com.exoticworlds.compat.create.CreateSeamFold;
import com.exoticworlds.compat.create.LappedKeys;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import java.util.List;
import java.util.Queue;
import java.util.Set;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Contraption.class, remap = false)
public class ContraptionMixin {
    @Unique
    private static final String CIRCLES_THE_WORLD_KEY = "message.exotic_worlds.create.structure_circles_the_world";

    @Shadow
    public BlockPos anchor;

    @Shadow
    public AABB bounds;

    @Shadow
    protected List<AABB> superglue;

    @Shadow
    protected Multimap<BlockPos, StructureBlockInfo> capturedMultiblocks;

    @ModifyVariable(method = "searchMovedStructure", at = @At("STORE"), ordinal = 0)
    private Set<BlockPos> exotic_worlds$closeTheWalkOnCanonicalKeys(Set<BlockPos> visited, Level world, BlockPos pos,
            Direction forcedDirection) {
        return LappedKeys.set(world);
    }

    @WrapMethod(method = "moveBlock")
    private boolean exotic_worlds$dropANameItsOwnCopyAlreadyClaimed(Level world, Direction forcedDirection,
            Queue<BlockPos> frontier, Set<BlockPos> visited, Operation<Boolean> original) {
        BlockPos polled = frontier.peek();
        if (polled == null || !visited.contains(polled)) {
            return original.call(world, forcedDirection, frontier, visited);
        }

        frontier.poll();
        return true;
    }

    @Inject(method = "searchMovedStructure", at = @At("RETURN"))
    private void exotic_worlds$refuseAStructureThatCirclesTheWorld(Level world, BlockPos pos, Direction forcedDirection,
            CallbackInfoReturnable<Boolean> cir, @Local Set<BlockPos> visited) throws AssemblyException {
        if (visited instanceof LappedKeys keys && keys.lapped() != null) {
            throw new AssemblyException(Component.translatable(CIRCLES_THE_WORLD_KEY));
        }
    }

    @ModifyExpressionValue(
            method = "captureMultiblock",
            at = @At(value = "INVOKE",
                    target = CatnipInjectionTargets.NBT_HELPER_READ_BLOCK_POS))
    private BlockPos exotic_worlds$foldCapturedController(BlockPos stored, BlockPos localPos, StructureBlockInfo info,
            BlockEntity be) {
        return CreateSeamFold.nearestCopy(be.getLevel(), localPos.offset(anchor), stored);
    }

    @WrapMethod(method = "moveChassis")
    private boolean exotic_worlds$walkChassisInTheAssemblyFrame(Level world, BlockPos pos, Direction movementDirection,
            Queue<BlockPos> frontier, Set<BlockPos> visited, Operation<Boolean> original) {
        return ChassisWalkFrame.withAnchor(world, pos,
                () -> original.call(world, pos, movementDirection, frontier, visited));
    }

    @Inject(method = "removeBlocksFromWorld",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Ljava/util/Set;forEach(Ljava/util/function/Consumer;)V"),
            require = 1,
            allow = 1)
    private void exotic_worlds$foldGlueIntoTheLocalFrame(Level world, BlockPos offset, CallbackInfo ci) {
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(world);
        if (fold == null) {
            return;
        }

        Vec3 center = bounds == null ? Vec3.ZERO : bounds.getCenter();
        superglue.replaceAll(box -> fold.nearestCopyOriented(center, box).value());
    }

    @ModifyExpressionValue(method = "addBlocksToWorld",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/contraptions/StructureTransform;apply(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$canonicalDisassemblyTarget(BlockPos target, @Local(argsOnly = true) Level world) {
        return CreateSeamFold.canonicalOnServer(world, target);
    }

    @Inject(method = "addBlocksToWorld",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lcom/simibubi/create/content/contraptions/Contraption;translateMultiblockControllers(Lcom/simibubi/create/content/contraptions/StructureTransform;)V"))
    private void exotic_worlds$canonicalizeTranslatedControllers(Level world, StructureTransform transform,
            CallbackInfo ci) {
        for (StructureBlockInfo info : capturedMultiblocks.values()) {
            CompoundTag nbt = info.nbt();
            // A LastKnownPos part keeps its capture-time local Controller, which canonicalizing would corrupt.
            if (nbt == null || !nbt.contains(CreateTranslation.CONTROLLER_KEY)
                    || nbt.contains(CreateTranslation.LAST_KNOWN_POS_KEY)) {
                continue;
            }

            BlockPos stored = NBTHelper.readBlockPos(nbt, CreateTranslation.CONTROLLER_KEY);
            BlockPos canonical = CreateSeamFold.canonicalOnServer(world, stored);
            if (canonical != stored) {
                nbt.put(CreateTranslation.CONTROLLER_KEY, NbtUtils.writeBlockPos(canonical));
            }
        }
    }
}

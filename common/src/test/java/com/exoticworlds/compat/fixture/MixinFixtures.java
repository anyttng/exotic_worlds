package com.exoticworlds.compat.fixture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

public final class MixinFixtures {
    private static final String TARGET = "com.exoticworlds.compat.fixture.FixtureTarget";
    private static final String NOWHERE = "com.exoticworlds.compat.fixture.Nowhere";
    private static final String FOREIGN_MIXIN = "com.exoticworlds.compat.fixture.FixtureForeignMixin";
    private static final String HELPER = "Lcom/exoticworlds/compat/fixture/FixtureTarget;helper(I)I";
    private static final String ABSENT_CALL = "Lcom/exoticworlds/compat/fixture/FixtureTarget;absent(I)I";
    private static final String COUNTER = "Lcom/exoticworlds/compat/fixture/FixtureTarget;counter:I";
    private static final String ABSENT_FIELD = "Lcom/exoticworlds/compat/fixture/FixtureTarget;absent:I";
    private static final String VALUE_OF = "Ljava/lang/Integer;valueOf(I)Ljava/lang/Integer;";
    private static final String ABS = "Ljava/lang/Math;abs(I)I";
    private static final String HANDLER = "@MixinSquared:Handler";
    private static final String SHADOWING_MIXIN = "com.exoticworlds.compat.fixture.FixtureShadowingMixin";
    public static final String INTERMEDIARY_TARGET = "com.exoticworlds.compat.fixture.IntermediaryTarget";

    @Mixin(FixtureTarget.class)
    public static class WrapOperationPresent {
        @WrapOperation(method = "work", at = @At(value = "INVOKE", target = HELPER))
        private void wrap() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class WrapOperationMissing {
        @WrapOperation(method = "work", at = @At(value = "INVOKE", target = ABSENT_CALL))
        private void wrap() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class WrapMethodPresent {
        @WrapMethod(method = "helper(I)I")
        private void wrap() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class WrapMethodQuantified {
        @WrapMethod(method = "helper*")
        private void wrap() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class WrapMethodMissing {
        @WrapMethod(method = "helper(J)I")
        private void wrap() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class InjectPresent {
        @Inject(method = "work", at = @At("HEAD"))
        private void inject() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class InjectMissing {
        @Inject(method = "absent", at = @At("HEAD"))
        private void inject() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class ModifyReturnValuePresent {
        @ModifyReturnValue(method = "helper", at = @At("RETURN"))
        private void modify() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class ModifyReturnValueMissing {
        @ModifyReturnValue(method = "absent", at = @At("RETURN"))
        private void modify() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class ModifyExpressionValuePresent {
        @ModifyExpressionValue(method = "work", at = @At(value = "FIELD", target = COUNTER))
        private void modify() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class ModifyExpressionValueMissing {
        @ModifyExpressionValue(method = "work", at = @At(value = "FIELD", target = ABSENT_FIELD))
        private void modify() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class ModifyVariablePresent {
        @ModifyVariable(method = "helper", at = @At("HEAD"), argsOnly = true)
        private void modify() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class ModifyVariableMissing {
        @ModifyVariable(method = "absent", at = @At("HEAD"), argsOnly = true)
        private void modify() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class ModifyArgPresent {
        @ModifyArg(method = "work", at = @At(value = "INVOKE", target = HELPER))
        private void modify() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class ModifyArgMissing {
        @ModifyArg(method = "work", at = @At(value = "INVOKE", target = ABSENT_CALL))
        private void modify() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class ModifyArgsPresent {
        @ModifyArgs(method = "work", at = @At(value = "INVOKE", target = HELPER))
        private void modify() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class ModifyArgsMissing {
        @ModifyArgs(method = "work", at = @At(value = "INVOKE", target = ABSENT_CALL))
        private void modify() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class ShadowFieldPresent {
        @Shadow
        public int counter;
    }

    @Mixin(FixtureTarget.class)
    public static class ShadowFieldMissing {
        @Shadow
        public int absent;
    }

    @Mixin(FixtureTarget.class)
    public static class ShadowMethodPresent {
        @Shadow
        public int helper(int value) {
            return value;
        }
    }

    @Mixin(FixtureTarget.class)
    public static class ShadowMethodMissing {
        @Shadow
        public long helper(long value) {
            return value;
        }
    }

    @Mixin(FixtureTarget.class)
    public static class AccessorPresent {
        @Accessor("counter")
        public int counter() {
            return 0;
        }
    }

    @Mixin(FixtureTarget.class)
    public static class AccessorMissing {
        @Accessor("absent")
        public int absent() {
            return 0;
        }
    }

    @Mixin(FixtureTarget.class)
    public static class InvokerPresent {
        @Invoker("helper")
        public int callHelper(int value) {
            return value;
        }
    }

    @Mixin(FixtureTarget.class)
    public static class InvokerMissing {
        @Invoker("helper")
        public int callHelper(long value) {
            return 0;
        }
    }

    @Mixin(FixtureTarget.class)
    public static class TargetHandlerPresent {
        @WrapOperation(method = HANDLER, at = @At(value = "INVOKE", target = VALUE_OF))
        @TargetHandler(mixin = FOREIGN_MIXIN, name = "handler")
        private void wrap() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class TargetHandlerMissing {
        @WrapOperation(method = HANDLER, at = @At(value = "INVOKE", target = VALUE_OF))
        @TargetHandler(mixin = FOREIGN_MIXIN, name = "absent")
        private void wrap() {
        }
    }

    @Pseudo
    @Mixin(targets = {TARGET, NOWHERE})
    public static class PseudoOneOfTwoPresent {
        @Inject(method = "work", at = @At("HEAD"))
        private void inject() {
        }
    }

    @Pseudo
    @Mixin(targets = NOWHERE)
    public static class PseudoNonePresent {
    }

    @Mixin(targets = NOWHERE)
    public static class TargetClassMissing {
    }

    @Mixin(FixtureTarget.class)
    public static class NewPresent {
        @WrapOperation(method = "work", at = @At(value = "NEW", target = "java/lang/StringBuilder"))
        private void wrap() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class NewMissing {
        @WrapOperation(method = "work", at = @At(value = "NEW", target = "java/util/ArrayList"))
        private void wrap() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class NewByConstructorPresent {
        @WrapOperation(method = "create",
                at = @At(value = "NEW", target = "()Lcom/exoticworlds/compat/fixture/FixtureTarget;"))
        private void wrap() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class OrdinalBeyondTheCalls {
        @WrapOperation(method = "work", at = @At(value = "INVOKE", target = HELPER, ordinal = 1))
        private void wrap() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class BodyFromAnotherClass {
        @WrapOperation(method = "work", at = @At(value = "INVOKE", target = ABS))
        private void wrap() {
        }
    }

    @Mixin(targets = "net.minecraft.core.BlockPos")
    public static class GameTarget {
        @Shadow
        public int absent;
    }

    @Mixin(FixtureTarget.class)
    public static class LoaderPairOnePresent {
        @WrapMethod(method = {"work()V", "method_9999()V"})
        private void wrap() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class LoaderPairNeitherPresent {
        @WrapMethod(method = {"absent()V", "method_9999()V"})
        private void wrap() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class AnyMethodSelector {
        @Inject(method = "*", at = @At("HEAD"))
        private void inject() {
        }
    }

    @Mixin(FixtureTarget.class)
    public static class TargetHandlerShadowCall {
        @WrapOperation(method = HANDLER, at = @At(value = "INVOKE", target = HELPER))
        @TargetHandler(mixin = SHADOWING_MIXIN, name = "handler")
        private void wrap() {
        }
    }

    @Mixin(targets = {TARGET, INTERMEDIARY_TARGET})
    public static class OtherLoaderTarget {
        @Shadow
        public int counter;
    }

    private MixinFixtures() {
    }
}

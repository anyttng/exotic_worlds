package com.exoticworlds.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import com.exoticworlds.compat.MixinTargetCheck.Reason;
import com.exoticworlds.compat.MixinTargetCheck.Refusal;
import com.exoticworlds.compat.fixture.FixtureForeignMixin;
import com.exoticworlds.compat.fixture.MixinFixtures;

class MixinTargetCheckTest {
    private static final ClassLoader LOADER = MixinTargetCheckTest.class.getClassLoader();
    private static final String FOREIGN_MIXIN = FixtureForeignMixin.class.getName().replace('.', '/');

    static Stream<Class<?>> admitted() {
        return Stream.of(MixinFixtures.WrapOperationPresent.class, MixinFixtures.WrapMethodPresent.class,
                MixinFixtures.WrapMethodQuantified.class,
                MixinFixtures.InjectPresent.class, MixinFixtures.ModifyReturnValuePresent.class,
                MixinFixtures.ModifyExpressionValuePresent.class, MixinFixtures.ModifyVariablePresent.class,
                MixinFixtures.ModifyArgPresent.class, MixinFixtures.ModifyArgsPresent.class,
                MixinFixtures.ShadowFieldPresent.class, MixinFixtures.ShadowMethodPresent.class,
                MixinFixtures.AccessorPresent.class, MixinFixtures.InvokerPresent.class,
                MixinFixtures.TargetHandlerPresent.class, MixinFixtures.PseudoOneOfTwoPresent.class,
                MixinFixtures.NewPresent.class, MixinFixtures.NewByConstructorPresent.class,
                MixinFixtures.GameTarget.class, MixinFixtures.LoaderPairOnePresent.class,
                MixinFixtures.AnyMethodSelector.class, MixinFixtures.TargetHandlerShadowCall.class);
    }

    static Stream<Arguments> refused() {
        return Stream.of(
                Arguments.of(MixinFixtures.WrapOperationMissing.class, Reason.MISSING_INVOKE),
                Arguments.of(MixinFixtures.WrapMethodMissing.class, Reason.MISSING_METHOD),
                Arguments.of(MixinFixtures.InjectMissing.class, Reason.MISSING_METHOD),
                Arguments.of(MixinFixtures.ModifyReturnValueMissing.class, Reason.MISSING_METHOD),
                Arguments.of(MixinFixtures.ModifyExpressionValueMissing.class, Reason.MISSING_FIELD_ACCESS),
                Arguments.of(MixinFixtures.ModifyVariableMissing.class, Reason.MISSING_METHOD),
                Arguments.of(MixinFixtures.ModifyArgMissing.class, Reason.MISSING_INVOKE),
                Arguments.of(MixinFixtures.ModifyArgsMissing.class, Reason.MISSING_INVOKE),
                Arguments.of(MixinFixtures.ShadowFieldMissing.class, Reason.MISSING_FIELD),
                Arguments.of(MixinFixtures.ShadowMethodMissing.class, Reason.MISSING_METHOD),
                Arguments.of(MixinFixtures.AccessorMissing.class, Reason.MISSING_FIELD),
                Arguments.of(MixinFixtures.InvokerMissing.class, Reason.MISSING_METHOD),
                Arguments.of(MixinFixtures.TargetHandlerMissing.class, Reason.MISSING_METHOD),
                Arguments.of(MixinFixtures.PseudoNonePresent.class, Reason.MISSING_CLASS),
                Arguments.of(MixinFixtures.TargetClassMissing.class, Reason.MISSING_CLASS),
                Arguments.of(MixinFixtures.NewMissing.class, Reason.MISSING_NEW),
                Arguments.of(MixinFixtures.OrdinalBeyondTheCalls.class, Reason.MISSING_INVOKE),
                Arguments.of(MixinFixtures.BodyFromAnotherClass.class, Reason.MISSING_INVOKE),
                Arguments.of(MixinFixtures.LoaderPairNeitherPresent.class, Reason.MISSING_METHOD));
    }

    @ParameterizedTest
    @MethodSource("admitted")
    void aMixinWhoseEveryMemberIsThereIsAdmitted(Class<?> fixture) {
        assertEquals(List.of(), check(fixture, Map.of()), fixture.getSimpleName() + " names only members its target has");
    }

    @ParameterizedTest
    @MethodSource("refused")
    void aMixinMissingAMemberIsRefusedWithItsReason(Class<?> fixture, Reason reason) {
        List<Refusal> refusals = check(fixture, Map.of());

        assertEquals(1, refusals.size(), fixture.getSimpleName() + " misses exactly one member: " + refusals);
        assertEquals(reason, refusals.getFirst().reason(), fixture.getSimpleName() + " is refused for the wrong reason");
        assertEquals(fixture.getName(), refusals.getFirst().mixin(), "the refusal names the mixin it came from");
    }

    @Test
    void aBodyAnotherClassSuppliesIsReadFromThatClass() {
        assertEquals(List.of(),
                check(MixinFixtures.BodyFromAnotherClass.class,
                        Map.of(MixinFixtures.BodyFromAnotherClass.class.getName(), FOREIGN_MIXIN)),
                "the call the wrap names is in the substitute body, not in the target's own");
    }

    @Test
    void aBodySourceThatIsAbsentFallsBackToTheTarget() {
        List<Refusal> refusals = check(MixinFixtures.WrapOperationPresent.class,
                Map.of(MixinFixtures.WrapOperationPresent.class.getName(), "com/exoticworlds/compat/fixture/Nowhere"));

        assertEquals(List.of(), refusals, "with no substitute on the classpath the target's own body holds the call");
    }

    @Test
    void aTargetSpelledForTheOtherLoaderIsLeftUnchecked() {
        String internal = MixinFixtures.INTERMEDIARY_TARGET.replace('.', '/');
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, internal, null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_PUBLIC, "field_1", "Lnet/minecraft/class_1937;", null, null).visitEnd();
        writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        ClassLoader loader = new ClassLoader(LOADER) {
            @Override
            public InputStream getResourceAsStream(String name) {
                return name.equals(internal + ".class") ? new ByteArrayInputStream(bytes) : super.getResourceAsStream(name);
            }
        };

        assertEquals(List.of(), new MixinTargetCheck(loader, Map.of()).check(MixinFixtures.OtherLoaderTarget.class.getName()),
                "an intermediary build of the target on a Mojmap runtime is the other loader's, not this one's");
    }

    @Test
    void aMixinClassThatIsNotThereIsRefused() {
        List<Refusal> refusals = new MixinTargetCheck(LOADER, Map.of())
                .check("com.exoticworlds.compat.fixture.NoSuchMixin");

        assertEquals(1, refusals.size());
        assertEquals(Reason.MISSING_MIXIN, refusals.getFirst().reason());
    }

    @Test
    void aReasonReadsInTheProbeLineAsLowerCase() {
        assertEquals("missing_field_access", Reason.MISSING_FIELD_ACCESS.toString(),
                "probe values are lower-case with underscores");
    }

    private static List<Refusal> check(Class<?> fixture, Map<String, String> bodySources) {
        return new MixinTargetCheck(LOADER, bodySources).check(fixture.getName());
    }
}

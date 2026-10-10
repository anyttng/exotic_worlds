package com.exoticworlds.compat.c2me;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.SlotAxes;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeEmitter;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeGen;
import com.ishland.c2me.opts.dfc.common.gen.jvm.util.DfcObjectCache;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;

import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;

public final class C2meFoldedNoiseEmitter implements BytecodeEmitter<C2meFoldedNoiseNode> {
    public static final C2meFoldedNoiseEmitter INSTANCE = new C2meFoldedNoiseEmitter();

    private static final String SAMPLE_CLASS = Type.getInternalName(ContextScaledNoise.class);
    private static final String TRANSFORMER_DESC = Type.getDescriptor(WorldFold.class);
    private static final String SLOT_AXES_DESC = Type.getDescriptor(SlotAxes.class);
    private static final String NOISE_HOLDER_DESC = Type.getDescriptor(NoiseHolder.class);

    private static final String SAMPLE_METHOD = "sampleWrapped";
    private static final String SAMPLE_DESC = Type.getMethodDescriptor(
            Type.DOUBLE_TYPE,
            Type.getType(WorldFold.class),
            Type.getType(SlotAxes.class),
            Type.getType(NoiseHolder.class),
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE);

    private static final String WARPED_SAMPLE_METHOD = "sampleWrappedWarped";
    private static final String WARPED_SAMPLE_DESC = Type.getMethodDescriptor(
            Type.DOUBLE_TYPE,
            Type.getType(WorldFold.class),
            Type.getType(NoiseHolder.class),
            Type.INT_TYPE,
            Type.DOUBLE_TYPE,
            Type.INT_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE);

    private static final String LOOP_CLASS = Type.getInternalName(C2meFoldedNoiseLoop.class);

    private static final String FILL_METHOD = "fill";
    private static final String FILL_DESC = Type.getMethodDescriptor(
            Type.VOID_TYPE,
            Type.getType(WorldFold.class),
            Type.getType(SlotAxes.class),
            Type.getType(NoiseHolder.class),
            Type.getType(double[].class),
            Type.getType(double[].class),
            Type.DOUBLE_TYPE,
            Type.getType(double[].class),
            Type.DOUBLE_TYPE,
            Type.getType(double[].class),
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE);

    private static final String WARPED_FILL_METHOD = "fillWarped";
    private static final String WARPED_FILL_DESC = Type.getMethodDescriptor(
            Type.VOID_TYPE,
            Type.getType(WorldFold.class),
            Type.getType(NoiseHolder.class),
            Type.getType(double[].class),
            Type.getType(int[].class),
            Type.getType(int[].class),
            Type.getType(double[].class),
            Type.DOUBLE_TYPE,
            Type.getType(double[].class),
            Type.DOUBLE_TYPE,
            Type.getType(double[].class),
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE);

    // C2ME's own arrangement: the block coordinates as ints, and the result array doubling as the buffer for the
    // first input that needs one.
    private static final int SINGLE_X_LOCAL = 1;
    private static final int SINGLE_Z_LOCAL = 3;
    private static final int RESULT_ARRAY_LOCAL = 1;
    private static final int MULTI_X_LOCAL = 2;
    private static final int MULTI_Z_LOCAL = 4;

    private static final int OBJECT_CACHE_LOCAL = 6;

    private C2meFoldedNoiseEmitter() {
    }

    @Override
    public void doBytecodeGenSingle(C2meFoldedNoiseNode node, BytecodeGen.Context context, InstructionAdapter m,
            BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        String noiseField = context.newField(NoiseHolder.class, node.noise);
        String transformerField = context.newField(WorldFold.class, node.transformer);
        ValuesMethodDefF64 slotXMethod = context.newSingleMethodF64(node.slotX);
        ValuesMethodDefF64 slotYMethod = context.newSingleMethodF64(node.slotY);
        ValuesMethodDefF64 slotZMethod = context.newSingleMethodF64(node.slotZ);

        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, transformerField, TRANSFORMER_DESC);
        if (node.warped()) {
            m.load(0, InstructionAdapter.OBJECT_TYPE);
            m.getfield(context.className, noiseField, NOISE_HOLDER_DESC);
            m.load(SINGLE_X_LOCAL, Type.INT_TYPE);
            context.callDelegateSingle(m, slotYMethod);
            m.load(SINGLE_Z_LOCAL, Type.INT_TYPE);
            context.callDelegateSingle(m, slotXMethod);
            context.callDelegateSingle(m, slotZMethod);
            m.dconst(node.warpDivisor);
            loadScales(m, node);
            m.invokestatic(SAMPLE_CLASS, WARPED_SAMPLE_METHOD, WARPED_SAMPLE_DESC, false);
        } else {
            String slotAxesField = context.newField(SlotAxes.class, node.slotAxes);
            m.load(0, InstructionAdapter.OBJECT_TYPE);
            m.getfield(context.className, slotAxesField, SLOT_AXES_DESC);
            m.load(0, InstructionAdapter.OBJECT_TYPE);
            m.getfield(context.className, noiseField, NOISE_HOLDER_DESC);
            context.callDelegateSingle(m, slotXMethod);
            context.callDelegateSingle(m, slotYMethod);
            context.callDelegateSingle(m, slotZMethod);
            loadScales(m, node);
            m.invokestatic(SAMPLE_CLASS, SAMPLE_METHOD, SAMPLE_DESC, false);
        }

        m.areturn(Type.DOUBLE_TYPE);
    }

    @Override
    public void doBytecodeGenMulti(C2meFoldedNoiseNode node, BytecodeGen.Context context, InstructionAdapter m,
            BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        String noiseField = context.newField(NoiseHolder.class, node.noise);
        String transformerField = context.newField(WorldFold.class, node.transformer);

        ValuesMethodDefF64 slotXMethod = context.newMultiMethodF64(node.slotX);
        ValuesMethodDefF64 slotYMethod = context.newMultiMethodF64(node.slotY);
        ValuesMethodDefF64 slotZMethod = context.newMultiMethodF64(node.slotZ);
        boolean constantX = slotXMethod.isConst();
        boolean constantY = slotYMethod.isConst();
        boolean constantZ = slotZMethod.isConst();

        int arraysNeeded = (constantX ? 0 : 1) + (constantY ? 0 : 1) + (constantZ ? 0 : 1);
        int[] arrays = new int[arraysNeeded];
        if (arraysNeeded >= 1) {
            arrays[0] = RESULT_ARRAY_LOCAL;
        }

        for (int arrayIdx = 1; arrayIdx < arraysNeeded; arrayIdx++) {
            arrays[arrayIdx] = localVarConsumer.createLocalVariable("foldedRes" + arrayIdx,
                    Type.getDescriptor(double[].class));
            m.load(OBJECT_CACHE_LOCAL, InstructionAdapter.OBJECT_TYPE);
            m.load(RESULT_ARRAY_LOCAL, InstructionAdapter.OBJECT_TYPE);
            m.arraylength();
            m.iconst(0);
            m.invokeinterface(
                    Type.getInternalName(DfcObjectCache.class),
                    "getDoubleArray",
                    Type.getMethodDescriptor(Type.getType(double[].class), Type.INT_TYPE, Type.BOOLEAN_TYPE));
            m.store(arrays[arrayIdx], InstructionAdapter.OBJECT_TYPE);
        }

        int filledArrays = 0;
        if (!constantX) {
            context.callDelegateMulti(m, slotXMethod, arrays[filledArrays++]);
        }

        if (!constantY) {
            context.callDelegateMulti(m, slotYMethod, arrays[filledArrays++]);
        }

        if (!constantZ) {
            context.callDelegateMulti(m, slotZMethod, arrays[filledArrays++]);
        }

        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, transformerField, TRANSFORMER_DESC);
        if (!node.warped()) {
            String slotAxesField = context.newField(SlotAxes.class, node.slotAxes);
            m.load(0, InstructionAdapter.OBJECT_TYPE);
            m.getfield(context.className, slotAxesField, SLOT_AXES_DESC);
        }

        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, noiseField, NOISE_HOLDER_DESC);
        m.load(RESULT_ARRAY_LOCAL, InstructionAdapter.OBJECT_TYPE);
        if (node.warped()) {
            m.load(MULTI_X_LOCAL, InstructionAdapter.OBJECT_TYPE);
            m.load(MULTI_Z_LOCAL, InstructionAdapter.OBJECT_TYPE);
        }

        int readArrays = 0;
        readArrays = loadAxis(m, arrays, readArrays, slotXMethod, constantX);
        readArrays = loadAxis(m, arrays, readArrays, slotYMethod, constantY);
        loadAxis(m, arrays, readArrays, slotZMethod, constantZ);

        if (node.warped()) {
            m.dconst(node.warpDivisor);
            loadScales(m, node);
            m.invokestatic(LOOP_CLASS, WARPED_FILL_METHOD, WARPED_FILL_DESC, false);
        } else {
            loadScales(m, node);
            m.invokestatic(LOOP_CLASS, FILL_METHOD, FILL_DESC, false);
        }

        for (int arrayIdx = 1; arrayIdx < arrays.length; arrayIdx++) {
            m.load(OBJECT_CACHE_LOCAL, InstructionAdapter.OBJECT_TYPE);
            m.load(arrays[arrayIdx], InstructionAdapter.OBJECT_TYPE);
            m.invokeinterface(
                    Type.getInternalName(DfcObjectCache.class),
                    "recycle",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(double[].class)));
        }

        m.areturn(Type.VOID_TYPE);
    }

    private static void loadScales(InstructionAdapter m, C2meFoldedNoiseNode node) {
        m.dconst(node.horizontalScale);
        m.dconst(node.vanillaScale);
        m.dconst(node.verticalShare);
    }

    private static int loadAxis(InstructionAdapter m, int[] arrays, int readArrays,
            ValuesMethodDefF64 method, boolean constant) {
        if (constant) {
            m.aconst(null);
            m.dconst(method.constValue());
            return readArrays;
        }

        m.load(arrays[readArrays], InstructionAdapter.OBJECT_TYPE);
        m.dconst(0.0);
        return readArrays + 1;
    }
}

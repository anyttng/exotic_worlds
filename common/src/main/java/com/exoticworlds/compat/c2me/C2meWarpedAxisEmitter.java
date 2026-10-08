package com.exoticworlds.compat.c2me;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.engine.noise.DomainWarp;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeEmitter;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeGen;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;

public final class C2meWarpedAxisEmitter implements BytecodeEmitter<C2meWarpedAxisNode> {
    public static final C2meWarpedAxisEmitter INSTANCE = new C2meWarpedAxisEmitter();

    private static final String WARP_CLASS = Type.getInternalName(DomainWarp.class);
    private static final String LATTICE_DESC = Type.getDescriptor(TranslationLattice.class);

    private static final String WARP_X_METHOD = "applyX";
    private static final String WARP_X_DESC = Type.getMethodDescriptor(
            Type.DOUBLE_TYPE,
            Type.getType(TranslationLattice.class),
            Type.INT_TYPE,
            Type.INT_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE);
    private static final String WARP_Z_METHOD = "applyZ";
    private static final String WARP_Z_DESC = Type.getMethodDescriptor(
            Type.DOUBLE_TYPE,
            Type.getType(TranslationLattice.class),
            Type.INT_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE);

    private static final int SINGLE_X_LOCAL = 1;
    private static final int SINGLE_Z_LOCAL = 3;
    private static final int RESULT_ARRAY_LOCAL = 1;
    private static final int MULTI_X_LOCAL = 2;
    private static final int MULTI_Z_LOCAL = 4;

    private C2meWarpedAxisEmitter() {
    }

    @Override
    public void doBytecodeGenSingle(C2meWarpedAxisNode node, BytecodeGen.Context context, InstructionAdapter m,
            BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        String latticeField = context.newField(TranslationLattice.class, node.lattice);
        ValuesMethodDefF64 shiftMethod = context.newSingleMethodF64(node.shift);
        boolean xAxis = node.axis == CoordinateNode.Axis.X;

        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, latticeField, LATTICE_DESC);
        if (xAxis) {
            m.load(SINGLE_X_LOCAL, Type.INT_TYPE);
        }

        m.load(SINGLE_Z_LOCAL, Type.INT_TYPE);
        context.callDelegateSingle(m, shiftMethod);
        m.dconst(node.divisor);
        invokeWarp(m, xAxis);
        m.areturn(Type.DOUBLE_TYPE);
    }

    @Override
    public void doBytecodeGenMulti(C2meWarpedAxisNode node, BytecodeGen.Context context, InstructionAdapter m,
            BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        String latticeField = context.newField(TranslationLattice.class, node.lattice);
        ValuesMethodDefF64 shiftMethod = context.newMultiMethodF64(node.shift);
        boolean xAxis = node.axis == CoordinateNode.Axis.X;
        boolean constantShift = shiftMethod.isConst();
        if (!constantShift) {
            context.callDelegateMulti(m, shiftMethod, RESULT_ARRAY_LOCAL);
        }

        context.doCountedLoop(m, localVarConsumer, idx -> {
            m.load(RESULT_ARRAY_LOCAL, InstructionAdapter.OBJECT_TYPE);
            m.load(idx, Type.INT_TYPE);
            m.load(0, InstructionAdapter.OBJECT_TYPE);
            m.getfield(context.className, latticeField, LATTICE_DESC);
            if (xAxis) {
                m.load(MULTI_X_LOCAL, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.INT_TYPE);
            }

            m.load(MULTI_Z_LOCAL, InstructionAdapter.OBJECT_TYPE);
            m.load(idx, Type.INT_TYPE);
            m.aload(Type.INT_TYPE);
            if (constantShift) {
                m.dconst(shiftMethod.constValue());
            } else {
                m.load(RESULT_ARRAY_LOCAL, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.DOUBLE_TYPE);
            }

            m.dconst(node.divisor);
            invokeWarp(m, xAxis);
            m.astore(Type.DOUBLE_TYPE);
        });
        m.areturn(Type.VOID_TYPE);
    }

    private static void invokeWarp(InstructionAdapter m, boolean xAxis) {
        if (xAxis) {
            m.invokestatic(WARP_CLASS, WARP_X_METHOD, WARP_X_DESC, false);
        } else {
            m.invokestatic(WARP_CLASS, WARP_Z_METHOD, WARP_Z_DESC, false);
        }
    }
}

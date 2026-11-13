package com.exoticworlds.compat.c2me;

import org.objectweb.asm.commons.InstructionAdapter;

import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeEmitter;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeGen;
import com.ishland.c2me.opts.dfc.common.gen.jvm.emitters.integration.lithostitched.misc.GenericFastNoiseNodeBytecodeEmitter;

public final class C2meFoldedFastNoiseEmitter implements BytecodeEmitter<C2meFoldedFastNoiseNode> {
    public static final C2meFoldedFastNoiseEmitter INSTANCE = new C2meFoldedFastNoiseEmitter();

    private C2meFoldedFastNoiseEmitter() {
    }

    @Override
    public void doBytecodeGenSingle(C2meFoldedFastNoiseNode node, BytecodeGen.Context context, InstructionAdapter m,
            BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        GenericFastNoiseNodeBytecodeEmitter.INSTANCE.doBytecodeGenSingle(node, context, m, localVarConsumer);
    }

    @Override
    public void doBytecodeGenMulti(C2meFoldedFastNoiseNode node, BytecodeGen.Context context, InstructionAdapter m,
            BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        GenericFastNoiseNodeBytecodeEmitter.INSTANCE.doBytecodeGenMulti(node, context, m, localVarConsumer);
    }
}

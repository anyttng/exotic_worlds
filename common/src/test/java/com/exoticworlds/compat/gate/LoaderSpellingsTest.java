package com.exoticworlds.compat.gate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class LoaderSpellingsTest {
    private static final String MOJMAP_CONTEXT = "Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;";
    private static final String INTERMEDIARY_CONTEXT = "Lnet/minecraft/class_6910$class_6912;";

    @Test
    void aMojmapNameWithARemappedDescriptorIsHeldByItsIntermediaryTwin() {
        String compute = "compute(" + INTERMEDIARY_CONTEXT + ")D";
        String intermediary = "method_40464(" + INTERMEDIARY_CONTEXT + ")D";

        assertTrue(LoaderSpellings.twinSelected(compute, List.of(compute, intermediary), intermediary::equals),
                "Loom remaps both descriptors on Fabric, so the name alone tells the two spellings apart");
    }

    @Test
    void anIntermediaryNameWithAMojmapDescriptorIsHeldByItsMojmapTwin() {
        String compute = "compute(" + MOJMAP_CONTEXT + ")D";
        String intermediary = "method_40464(" + MOJMAP_CONTEXT + ")D";

        assertTrue(LoaderSpellings.twinSelected(intermediary, List.of(compute, intermediary), compute::equals),
                "on NeoForge the Mojmap spelling is the one found");
    }

    @Test
    void aTwinThatIsNotFoundHoldsNothing() {
        String compute = "compute(" + INTERMEDIARY_CONTEXT + ")D";
        String intermediary = "method_40464(" + INTERMEDIARY_CONTEXT + ")D";

        assertFalse(LoaderSpellings.twinSelected(compute, List.of(compute, intermediary), any -> false),
                "with neither spelling in the target the selector stays missing");
    }

    @Test
    void twoMojmapNamesAreNotTwins() {
        String compute = "compute(" + INTERMEDIARY_CONTEXT + ")D";
        String other = "sample(" + MOJMAP_CONTEXT + ")D";

        assertFalse(LoaderSpellings.twinSelected(compute, List.of(compute, other), other::equals),
                "a different method in the other namespace's descriptor is not the same method");
    }

    @Test
    void oneNameInTwoDescriptorNamespacesIsATwin() {
        String intermediary = "<init>(Lnet/minecraft/class_1937;)V";
        String mojmap = "<init>(Lnet/minecraft/world/level/Level;)V";

        assertTrue(LoaderSpellings.twinSelected(intermediary, List.of(intermediary, mojmap), mojmap::equals),
                "a constructor is spelled per loader by its descriptor alone");
    }
}

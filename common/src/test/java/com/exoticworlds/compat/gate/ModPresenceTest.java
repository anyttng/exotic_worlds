package com.exoticworlds.compat.gate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment.Side;

import com.exoticworlds.compat.fixture.scan.FixtureCaller;
import com.exoticworlds.compat.fixture.scan.FixtureScanPlugin;
import com.exoticworlds.compat.fixture.shared.FixtureSharedFirst;
import com.exoticworlds.compat.fixture.shared.FixtureSharedPlugin;
import com.exoticworlds.compat.fixture.shared.FixtureSharedSecond;
import com.exoticworlds.compat.fixture.shared.mixin.FixtureSharedFirstMixin;
import com.exoticworlds.compat.fixture.shared.mixin.FixtureSharedSecondMixin;
import com.mojang.logging.LogUtils;

class ModPresenceTest {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String SHIPPED_CLASS = "com/exoticworlds/compat/gate/ModPresence.class";
    private static final String ABSENT_CLASS = "com/exoticworlds/compat/NoSuchModEntryPoint.class";
    private static final String CONFIG = "fixture.mixins.json";
    private static final String SIDED_CONFIG = "fixture.sided.mixins.json";
    private static final String ABSENT_CONFIG = "no_such.mixins.json";
    private static final String FIXTURES = "com.exoticworlds.compat.fixture.MixinFixtures$";
    private static final String MIXIN_PREFIX = "MixinFixtures$";
    private static final String WRAP_PRESENT = "WrapOperationPresent";
    private static final String SHADOW_MISSING = "ShadowFieldMissing";
    private static final String INJECT_PRESENT = "InjectPresent";

    @Test
    void aGateWhoseCoveredMixinsAllHoldOpens() {
        assertTrue(ModPresence.gate(LOGGER, "[test-compat] gate holding_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, mixin -> !mixin.equals(MIXIN_PREFIX + SHADOW_MISSING))
                .build().isAdmitted(), "every mixin the gate covers names only members its target has");
    }

    @Test
    void aGateClosesOnTheOneMixinOfSeveralThatLostAMember() {
        assertFalse(ModPresence.gate(LOGGER, "[test-compat] gate one_moved_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG)
                .build().isAdmitted(), "one covered mixin shadows a field its target no longer declares");
    }

    @Test
    void aMixinTheGateDoesNotCoverNeverClosesIt() {
        assertTrue(ModPresence.gate(LOGGER, "[test-compat] gate narrowed_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, (MIXIN_PREFIX + WRAP_PRESENT)::equals)
                .build().isAdmitted(), "the refused mixin belongs to another gate's set");
    }

    @Test
    void aGateWhoseModIsAbsentClosesWithoutReadingItsConfig() {
        assertFalse(ModPresence.gate(LOGGER, "[test-compat] gate uninstalled_present")
                .probing(ABSENT_CLASS)
                .checking(ABSENT_CONFIG)
                .build().isAdmitted(), "an absent mod closes the gate before its config is opened");
    }

    @Test
    void aGateClosesWhenAnyOfItsResourcesIsAbsent() {
        assertFalse(ModPresence.gate(LOGGER, "[test-compat] gate half_installed_present")
                .probing(SHIPPED_CLASS, ABSENT_CLASS)
                .build().isAdmitted(), "a gate standing on two mods needs both");
    }

    @Test
    void aGateClosesWhenAGateItRequiresRefuses() {
        ModPresence refusing = ModPresence.gate(LOGGER, "[test-compat] gate required_refusing_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, (MIXIN_PREFIX + SHADOW_MISSING)::equals)
                .build();

        assertFalse(ModPresence.gate(LOGGER, "[test-compat] gate requiring_refused_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, (MIXIN_PREFIX + WRAP_PRESENT)::equals)
                .requiring(refusing)
                .build().isAdmitted(), "its own mixin holds, but it casts into what the refused gate's mixins add");
    }

    @Test
    void aGateWhoseRequiredGateAdmitsOpens() {
        ModPresence admitting = ModPresence.gate(LOGGER, "[test-compat] gate required_admitting_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, (MIXIN_PREFIX + INJECT_PRESENT)::equals)
                .build();

        assertTrue(ModPresence.gate(LOGGER, "[test-compat] gate requiring_admitted_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, (MIXIN_PREFIX + WRAP_PRESENT)::equals)
                .requiring(admitting)
                .build().isAdmitted(), "both its own mixin and the required gate's hold");
    }

    @Test
    void aGateRequiringAnyOpensOnTheOneThatPassesWhileThatOneRequiresIt() {
        ModPresence[] root = new ModPresence[1];
        ModPresence absent = ModPresence.gate(LOGGER, "[test-compat] gate any_absent_present")
                .probing(ABSENT_CLASS)
                .build();
        ModPresence shared = ModPresence.gate(LOGGER, "[test-compat] gate any_shared_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, (MIXIN_PREFIX + WRAP_PRESENT)::equals)
                .requiringAny(List.of(() -> absent, () -> root[0]))
                .build();
        root[0] = ModPresence.gate(LOGGER, "[test-compat] gate any_root_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, (MIXIN_PREFIX + INJECT_PRESENT)::equals)
                .requiring(shared)
                .build();

        assertTrue(root[0].isAdmitted(), "the root's own mixin holds and the shared gate it requires opens");
        assertTrue(shared.isAdmitted(), "one of the gates it requires any of passes its own checks");
    }

    @Test
    void aGateRequiringAnyClosesWhenNoneOfThemPasses() {
        ModPresence absent = ModPresence.gate(LOGGER, "[test-compat] gate none_absent_present")
                .probing(ABSENT_CLASS)
                .build();
        ModPresence refusing = ModPresence.gate(LOGGER, "[test-compat] gate none_refusing_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, (MIXIN_PREFIX + SHADOW_MISSING)::equals)
                .build();

        assertFalse(ModPresence.gate(LOGGER, "[test-compat] gate none_shared_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, (MIXIN_PREFIX + WRAP_PRESENT)::equals)
                .requiringAny(List.of(() -> absent, () -> refusing))
                .build().isAdmitted(), "one gate's mod is absent and the other's mixin lost a member");
    }

    @Test
    void aRootRefusedByItsOwnMemberClosesTheGateRequiringAnyOfIt() {
        ModPresence[] root = new ModPresence[1];
        ModPresence shared = ModPresence.gate(LOGGER, "[test-compat] gate refused_shared_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, (MIXIN_PREFIX + WRAP_PRESENT)::equals)
                .requiringAny(List.of(() -> root[0]))
                .build();
        root[0] = ModPresence.gate(LOGGER, "[test-compat] gate refused_root_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, (MIXIN_PREFIX + SHADOW_MISSING)::equals)
                .requiring(shared)
                .build();

        assertFalse(shared.isAdmitted(), "the only gate it requires any of shadows a field its target lacks");
        assertFalse(root[0].isAdmitted(), "the root refuses on its own member");
    }

    @Test
    void aGateWithoutAConfigIsTheBareProbe() {
        assertTrue(ModPresence.gate(LOGGER, "[test-compat] gate shipped_present").probing(SHIPPED_CLASS).build()
                .isAdmitted(), "the mod's own class file resolves through the loader the builder picks");
        assertFalse(ModPresence.gate(LOGGER, "[test-compat] gate absent_present").probing(ABSENT_CLASS).build()
                .isAdmitted(), "no jar on the classpath carries that class file");
    }

    @Test
    void aGateCoversTheConfigMixinsItsPredicateTakesOnBothSides() {
        ModPresence gate = ModPresence.gate(LOGGER, "[test-compat] gate covering_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG, mixin -> !mixin.equals(MIXIN_PREFIX + SHADOW_MISSING))
                .build();

        assertTrue(gate.isCovering(FIXTURES + WRAP_PRESENT), "a common mixin the predicate takes");
        assertTrue(gate.isCovering(FIXTURES + INJECT_PRESENT), "a client mixin the predicate takes");
        assertFalse(gate.isCovering(FIXTURES + SHADOW_MISSING), "the predicate leaves this one to another gate");
        assertFalse(gate.isCovering("com.exoticworlds.mixin." + WRAP_PRESENT), "a mixin of another config's package");
    }

    @Test
    void aDedicatedServerChecksNoClientMixin() {
        assertTrue(sidedGate("server").build(ModPresence.class.getClassLoader(), () -> Side.SERVER).isAdmitted(),
                "Mixin applies no client block on a dedicated server, so its missing member closes nothing");
    }

    @Test
    void anUnknownSideChecksNeitherSidedBlock() {
        assertTrue(sidedGate("unknown").build(ModPresence.class.getClassLoader(), () -> Side.UNKNOWN).isAdmitted(),
                "Mixin applies only the common block where it cannot detect the side");
    }

    @Test
    void aClientChecksItsClientMixins() {
        assertFalse(sidedGate("client").build(ModPresence.class.getClassLoader(), () -> Side.CLIENT).isAdmitted(),
                "the client block applies on a client, and one of its mixins names a missing member");
    }

    @Test
    void outsideAMixinRunEveryBlockIsChecked() {
        assertFalse(sidedGate("unsided").build().isAdmitted(),
                "with no side handed in, the client block is checked as on a client");
    }

    private static ModPresence.Builder sidedGate(String label) {
        return ModPresence.gate(LOGGER, "[test-compat] gate sided_" + label + "_present")
                .probing(SHIPPED_CLASS)
                .checking(SIDED_CONFIG);
    }

    @Test
    void aWithheldMixinIsNeitherCheckedNorApplied() {
        ModPresence gate = ModPresence.gate(LOGGER, "[test-compat] gate withheld_present")
                .probing(SHIPPED_CLASS)
                .checking(CONFIG)
                .withholding(MIXIN_PREFIX + SHADOW_MISSING, () -> true)
                .build();

        assertTrue(gate.isAdmitted(), "the one mixin naming a missing member is withheld, so the rest opens the gate");
        assertFalse(gate.isAdmitting(FIXTURES + SHADOW_MISSING), "a withheld mixin is never applied");
        assertTrue(gate.isAdmitting(FIXTURES + WRAP_PRESENT), "the mixins beside it apply");
    }

    @Test
    void theBareProbeAnswersTheSameAsAGate() {
        assertTrue(ModPresence.hasResource(SHIPPED_CLASS), "the bare probe resolves the shipped class through the same loader");
        assertFalse(ModPresence.hasResource(ABSENT_CLASS), "the bare probe reads an absent class as absent");
    }

    @Test
    void theProbeAsksTheClassLoaderOnce() {
        CountingLoader loader = new CountingLoader(SHIPPED_CLASS);
        ModPresence gate = ModPresence.gate(LOGGER, "[test-compat] gate once_present").probing(SHIPPED_CLASS)
                .build(loader);

        assertTrue(gate.isAdmitted());
        assertTrue(gate.isAdmitted());

        assertEquals(1, loader.lookups, "the second admitted() answers from the value the first one probed");
        assertEquals(SHIPPED_CLASS, loader.asked, "the probe asks for the resource its gate names");
    }

    @Test
    void aGateWhoseCompatClassesReachOnlyWhatTheModHasOpens() {
        assertTrue(scanGate(ScanFixtures.SCAN_CONFIG).build(ScanFixtures.current()).isAdmitted(),
                "every class of the gate's package reaches only members the mod has");
    }

    @Test
    void aCompatClassReachingAMemberTheModLacksClosesTheGate() {
        assertFalse(scanGate(ScanFixtures.SCAN_CONFIG).build(ScanFixtures.older()).isAdmitted(),
                "a class outside the mixins calls a method the older mod lacks");
    }

    @Test
    void aDeclaredOptionalHolderAndANestedGatesPackageAreNotRead() {
        ModPresence gate = scanGate(ScanFixtures.SCAN_CONFIG).optional(ScanFixtures.OPTIONAL_HOLDER)
                .build(ScanFixtures.withoutLater());

        assertTrue(gate.isAdmitted(), "only the optional holder and the nested gate's class name the missing class");
        assertEquals(List.of(FixtureScanPlugin.class.getName(), FixtureCaller.class.getName()),
                gate.scanClasses(), "the gate reads its own package and leaves the nested one to its gate");
    }

    @Test
    void anUndeclaredHolderOfAMissingClassClosesTheGate() {
        assertFalse(scanGate(ScanFixtures.SCAN_CONFIG).build(ScanFixtures.withoutLater()).isAdmitted(),
                "the holder is read like every other class of the package");
    }

    @Test
    void aNestedGateReadsItsOwnPackage() {
        assertFalse(scanGate(ScanFixtures.NESTED_CONFIG).build(ScanFixtures.withoutLater()).isAdmitted(),
                "the nested gate's caller names the class the mod lacks");
    }

    @Test
    void aGateCheckingTheModsPackagesClosesWithoutTheClassIndex() {
        assertFalse(scanGate(ScanFixtures.SCAN_CONFIG).build(ScanFixtures.withoutIndex()).isAdmitted(),
                "a build without the index cannot say which classes the gate stands for");
    }

    @Test
    void gatesSharingAConfigEachReadTheClassesTheyScanAndTheMixinsTheyCover() {
        ModPresence first = firstSharedGate().build(ScanFixtures.withoutLater());
        ModPresence second = secondSharedGate().build(ScanFixtures.withoutLater());

        assertFalse(first.isAdmitted(), "the first gate's class and mixin create the class the mod lacks");
        assertTrue(second.isAdmitted(), "the second gate reads neither of them");
        assertEquals(List.of(FixtureSharedFirst.class.getName(), FixtureSharedFirstMixin.class.getName()),
                first.scanClasses(), "the class its scanning set takes and the mixin it covers");
        assertEquals(List.of(FixtureSharedPlugin.class.getName(), FixtureSharedSecond.class.getName(),
                        FixtureSharedSecondMixin.class.getName()), second.scanClasses(),
                "every other class of the root and the mixin it covers");
    }

    @Test
    void aWithheldMixinIsNotRead() {
        ModPresence gate = sharedGate(ScanFixtures.SHARED_FIRST_MIXIN)
                .scanning(name -> false)
                .withholding(ScanFixtures.SHARED_FIRST_MIXIN, () -> true)
                .build(ScanFixtures.withoutLater());

        assertTrue(gate.isAdmitted(), "the withheld mixin is the gate's only class naming the class the mod lacks");
        assertEquals(List.of(), gate.scanClasses(), "a mixin never applied never runs its body");
    }

    @Test
    void aGateWithoutAScanningSetOwnsItsWholeRoot() {
        ModPresence gate = sharedGate(ScanFixtures.SHARED_FIRST_MIXIN).build(ScanFixtures.current());

        assertEquals(List.of(FixtureSharedPlugin.class.getName(), FixtureSharedFirst.class.getName(),
                        FixtureSharedSecond.class.getName(), FixtureSharedFirstMixin.class.getName(),
                        FixtureSharedSecondMixin.class.getName()), gate.scanOwnedClasses(),
                "beside a sibling gate every class of the root then has two owners");
    }

    private static ModPresence.Builder firstSharedGate() {
        return sharedGate(ScanFixtures.SHARED_FIRST_MIXIN).scanning(ScanFixtures.SHARED_FIRST::equals);
    }

    private static ModPresence.Builder secondSharedGate() {
        return sharedGate(ScanFixtures.SHARED_SECOND_MIXIN)
                .scanning(name -> !name.equals(ScanFixtures.SHARED_FIRST));
    }

    private static ModPresence.Builder sharedGate(String mixin) {
        return ModPresence.gate(LOGGER, "[test-compat] gate shared_present")
                .probing(SHIPPED_CLASS)
                .checking(ScanFixtures.SHARED_CONFIG, mixin::equals)
                .modPackages(ScanFixtures.MOD_PACKAGE);
    }

    private static ModPresence.Builder scanGate(String config) {
        return ModPresence.gate(LOGGER, "[test-compat] gate scan_present")
                .probing(SHIPPED_CLASS)
                .checking(config)
                .modPackages(ScanFixtures.MOD_PACKAGE);
    }

    private static final class CountingLoader extends ClassLoader {
        private final String resolvable;

        private int lookups;
        private String asked;

        private CountingLoader(String resolvable) {
            super(null);
            this.resolvable = resolvable;
        }

        @Override
        public URL getResource(String name) {
            this.lookups++;
            this.asked = name;

            return name.equals(this.resolvable) ? ModPresenceTest.class.getResource("ModPresenceTest.class") : null;
        }
    }
}

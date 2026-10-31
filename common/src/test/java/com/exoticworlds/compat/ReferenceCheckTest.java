package com.exoticworlds.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.MixinTargetCheck.Reason;
import com.exoticworlds.compat.MixinTargetCheck.Refusal;
import com.exoticworlds.compat.fixture.mod.FixtureModApi;
import com.exoticworlds.compat.fixture.mod.FixtureModThing;
import com.exoticworlds.compat.fixture.scan.FixtureCaller;

class ReferenceCheckTest {
    private static final String CALLER = FixtureCaller.class.getName();
    private static final String API = ScanFixtures.internalName(FixtureModApi.class);

    @Test
    void aCompatClassReachingMembersTheModLacksIsRefusedForEach() {
        List<Refusal> refusals = check(ScanFixtures.older(), ScanFixtures.MOD_PACKAGE);

        assertEquals(Set.of(
                        new Refusal(CALLER, API + "#answer()I", Reason.MISSING_METHOD),
                        new Refusal(CALLER, API + "#count:I", Reason.MISSING_FIELD),
                        new Refusal(CALLER, ScanFixtures.internalName(FixtureModThing.class), Reason.MISSING_CLASS)),
                Set.copyOf(refusals), "the older mod has the class but neither member, and not the created class");
        assertEquals(3, refusals.size(), "each missing reference is refused once: " + refusals);
    }

    @Test
    void theSameClassAgainstTheModThatHasItAllIsAdmitted() {
        assertEquals(List.of(), check(ScanFixtures.current(), ScanFixtures.MOD_PACKAGE),
                "every member the caller reaches is in the mod, hashCode through Object");
    }

    @Test
    void aReferenceOutsideTheModsPackagesIsNotChecked() {
        assertEquals(List.of(), check(ScanFixtures.older(), "net/nowhere/"),
                "the caller reaches nothing in the packages the gate names");
    }

    private static List<Refusal> check(ClassLoader loader, String modPackage) {
        return new ReferenceCheck(new ClassFiles(loader), List.of(modPackage)).check(CALLER);
    }
}

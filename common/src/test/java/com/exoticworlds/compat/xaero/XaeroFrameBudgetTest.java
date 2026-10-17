package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.xaero.XaeroFrameBudget.Verdict;

class XaeroFrameBudgetTest {
    private static final ClassLoader LOADER = XaeroFrameBudgetTest.class.getClassLoader();
    private static final String SMALL_CLASS = "com/exoticworlds/compat/xaero/XaeroFrameBudget.class";
    private static final String SMALL_METHOD = "measure";

    @Test
    void theShippedGuiMapExceedsTheBudgetUntouched() {
        byte[] guiMap = XaeroFrameBudget.read(LOADER, XaeroFrameBudget.GUI_MAP_RESOURCE);
        assertNotNull(guiMap, "GuiMap.class is not on the test runtime");

        Verdict verdict = XaeroFrameBudget.measure(LOADER, guiMap, XaeroFrameBudget.RENDER_STATE_METHOD,
                XaeroFrameBudget.BUDGET, false);

        assertTrue(verdict.limitPresent(), "the test runtime's ASM has no frame budget");
        assertFalse(verdict.stripped());
        assertTrue(verdict.exceeded());
    }

    @Test
    void theShippedGuiMapFitsTheBudgetStripped() {
        byte[] guiMap = XaeroFrameBudget.read(LOADER, XaeroFrameBudget.GUI_MAP_RESOURCE);
        assertNotNull(guiMap, "GuiMap.class is not on the test runtime");

        Verdict verdict = XaeroFrameBudget.measure(LOADER, guiMap, XaeroFrameBudget.RENDER_STATE_METHOD,
                XaeroFrameBudget.BUDGET, true);

        assertTrue(verdict.stripped(), "the frames jar is not on the test runtime");
        assertFalse(verdict.exceeded());
    }

    @Test
    void aSmallMethodFitsTheBudget() {
        byte[] small = XaeroFrameBudget.read(LOADER, SMALL_CLASS);
        assertNotNull(small);

        Verdict verdict = XaeroFrameBudget.measure(LOADER, small, SMALL_METHOD, XaeroFrameBudget.BUDGET, false);

        assertTrue(verdict.limitPresent());
        assertFalse(verdict.exceeded());
    }

    @Test
    void anAbsentClassIsNotExceeded() {
        assertEquals(Verdict.ABSENT, XaeroFrameBudget.measure(LOADER, null, SMALL_METHOD, XaeroFrameBudget.BUDGET, true));
    }
}

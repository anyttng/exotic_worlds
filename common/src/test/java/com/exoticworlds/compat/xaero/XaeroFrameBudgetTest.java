package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.xaero.XaeroFrameBudget.Verdict;

class XaeroFrameBudgetTest {
    private static final ClassLoader LOADER = XaeroFrameBudgetTest.class.getClassLoader();
    private static final String SMALL_METHOD = "measure";
    private static final Verdict NO_BUDGET = new Verdict(true, false, false, false);

    @Test
    void theShippedGuiMapIsNotWithheldWithoutABudget() {
        byte[] guiMap = XaeroFrameBudget.read(LOADER, XaeroFrameBudget.GUI_MAP_RESOURCE);
        assertNotNull(guiMap, "GuiMap.class is not on the test runtime");

        assertEquals(NO_BUDGET, XaeroFrameBudget.measure(LOADER, guiMap, XaeroFrameBudget.RENDER_STATE_METHOD,
                XaeroFrameBudget.BUDGET, true));
    }

    @Test
    void anAbsentClassIsNotExceeded() {
        assertEquals(Verdict.ABSENT, XaeroFrameBudget.measure(LOADER, null, SMALL_METHOD, XaeroFrameBudget.BUDGET, true));
    }
}

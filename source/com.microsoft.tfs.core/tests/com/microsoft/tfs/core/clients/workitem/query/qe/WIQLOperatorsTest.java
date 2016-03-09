// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query.qe;

import junit.framework.TestCase;

public class WIQLOperatorsTest extends TestCase {
    // These tests expect to be run in an English locale. WIQLOperators loads
    // its localized strings once into static storage, so we can't switch
    // locales while testing.

    // For the "Today" macro, the invariant string is lower-case "today", and
    // the English local one is upper-case "Today"

    public void testGetLocalizedOperator() {
        // Just chose a few here
        assertEquals("Contains", WIQLOperators.getLocalizedOperator(WIQLOperators.CONTAINS)); //$NON-NLS-1$
        assertEquals("Contains Words", WIQLOperators.getLocalizedOperator(WIQLOperators.CONTAINS_WORDS)); //$NON-NLS-1$
        assertEquals("@Today", WIQLOperators.getLocalizedOperator(WIQLOperators.MACRO_TODAY)); //$NON-NLS-1$
    }

    public void testGetInvariantOperator() {
        // Just chose a few here
        assertEquals(WIQLOperators.CONTAINS, WIQLOperators.getInvariantOperator("Contains")); //$NON-NLS-1$
        assertEquals(WIQLOperators.CONTAINS_WORDS, WIQLOperators.getInvariantOperator("Contains Words")); //$NON-NLS-1$
        assertEquals(WIQLOperators.MACRO_TODAY, WIQLOperators.getInvariantOperator("@Today")); //$NON-NLS-1$
    }

    public void testGetLocalizedTodayMinusMacroInteger() {
        assertEquals("@Today - 0", WIQLOperators.getLocalizedTodayMinusMacro(0)); //$NON-NLS-1$
        assertEquals("@Today - 2", WIQLOperators.getLocalizedTodayMinusMacro(2)); //$NON-NLS-1$
        assertEquals("@Today - 2000", WIQLOperators.getLocalizedTodayMinusMacro(2000)); //$NON-NLS-1$
        assertEquals("@Today - -2000", WIQLOperators.getLocalizedTodayMinusMacro(-2000)); //$NON-NLS-1$
        assertEquals("@Today - 2147483647", WIQLOperators.getLocalizedTodayMinusMacro(Integer.MAX_VALUE)); //$NON-NLS-1$
    }

    public void testGetLocalizedTodayMinusMacro() {
        assertEquals("@Today - 100", WIQLOperators.getLocalizedTodayMinusMacro("@today - 100")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("@Today-100", WIQLOperators.getLocalizedTodayMinusMacro("@today-100")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("@Today-", WIQLOperators.getLocalizedTodayMinusMacro("@today-")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("@Today+1", WIQLOperators.getLocalizedTodayMinusMacro("@today+1")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testGetInvariantTodayMinusMacro() {
        assertEquals("@today - 100", WIQLOperators.getInvariantTodayMinusMacro("@Today - 100")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("@today-100", WIQLOperators.getInvariantTodayMinusMacro("@Today-100")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("@today-", WIQLOperators.getInvariantTodayMinusMacro("@Today-")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("@today+1", WIQLOperators.getInvariantTodayMinusMacro("@Today+1")); //$NON-NLS-1$ //$NON-NLS-2$
    }
}

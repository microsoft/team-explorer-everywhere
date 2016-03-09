// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin.filters;

import java.util.regex.Pattern;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Filters pending changes by regular expressions matched against the change's
 * server item.
 * </p>
 * <p>
 * If this class is initialized with an empty array of expressions, all calls to
 * {@link #passes(PendingChange)} return true (everything passes). A non-empty
 * array during initialization causes only items that match any of the
 * expressions to pass.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class ScopeFilter implements PendingChangeFilter {
    /**
     * Flags used when compiling regular expression strings into {@link Pattern}
     * objects. These can't be changed by callers, but are exposed to enable
     * compatible user-interfaces (which may show regular expression matching
     * previews, for example).
     */
    public static final int EXPRESSION_FLAGS = Pattern.UNICODE_CASE;

    private final Pattern[] patterns;

    /**
     * Create a filter using the given expressions.
     *
     * @param scopeExpressions
     *        the expressions to use to filter. If non-empty, only changes whose
     *        server paths match any of the expressions pass (others do not
     *        pass). If empty, all changes pass.
     */
    public ScopeFilter(final String[] scopeExpressions) {
        Check.notNull(scopeExpressions, "scopeExpressions"); //$NON-NLS-1$

        patterns = new Pattern[scopeExpressions.length];
        for (int i = 0; i < scopeExpressions.length; i++) {
            patterns[i] = Pattern.compile(scopeExpressions[i], EXPRESSION_FLAGS);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean passes(final PendingChange change) {
        return passes(change.getServerItem());
    }

    /**
     * Public for validation by UI code that wishes to pass sample paths and
     * test our engine.
     *
     * @param serverPath
     *        the server path to test (must not be <code>null</code>)
     * @return true if the path passes, false if it does not. See
     *         {@link PendingChangeFilter#passes(PendingChange)} for details.
     */
    public boolean passes(final String serverPath) {
        /*
         * Integer.MIN_VALUE is pass because empty, all other non-negative
         * values are pass-because-of-expression-at-that-index. -1 is the only
         * do-not-pass value.
         */
        return (passesWhich(serverPath) != -1);
    }

    /**
     * Public for validation by UI code that wishes to pass sample paths and
     * test our engine. Exposes which pattern matched.
     *
     * @param serverPath
     *        the server path to test (must not be <code>null</code>)
     * @return -1 if the path did not match any expressions (does not pass), or
     *         the the non-negative index of the expression the path matched. If
     *         there were no configured expressions, {@link Integer#MIN_VALUE}
     *         is returned (the path passes).
     */
    public int passesWhich(final String serverPath) {
        if (patterns.length == 0) {
            // Passes.
            return Integer.MIN_VALUE;
        }

        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i].matcher(serverPath).matches()) {
                // Passes at i.
                return i;
            }
        }

        // Does not pass.
        return -1;
    }
}

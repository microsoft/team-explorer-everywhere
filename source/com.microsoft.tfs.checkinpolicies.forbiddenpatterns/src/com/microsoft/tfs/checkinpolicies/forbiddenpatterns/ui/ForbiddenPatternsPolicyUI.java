// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.forbiddenpatterns.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.checkinpolicies.forbiddenpatterns.ForbiddenPatternsPolicy;
import com.microsoft.tfs.core.checkinpolicies.PolicyContextKeys;
import com.microsoft.tfs.core.checkinpolicies.PolicyEditArgs;

/**
 * A TFS Check-in Policy that prevents files containing forbidden patterns from
 * being checked in.
 * <p>
 * This is the graphical implementation of the policy.
 */
public class ForbiddenPatternsPolicyUI extends ForbiddenPatternsPolicy {
    /**
     * All policy implementations must include a zero-argument constructor, so
     * they can be dynamically created by the policy framework.
     */
    public ForbiddenPatternsPolicyUI() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.checkinpolicies.PolicyBase#edit(com.microsoft.
     * tfs.core.checkinpolicies.PolicyEditArgs)
     */
    @Override
    public boolean edit(final PolicyEditArgs policyEditArgs) {
        final Shell shell = (Shell) policyEditArgs.getContext().getProperty(PolicyContextKeys.SWT_SHELL);

        if (shell == null) {
            return false;
        }

        final Pattern[] existingPatterns = getForbiddenPatterns();
        String[] expressionStrings = new String[existingPatterns.length];

        for (int i = 0; i < existingPatterns.length; i++) {
            expressionStrings[i] = (existingPatterns[i]).pattern();
        }

        final ForbiddenPatternsDialog dialog =
            new ForbiddenPatternsDialog(shell, policyEditArgs.getTeamProject().getName(), expressionStrings);

        if (dialog.open() != IDialogConstants.OK_ID) {
            return false;
        }

        expressionStrings = dialog.getExpressions();

        final List newPatterns = new ArrayList();
        for (int i = 0; i < expressionStrings.length; i++) {
            final Pattern p = makePattern(expressionStrings[i]);
            if (p != null) {
                newPatterns.add(p);
            }
        }

        setForbiddenPatterns((Pattern[]) newPatterns.toArray(new Pattern[newPatterns.size()]));

        return true;
    }
}

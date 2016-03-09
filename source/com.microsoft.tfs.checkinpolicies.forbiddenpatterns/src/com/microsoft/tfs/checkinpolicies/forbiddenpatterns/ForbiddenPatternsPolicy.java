// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.forbiddenpatterns;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.microsoft.tfs.checkinpolicies.forbiddenpatterns.ui.ForbiddenPatternsPolicyUI;
import com.microsoft.tfs.core.checkinpolicies.PolicyBase;
import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyEditArgs;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluationCancelledException;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.checkinpolicies.PolicyType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.util.Check;

/**
 * A TFS Check-in Policy that prevents files containing forbidden patterns from
 * being checked in.
 * <p>
 * See {@link ForbiddenPatternsPolicyUI} for an extension of this class that
 * provides for graphical configuration and evaluation.
 */
public class ForbiddenPatternsPolicy extends PolicyBase {
    private final static String PATTERN_MEMENTO_NAME = "pattern"; //$NON-NLS-1$

    /**
     * Flags used when compiling regular expression strings into {@link Pattern}
     * objects.
     */
    public static final int EXPRESSION_FLAGS = Pattern.UNICODE_CASE;

    private final static PolicyType TYPE =
        new PolicyType(
            "com.teamprise.checkinpolicies.forbiddenpatterns.ForbiddenPatternsPolicy-1", //$NON-NLS-1$

            Messages.getString("ForbiddenPatternsPolicy.Name"), //$NON-NLS-1$

            Messages.getString("ForbiddenPatternsPolicy.ShortDescription"), //$NON-NLS-1$

            Messages.getString("ForbiddenPatternsPolicy.LongDescription"), //$NON-NLS-1$

            Messages.getString("ForbiddenPatternsPolicy.InstallInstructions")); //$NON-NLS-1$

    /**
     * Contains {@link Pattern} instances. Access to this list is synchronized
     * on the list.
     */
    private final List forbiddenPatterns = new ArrayList();

    /**
     * All policy implementations must include a zero-argument constructor, so
     * they can be dynamically created by the policy framework.
     */
    public ForbiddenPatternsPolicy() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.checkinpolicies.PolicyBase#canEdit()
     */
    @Override
    public boolean canEdit() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.checkinpolicies.PolicyBase#edit(com.microsoft.
     * tfs.core .checkinpolicies.PolicyEditArgs)
     */
    @Override
    public boolean edit(final PolicyEditArgs policyEditArgs) {
        /*
         * Extending classes may override.
         */

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.checkinpolicies.PolicyBase#evaluate(com.microsoft
     * .tfs.core.checkinpolicies.PolicyContext)
     */
    @Override
    public PolicyFailure[] evaluate(final PolicyContext context) throws PolicyEvaluationCancelledException {
        final PendingCheckin pc = getPendingCheckin();

        final PendingChange[] checkedChanges = pc.getPendingChanges().getCheckedPendingChanges();

        final List failures = new ArrayList();

        final Pattern[] patterns = getForbiddenPatterns();

        for (int i = 0; i < checkedChanges.length; i++) {
            final PendingChange change = checkedChanges[i];

            for (int j = 0; j < patterns.length; j++) {
                final Pattern pattern = patterns[j];

                if (pattern.matcher(change.getServerItem()).matches()) {
                    final String messageFormat = Messages.getString("ForbiddenPatternsPolicy.ForbiddenRegExFormat"); //$NON-NLS-1$
                    final String message =
                        MessageFormat.format(messageFormat, checkedChanges[i].getServerItem(), pattern.pattern());
                    failures.add(new PolicyFailure(message, this));
                }
            }
        }

        return (PolicyFailure[]) failures.toArray(new PolicyFailure[failures.size()]);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.checkinpolicies.PolicyBase#getPolicyType()
     */
    @Override
    public PolicyType getPolicyType() {
        /*
         * This class statically defines a type which is always appropriate.
         */
        return ForbiddenPatternsPolicy.TYPE;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.checkinpolicies.PolicyBase#loadConfiguration(com
     * .microsoft.tfs.core.memento.Memento)
     */
    @Override
    public void loadConfiguration(final Memento configurationMemento) {
        /*
         * Read our patterns from child mementos.
         */
        synchronized (this.forbiddenPatterns) {
            this.forbiddenPatterns.clear();

            final Memento[] children = configurationMemento.getChildren(PATTERN_MEMENTO_NAME);
            for (int i = 0; i < children.length; i++) {
                final Memento child = children[i];
                if (child != null && child.getTextData() != null && child.getTextData().length() > 0) {
                    final Pattern p = makePattern(child.getTextData());
                    if (p != null) {
                        this.forbiddenPatterns.add(p);
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.checkinpolicies.PolicyBase#saveConfiguration(com
     * .microsoft.tfs.core.memento.Memento)
     */
    @Override
    public void saveConfiguration(final Memento configurationMemento) {
        /*
         * Save our patterns to child mementos.
         */
        synchronized (this.forbiddenPatterns) {
            for (int i = 0; i < this.forbiddenPatterns.size(); i++) {
                final Pattern pattern = (Pattern) this.forbiddenPatterns.get(i);

                final Memento child = configurationMemento.createChild(PATTERN_MEMENTO_NAME);
                child.putTextData(pattern.pattern());
            }
        }
    }

    /**
     * Creates a pattern from an expression using this class's preferred regex
     * flags.
     *
     * @param expression
     *        the expression to compile (not null).
     * @return the pattern, or null if it could not be compiled.
     */
    public static Pattern makePattern(final String expression) {
        Check.notNull(expression, "expression"); //$NON-NLS-1$

        try {
            return Pattern.compile(expression, EXPRESSION_FLAGS);
        } catch (final PatternSyntaxException e) {
            return null;
        }
    }

    /**
     * @return a copy of the forbidden patterns list.
     */
    public Pattern[] getForbiddenPatterns() {
        synchronized (this.forbiddenPatterns) {
            return (Pattern[]) this.forbiddenPatterns.toArray(new Pattern[this.forbiddenPatterns.size()]);
        }
    }

    /**
     * Sets the forbidden patterns from the given array.
     *
     * @param forbiddenPatterns
     *        the array of {@link Pattern}s that should be forbidden (not null).
     */
    public void setForbiddenPatterns(final Pattern[] forbiddenPatterns) {
        Check.notNull(forbiddenPatterns, "forbiddenPatterns"); //$NON-NLS-1$

        synchronized (this.forbiddenPatterns) {
            this.forbiddenPatterns.clear();
            this.forbiddenPatterns.addAll(Arrays.asList(forbiddenPatterns));
        }
    }
}

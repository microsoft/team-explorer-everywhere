// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.build;

import java.text.MessageFormat;

import com.microsoft.tfs.checkinpolicies.build.settings.BuildPolicyConfiguration;
import com.microsoft.tfs.core.checkinpolicies.PolicyBase;
import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyEditArgs;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluationCancelledException;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.checkinpolicies.PolicyType;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.util.Check;

/**
 * A TFS Check-in Policy that ensures the Eclipse workspace builds. The user can
 * configure which kinds of build markers are checked for (JDT, CDT, whatever)
 * and which severities and priorities count as build-breakers (error, warning,
 * info, etc).
 * <p>
 * This class does not directly reference any Eclipse UI classes so it can load
 * an evaluate (even if only with a warning) in non-UI (CLC) environments.
 */
public class BuildPolicy extends PolicyBase {
    /**
     * The name of the memento inside our configuration memento that holds our
     * settings.
     */
    private static final String DIALOG_SETTINGS_MEMENTO_NAME = "buildPolicySettings"; //$NON-NLS-1$

    /**
     * Our static type information.
     */
    private static final PolicyType TYPE = new PolicyType(
        "com.teamprise.checkinpolicies.build.BuildPolicy-1", //$NON-NLS-1$

        Messages.getString("BuildPolicy.Name"), //$NON-NLS-1$

        Messages.getString("BuildPolicy.ShortDescription"), //$NON-NLS-1$

        Messages.getString("BuildPolicy.LongDescription"), //$NON-NLS-1$

        Messages.getString("BuildPolicy.InstallInstructions")); //$NON-NLS-1$

    /**
     * This is our edit-time and run-time configuration.
     */
    private BuildPolicyConfiguration configuration = new BuildPolicyConfiguration();

    /**
     * All policy implementations must include a zero-argument constructor, so
     * they can be dynamically created by the policy framework.
     */
    public BuildPolicy() {
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
    public synchronized boolean edit(final PolicyEditArgs policyEditArgs) {
        /*
         * Extending classes may override.
         */

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.checkinpolicies.PolicyBase#loadConfiguration(com
     * .microsoft.tfs.core.memento.Memento)
     */
    @Override
    public synchronized void loadConfiguration(final Memento configurationMemento) {
        final BuildPolicyConfiguration config = new BuildPolicyConfiguration();
        final Memento child = configurationMemento.getChild(DIALOG_SETTINGS_MEMENTO_NAME);

        if (child != null) {
            config.load(child);
        }

        synchronized (this) {
            configuration = config;
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
    public synchronized void saveConfiguration(final Memento configurationMemento) {
        final Memento child = configurationMemento.createChild(DIALOG_SETTINGS_MEMENTO_NAME);
        synchronized (this) {
            configuration.save(child);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.checkinpolicies.PolicyInstance#evaluate(com.
     * microsoft .tfs.core.checkinpolicies.PolicyContext)
     */
    @Override
    public synchronized PolicyFailure[] evaluate(final PolicyContext context)
        throws PolicyEvaluationCancelledException {
        final String messageFormat = Messages.getString("BuildPolicy.CanOnlyCheckBuildsInEclipseFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, TYPE.getShortDescription());
        System.err.println(message);

        return new PolicyFailure[0];
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
        return BuildPolicy.TYPE;
    }

    /**
     * @return a reference to the current configuration.
     */
    public BuildPolicyConfiguration getConfiguration() {
        synchronized (this) {
            return configuration;
        }
    }

    /**
     * @param configuration
     *        the new configuration to use (not null).
     */
    public void setConfiguration(final BuildPolicyConfiguration configuration) {
        Check.notNull(configuration, "configuration"); //$NON-NLS-1$

        synchronized (this) {
            this.configuration = configuration;
        }
    }
}

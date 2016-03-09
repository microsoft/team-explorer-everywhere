// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.memento.XMLMemento;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A {@link PolicyDefinition} represents a policy that is configured for a team
 * project on a Team Foundation Server. A definition has a policy type, some
 * flags like whether it is enabled, and policy type-specific configuration
 * information.
 * </p>
 * <p>
 * Several {@link PolicyDefinition} objects may be defined on a single team
 * project, and these are collected in a {@link PolicyAnnotation} for
 * serialization.
 * </p>
 * <p>
 * This class does not define a <b>type</b> of checkin policy (see
 * {@link PolicyType}), rather it defines an "attachment" of a policy (and its
 * configuration info) to a Team Project. <p This class is immutable (and
 * therefore thread-safe).
 * </p>
 * <p>
 * A {@link PolicyDefinition} has an evaluation priority (see
 * {@link #getPriority()}), which is a number that represents whether a policy
 * should be evaluated early or later when there are multiple policies definied
 * for a team project. Lower values mean earlier evaluation, and higher values
 * are later. The default priority is 0. Negative numbers can be used.
 * </p>
 * <p>
 * Each definition also contains regular expressions that allow the user to
 * limit which items the policy applies to within a team project. These
 * expressions are stored as an array of strings. An empty array means apply the
 * policy to all items inside the team project. A non-empty array means apply
 * the policy to all items inside the the team project that match any of the
 * expressions in the array.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class PolicyDefinition {
    /**
     * The single data version we read and write.
     */
    public static final int SUPPORTED_VERSION = 1;

    private final static String VERSION_ATTRIBUTE_NAME = "version"; //$NON-NLS-1$
    private final static String ENABLED_ATTRIBUTE_NAME = "enabled"; //$NON-NLS-1$
    private final static String PRIORITY_ATTRIBUTE_NAME = "priority"; //$NON-NLS-1$
    private final static String SCOPE_EXPRESSION_MEMENTO_NAME = "scope"; //$NON-NLS-1$

    private final static String POLICY_TYPE_MEMENTO_NAME = "policy-type"; //$NON-NLS-1$
    private final static String POLICY_TYPE_ID_ATTRIBUTE_NAME = "id"; //$NON-NLS-1$
    private final static String POLICY_TYPE_NAME_ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
    private final static String POLICY_TYPE_SHORT_DESCRIPTION_ATTRIBUTE_NAME = "short-description"; //$NON-NLS-1$
    private final static String POLICY_TYPE_LONG_DESCRIPTION_ATTRIBUTE_NAME = "long-description"; //$NON-NLS-1$
    private final static String POLICY_TYPE_INSTALLATION_INSTRUCTIONS_ATTRIBUTE_NAME = "installation-instructions"; //$NON-NLS-1$

    private final static String CONFIGURATION_DATA_MEMENTO_NAME = "configuration-data"; //$NON-NLS-1$

    private final PolicyType type;
    private final Memento configurationMemento;
    private final boolean enabled;
    private final int priority;
    private final String[] scopeExpressions;

    /**
     * Constructs a {@link PolicyDefinition} to hold the given information.
     */
    public PolicyDefinition(
        final PolicyType type,
        final Memento configurationMemento,
        final boolean enabled,
        final int priority,
        final String[] scopeExpressions) {
        Check.notNull(type, "type"); //$NON-NLS-1$
        Check.notNull(configurationMemento, "configurationMemento"); //$NON-NLS-1$
        Check.notNull(scopeExpressions, "scopeExpressions"); //$NON-NLS-1$

        this.type = type;
        this.configurationMemento = configurationMemento;
        this.enabled = enabled;
        this.priority = priority;
        this.scopeExpressions = scopeExpressions;
    }

    /**
     * Creates a {@link PolicyDefinition} that describes the given instance's
     * type and configuration. The configuration and type data is copied from
     * the given instance immediately.
     *
     * @param instance
     *        a configured {@link PolicyInstance} to create a definition for
     *        (must not be <code>null</code>)
     * @param enabled
     *        true if the policy definition is enabled, false if it is disabled.
     * @param priority
     *        the evaluation priority of this definition (see
     *        {@link #getPriority()}). 0 is a good default.
     */
    public PolicyDefinition(
        final PolicyInstance instance,
        final boolean enabled,
        final int priority,
        final String[] scopeExpressions) {
        Check.notNull(instance, "instance"); //$NON-NLS-1$

        type = instance.getPolicyType();

        // Get the configuration from the instance.
        configurationMemento = new XMLMemento(CONFIGURATION_DATA_MEMENTO_NAME);
        instance.saveConfiguration(configurationMemento);

        this.enabled = enabled;
        this.priority = priority;
        this.scopeExpressions = scopeExpressions;
    }

    /**
     * Reads a full {@link PolicyDefinition} from the given memento and its
     * children.
     *
     * @param definitionMemento
     *        the memento node to read definition information from (must not be
     *        <code>null</code>)
     * @return the definition information read from the memento.
     * @throws PolicySerializationException
     *         if an error occurred reading the definitions
     */
    public static PolicyDefinition fromMemento(final Memento definitionMemento) throws PolicySerializationException {
        Check.notNull(definitionMemento, "definitionMemento"); //$NON-NLS-1$

        final Integer schemaVersion = definitionMemento.getInteger(VERSION_ATTRIBUTE_NAME);
        if (schemaVersion == null) {
            throw new PolicySerializationException("The policy definition did not specify a schema version"); //$NON-NLS-1$
        }

        if (schemaVersion.intValue() != SUPPORTED_VERSION) {
            throw new PolicySerializationException(
                MessageFormat.format(
                    "Policy definition data version {0} can not be read by this definition serializer.", //$NON-NLS-1$
                    schemaVersion.toString()));
        }

        Integer priority = definitionMemento.getInteger(PRIORITY_ATTRIBUTE_NAME);
        if (priority == null) {
            priority = new Integer(0);
        }

        Boolean enabled = definitionMemento.getBoolean(ENABLED_ATTRIBUTE_NAME);
        if (enabled == null) {
            enabled = Boolean.TRUE;
        }

        // Read scope expressions child mementos.
        final List expressions = new ArrayList();
        final Memento[] scopeChildren = definitionMemento.getChildren(SCOPE_EXPRESSION_MEMENTO_NAME);
        for (int i = 0; i < scopeChildren.length; i++) {
            final Memento m = scopeChildren[i];
            if (m != null && m.getTextData() != null && m.getTextData().length() > 0) {
                expressions.add(m.getTextData());
            }
        }
        final String[] scopeExpressions = (String[]) expressions.toArray(new String[expressions.size()]);

        // Read the PolicyType information.
        final Memento policyTypeMemento = definitionMemento.getChild(POLICY_TYPE_MEMENTO_NAME);
        if (policyTypeMemento == null) {
            throw new PolicySerializationException(MessageFormat.format(
                "Could not find the {0} memento", //$NON-NLS-1$
                POLICY_TYPE_MEMENTO_NAME));
        }

        final String id = policyTypeMemento.getString(POLICY_TYPE_ID_ATTRIBUTE_NAME);
        final String name = policyTypeMemento.getString(POLICY_TYPE_NAME_ATTRIBUTE_NAME);
        final String shortDescription = policyTypeMemento.getString(POLICY_TYPE_SHORT_DESCRIPTION_ATTRIBUTE_NAME);
        final String longDescription = policyTypeMemento.getString(POLICY_TYPE_LONG_DESCRIPTION_ATTRIBUTE_NAME);
        final String installationInstructions =
            policyTypeMemento.getString(POLICY_TYPE_INSTALLATION_INSTRUCTIONS_ATTRIBUTE_NAME);

        if (id == null || id.length() == 0) {
            throw new PolicySerializationException("Policy had a null or empty id"); //$NON-NLS-1$
        }

        if (name == null || name.length() == 0) {
            throw new PolicySerializationException("Policy had a null or empty name"); //$NON-NLS-1$
        }

        if (shortDescription == null) {
            throw new PolicySerializationException("Policy had a null or empty short description"); //$NON-NLS-1$
        }

        if (longDescription == null) {
            throw new PolicySerializationException("Policy had a null or empty long description"); //$NON-NLS-1$
        }

        // Get the configuration element.
        final Memento configurationMemento = definitionMemento.getChild(CONFIGURATION_DATA_MEMENTO_NAME);
        if (configurationMemento == null) {
            throw new PolicySerializationException(MessageFormat.format(
                "Could not find the {0} memento", //$NON-NLS-1$
                CONFIGURATION_DATA_MEMENTO_NAME));
        }

        return new PolicyDefinition(
            new PolicyType(id, name, shortDescription, longDescription, installationInstructions),
            configurationMemento,
            enabled.booleanValue(),
            priority.intValue(),
            scopeExpressions);
    }

    /**
     * Writes this definition's data into the given empty {@link Memento}.
     *
     * @param definitionMemento
     *        the empty memento to write this definition's data into (must not
     *        be <code>null</code>)
     */
    public void toMemento(final Memento definitionMemento) {
        Check.notNull(definitionMemento, "definitionMemento"); //$NON-NLS-1$

        definitionMemento.putInteger(VERSION_ATTRIBUTE_NAME, SUPPORTED_VERSION);
        definitionMemento.putInteger(PRIORITY_ATTRIBUTE_NAME, getPriority());
        definitionMemento.putBoolean(ENABLED_ATTRIBUTE_NAME, isEnabled());

        // Write scope expressions child mementos.
        for (int i = 0; i < scopeExpressions.length; i++) {
            final String expression = scopeExpressions[i];
            if (expression != null && expression.length() > 0) {
                final Memento scopeMemento = definitionMemento.createChild(SCOPE_EXPRESSION_MEMENTO_NAME);
                scopeMemento.putTextData(expression);
            }
        }

        // Write the type information.
        final Memento policyTypeMemento = definitionMemento.createChild(POLICY_TYPE_MEMENTO_NAME);

        policyTypeMemento.putString(POLICY_TYPE_ID_ATTRIBUTE_NAME, type.getID());
        policyTypeMemento.putString(POLICY_TYPE_NAME_ATTRIBUTE_NAME, type.getName());
        policyTypeMemento.putString(POLICY_TYPE_SHORT_DESCRIPTION_ATTRIBUTE_NAME, type.getShortDescription());
        policyTypeMemento.putString(POLICY_TYPE_LONG_DESCRIPTION_ATTRIBUTE_NAME, type.getLongDescription());
        policyTypeMemento.putString(
            POLICY_TYPE_INSTALLATION_INSTRUCTIONS_ATTRIBUTE_NAME,
            type.getInstallationInstructions());

        // Create a child and copy the data from the memento we already have
        // into it.
        final Memento configurationMemento = definitionMemento.createChild(CONFIGURATION_DATA_MEMENTO_NAME);
        configurationMemento.putMemento(this.configurationMemento);
    }

    /**
     * @return true if this definition is enabled (policy is enforced) or false
     *         if it is disabled (policy is not enforced).
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the evaluation priority for this definition. Definitions with
     * priorities are evaluated before ones with higher priorities. Definitions
     * at equal priorities are evaluated in an undefined order.
     *
     * @return the evaluation priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @return the policy type this definition is for.
     */
    public PolicyType getType() {
        return type;
    }

    /**
     * Gets the regular expression strings that define the scope of this policy
     * definition (for items inside a team project). An empty array means apply
     * the policy to all items in the team project. A non-empty array means
     * apply the policy to all items inside the team project that match any of
     * the expressions in the array.
     *
     * @return the regular expression strings that define which items inside a
     *         team project this policy applies to (empty array means apply to
     *         all).
     */
    public String[] getScopeExpressions() {
        return scopeExpressions;
    }

    /**
     * @return the configuration data in the form of a {@link Memento} that this
     *         definition uses.
     */
    public Memento getConfigurationMemento() {
        return configurationMemento;
    }
}

// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Defines a kind of check-in policy. Policy implementations (classes on disk
 * that implement {@link PolicyInstance}) must declare their policy type, and
 * each type is identified by the type ID.
 * </p>
 * <p>
 * Implementations whose policy types have equal IDs are considered
 * <em>compatible</em>: they should read and write each others' configuration
 * information and evaluate the same rules during check-in.
 * </p>
 * <p>
 * Policies configured on Team Projects ("defined") indicate the associated
 * policy type so they can be loaded by clients (using the ID to find
 * implementations).
 * </p>
 * <p>
 * {@link PolicyLoader} is responsible for loading implementations given
 * appropriate type information.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class PolicyType {
    private final String id;
    private final String name;
    private final String shortDescription;
    private final String longDescription;
    private final String installationInstructions;

    /**
     * Constructs a fully-specified policy type.
     *
     * @param id
     *        a free-form string that uniquely identifies a kind of check-in
     *        policy. All configuration-compatible policy implementations that
     *        enforce the same set of check-in rules can be given the same type
     *        ID, so they can read each others configuration data. It's a good
     *        idea to include some kind of version identifier in a type ID, but
     *        it is not required. (must not be <code>null</code> or empty)
     * @param name
     *        the name of this policy (e.g. "Work Items"). This text is
     *        presented to the user in space-constrained places like lists and
     *        tables. (must not be <code>null</code> or empty)
     * @param shortDescription
     *        a short description of what this type of checkin policy does (e.g.
     *        "Require associated work items."). This text is presented to the
     *        user when he chooses a check-in policy type to define for a team
     *        project, or configures an existing definition. (must not be
     *        <code>null</code> or empty)
     * @param longDescription
     *        a longer description of what this type of checkin policy does
     *        (e.g. "This policy requires that one or more work items be
     *        associated with every check-in."). This text is shown to the user
     *        when selecting a policy implementation for definition on a team
     *        project. (must not be <code>null</code>)
     * @param installationInstructions
     *        instructions on how to install this policy implementation. This is
     *        shown to users when this type of policy is defined on a team
     *        project, but a client program cannot find an implementation. (must
     *        not be <code>null</code>)
     */
    public PolicyType(
        final String id,
        final String name,
        final String shortDescription,
        final String longDescription,
        final String installationInstructions) {
        super();

        Check.notNullOrEmpty(id, "id"); //$NON-NLS-1$
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$
        Check.notNullOrEmpty(shortDescription, "shortDescription"); //$NON-NLS-1$
        Check.notNull(longDescription, "longDescription"); //$NON-NLS-1$
        Check.notNull(installationInstructions, "installationInstructions"); //$NON-NLS-1$

        this.id = id;
        this.name = name;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.installationInstructions = installationInstructions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof PolicyType == false) {
            return false;
        }
        if (o == this) {
            return true;
        }

        // Only need to test ID.
        return ((PolicyType) o).id.equals(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return id + " [" + getShortDescription() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * @return a string that uniquely identifies a specific type of check-in
     *         policy. (never <code>null</code> or empty)
     *
     * @see PolicyType#PolicyType(String, String, String, String, String)
     */
    public String getID() {
        return id;
    }

    /**
     * @return the name of the policy (e.g. "Work Items"). (never
     *         <code>null</code> or empty)
     *
     * @see PolicyType#PolicyType(String, String, String, String, String)
     */
    public String getName() {
        return name;
    }

    /**
     * @return a short description of what this type of checkin policy does
     *         (e.g. "Require associated work items.") (never <code>null</code>
     *         or empty)
     *
     * @see PolicyType#PolicyType(String, String, String, String, String)
     */
    public String getShortDescription() {
        return shortDescription;
    }

    /**
     * @return a longer description of what this type of checkin policy does
     *         (e.g. "This policy requires that one or more work items be
     *         associated with every check-in.") (never <code>null</code>)
     *
     * @see PolicyType#PolicyType(String, String, String, String, String)
     */
    public String getLongDescription() {
        return longDescription;
    }

    /**
     * @return instructions to the end-user on how to install this type of
     *         checkin policy. (never <code>null</code>)
     *
     * @see PolicyType#PolicyType(String, String, String, String, String)
     */
    public String getInstallationInstructions() {
        return installationInstructions;
    }
}

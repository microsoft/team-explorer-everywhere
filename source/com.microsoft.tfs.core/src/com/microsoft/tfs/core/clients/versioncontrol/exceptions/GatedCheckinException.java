// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when the server rejects a check-in because a gated build
 * definition convers one or more of the pending changes.
 *
 * @since TEE-SDK-10.1
 */
public class GatedCheckinException extends ActionDeniedBySubscriberException {
    public Map<String, String> affectedBuildDefinitions = null;
    public String[] affectedBuildDefinitionNames = null;
    public String[] affectedBuildDefinitionURIs = null;

    public GatedCheckinException(final ActionDeniedBySubscriberException e) {
        super(e.getMessage());
        setProperties(e.getProperties());
    }

    /**
     * Gets a collection of (Name, Uri) pairs defining the build definitions
     * which blocked the checkin attempt. This property is always valid.
     */
    public Map<String, String> getAffectedBuildDefinitions() {
        if (affectedBuildDefinitions == null) {
            affectedBuildDefinitions = new HashMap<String, String>();

            final String[] definitionURIs = getProperties().getStringArrayProperty("AffectedBuildDefinitionUris"); //$NON-NLS-1$
            final String[] definitionNames = getProperties().getStringArrayProperty("AffectedBuildDefinitionNames"); //$NON-NLS-1$

            if (definitionURIs != null && definitionNames != null) {
                if (definitionURIs.length == definitionNames.length) {
                    for (int i = 0; i < definitionURIs.length; i++) {
                        affectedBuildDefinitions.put(definitionNames[i], definitionURIs[i]);
                    }
                }

                affectedBuildDefinitionNames = definitionNames;
                affectedBuildDefinitionURIs = definitionURIs;
            } else {
                affectedBuildDefinitionNames = new String[0];
                affectedBuildDefinitionURIs = new String[0];
            }
        }
        return affectedBuildDefinitions;
    }

    public String[] getAffectedBuildDefinitionNames() {
        if (affectedBuildDefinitionNames == null) {
            getAffectedBuildDefinitions();
        }

        return affectedBuildDefinitionNames;
    }

    public String[] getAffectedBuildDefinitionURIs() {
        if (affectedBuildDefinitionURIs == null) {
            getAffectedBuildDefinitions();
        }

        return affectedBuildDefinitionURIs;
    }

    /**
     * Gets the check-in ticket provided by the server for submitting a
     * CheckInShelveset build request. This ticket should be used when
     * requesting a build in response to this exception to ensure the ability to
     * submit even if the caller does not have the normally required
     * permissions.
     *
     * @return The gated checkin ticket.
     */
    public String getCheckinTicket() {
        return getProperties().getStringProperty("CheckInTicket"); //$NON-NLS-1$
    }

    /**
     * Gets the name of the shelveset that was created on behalf of the account
     * making the checkin. If a shelveset could not be created for any reason
     * this value will be null. This property is valid when SubCode is 1 or 3.
     *
     * @return The shelveset name.
     */
    public String getShelvesetName() {
        return getProperties().getStringProperty("ShelvesetName"); //$NON-NLS-1$
    }

    /**
     * Gets a value indicating whether or not the user that performed the
     * checkin has the required permissions to override the gated checkin
     * policy. This property is always valid.
     *
     * @return True if the user has override permission.
     */
    public boolean getOverridePermission() {
        return getProperties().getBooleanProperty("HasOverridePermission"); //$NON-NLS-1$
    }

    /**
     * Gets the sub code for this exception. The sub code indicates what
     * action(s) were performed on the server to aid the client in taking the
     * appropriate corrective action.
     *
     * @return The sub code for this exception
     */
    public int getSubCode() {
        return getProperties().getIntProperty("SubCode"); //$NON-NLS-1$
    }

    /**
     * Gets the ID of the queued build for this gated check-in if the build was
     * queued by the server. If the server did not queue a build, returns 0.
     *
     * @return the queued build ID if this build was queued by the server,
     *         otherwise 0.
     */
    public int getQueueID() {
        if (getProperties().hasIntProperty("QueueId")) //$NON-NLS-1$
        {
            return getProperties().getIntProperty("QueueId"); //$NON-NLS-1$
        }

        return 0;
    }
}

// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

import ms.ws._ActionDefinition;
import ms.ws._SecurityNamespaceDescription;

/**
 * Class for describing the details of a SecurityNamespace
 *
 * @threadsafety unknown
 */
public class SecurityNamespaceDescription extends WebServiceObjectWrapper implements Cloneable {
    public SecurityNamespaceDescription(final _SecurityNamespaceDescription webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Creates a SecurityNamespaceDescription which can be used to create a
     * SecurityNamespace
     *
     * @param namespaceId
     *        The id that uniquely identifies the SecurityNamespace.
     * @param name
     *        The non-localized name for the SecurityNamespace that will be used
     *        for things like the command-line.
     * @param displayName
     *        The localized display name for the SecurityNamespace.
     * @param databaseCategory
     *        This is the database category that describes where the security
     *        information for this SecurityNamespace should be stored.
     * @param separatorValue
     *        If the security tokens this namespace will be operating on need to
     *        be split on certain characters to determine its elements that
     *        character should be specified here. If not, this value must be the
     *        null character.
     * @param elementLength
     *        If the security tokens this namespace will be operating on need to
     *        be split on certain character lengths to determine its elements,
     *        that length should be specified here. If not, this value must be
     *        -1.
     * @param structure
     *        The structure that this SecurityNamespace will use to organize its
     *        tokens. If this namespace is hierarchical, either the
     *        separatorValue or the elementLength parameter must have a
     *        non-default value.
     * @param writePermission
     *        The permission bits needed by a user in order to modify security
     *        data in this SecurityNamespace.
     * @param readPermission
     *        The permission bits needed by a user in order to read security
     *        data in this SecurityNamespace.
     * @param actions
     *        The list of actions that this SecurityNamespace is responsible for
     *        securing.
     */
    public SecurityNamespaceDescription(
        final GUID namespaceId,
        final String name,
        final String displayName,
        final String databaseCategory,
        final char separatorValue,
        final int elementLength,
        final SecurityNamespaceStructure structure,
        final int writePermission,
        final int readPermission,
        final ActionDefinition[] actions) {
        // extensionType seems unused in the VS client
        // separatorValue is an int in the WSO: the UTF-16 code point of the
        // char
        this(
            new _SecurityNamespaceDescription(
                namespaceId.getGUIDString(),
                name,
                displayName,
                separatorValue,
                elementLength,
                writePermission,
                readPermission,
                databaseCategory,
                structure.getValue(),
                null /* extensionType */,
                actions != null ? (_ActionDefinition[]) WrapperUtils.unwrap(_ActionDefinition.class, actions)
                    : new _ActionDefinition[0]));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _SecurityNamespaceDescription getWebServiceObject() {
        return (_SecurityNamespaceDescription) webServiceObject;
    }

    /**
     * The structure that this SecurityNamespace will use to organize its
     * tokens.
     */
    public SecurityNamespaceStructure getNamespaceStructure() {
        return SecurityNamespaceStructure.fromInteger(getWebServiceObject().getStructure());
    }

    /**
     * The display name for the SecurityNamespace.
     */
    public String getDisplayName() {
        final String s = getWebServiceObject().getDisplayName();

        if (s == null || s.length() == 0) {
            return getName();
        }

        return s;
    }

    /**
     * The id that uniquely identifies the SecurityNamespace.
     */
    public GUID getNamespaceId() {
        return new GUID(getWebServiceObject().getNamespaceId());
    }

    /**
     * The non-localized name for the SecurityNamespace that will be used for
     * things like the command-line.
     */
    public String getName() {
        return getWebServiceObject().getName();
    }

    /**
     * If the security tokens this namespace will be operating on need to be
     * split on certain characters to determine its elements that character
     * should be specified here. If not, this value will be the null character.
     */
    public char getSeparatorValue() {
        return (char) getWebServiceObject().getSeparator();
    }

    /**
     * If the security tokens this namespace will be operating on need to be
     * split on certain character lengths to determine its elements, that length
     * should be specified here. If not, this value will be -1.
     */
    public int getElementLength() {
        return getWebServiceObject().getElementLength();
    }

    /**
     * The permission bits needed by a user in order to modify security data on
     * the SecurityNamespace.
     */
    public int getWritePermission() {
        return getWebServiceObject().getWritePermission();
    }

    /**
     * The permission bits needed by a user in order to read security data on
     * the SecurityNamespace.
     */
    public int getReadPermission() {
        return getWebServiceObject().getReadPermission();
    }

    /**
     * This is the database category that describes where the security
     * information for this SecurityNamespace should be stored.
     */
    public String getDatabaseCategory() {
        return getWebServiceObject().getDatabaseCategory();
    }

    /**
     * The list of actions that this SecurityNamespace is responsible for
     * securing.
     */
    public ActionDefinition[] getActions() {
        return (ActionDefinition[]) WrapperUtils.wrap(ActionDefinition.class, getWebServiceObject().getActions());
    }

    /**
     * Returns the bit mask that corresponds to the action name or 0 if it is
     * not an action defined in this SecurityNamespace.
     *
     * @param actionName
     *        The non-localized name for the action.
     * @return The bit mask that corresponds to the action name or 0 if it is
     *         not an action defined in this namespace.
     */
    public int getBitmaskForAction(final String actionName) {
        Check.notNull(actionName, "actionName"); //$NON-NLS-1$

        final _ActionDefinition[] actions = getWebServiceObject().getActions();

        if (actions != null) {
            for (final _ActionDefinition action : actions) {
                if (actionName.equalsIgnoreCase(action.getName())) {
                    return action.getBit();
                }
            }
        }

        return 0;
    }

    /**
     * Returns the action name for the bitmask or String.Empty if the bitmask
     * doesn't correspond to an action defined in this SecurityNamespace.
     *
     * @param bitmask
     *        The bitmask for a single permission whose action name should be
     *        returned. Note, the bitmask should only apply to one action. If 1
     *        = Read and 2 = Write then 1 and 2 are valid values but 3 is not.
     * @return The action name for the bitmask or String.Empty if the bitmask
     *         doesn't correspond to an action defined in this
     *         SecurityNamespace.
     */
    public String getActionNameForBitmask(final int bitmask) {
        final _ActionDefinition[] actions = getWebServiceObject().getActions();

        for (final _ActionDefinition action : actions) {
            if (action.getBit() == bitmask) {
                return action.getName();
            }
        }

        return ""; //$NON-NLS-1$
    }

    /**
     * Returns the action display name for the bitmask or String.Empty if the
     * bitmask doesn't correspond to an action.
     *
     * @param bitmask
     *        The bitmask for a single permission whose action display name
     *        should be returned. Note, the bitmask should only apply to one
     *        action. If 1 = Read and 2 = Write then 1 and 2 are valid values
     *        but 3 is not.
     * @return The action display name for the bitmask or String.Empty if the
     *         bitmask doesn't correspond to an action.
     */
    public String getActionDisplayNameForBitmask(final int bitmask) {
        final _ActionDefinition[] actions = getWebServiceObject().getActions();

        for (final _ActionDefinition action : actions) {
            if (action.getBit() == bitmask) {
                return action.getDisplayName();
            }
        }

        return ""; //$NON-NLS-1$
    }

    /**
     * @return Returns a copy of the description.
     */
    @Override
    public SecurityNamespaceDescription clone() {
        ActionDefinition[] actions = getActions();
        if (actions == null) {
            actions = new ActionDefinition[0];
        }

        return new SecurityNamespaceDescription(
            getNamespaceId(),
            getName(),
            getDisplayName(),
            getDatabaseCategory(),
            getSeparatorValue(),
            getElementLength(),
            getNamespaceStructure(),
            getWritePermission(),
            getReadPermission(),
            actions);
    }
}
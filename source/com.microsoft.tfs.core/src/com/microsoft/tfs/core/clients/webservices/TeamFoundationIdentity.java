// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

import ms.ws._IdentityDescriptor;
import ms.ws._KeyValueOfStringString;
import ms.ws._PropertyValue;
import ms.ws._TeamFoundationIdentity;

/**
 * Wrapper class for the {@link _TeamFoundationIdentity} proxy object.
 *
 * See {@link IdentityAttributeTags} for property names for use with
 * {@link #getAttribute(String, String)} and
 * {@link #setAttribute(String, String)}.
 *
 * @see IdentityAttributeTags
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class TeamFoundationIdentity extends WebServiceObjectWrapper {
    /*
     * Properties are kept in these maps instead of in the web service object to
     * support a more convenient API for accessing/updating them. The maps are
     * updated from the web service object's properties when constructed, but
     * changes to these maps are NOT reflected into the web service object for
     * sending to the server. Users must get the changed properties explicitly
     * from this object.
     */

    private final Map<String, Object> properties = new HashMap<String, Object>();
    private final Map<String, Object> localProperties = new HashMap<String, Object>();

    private final Set<String> modifiedPropertiesLog = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> modifiedLocalPropertiesLog = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    private String clientSideUniqueName;

    public TeamFoundationIdentity(
        final IdentityDescriptor descriptor,
        final String displayName,
        final boolean isActive,
        final IdentityDescriptor[] members,
        final IdentityDescriptor[] memberOf) {
        /*
         * Use empty attributes, properties, local properties in the wrapped
         * object. These are updated when the web service object is retrieved.
         */
        this(
            new _TeamFoundationIdentity(
                displayName,
                false,
                isActive,
                GUID.EMPTY.getGUIDString(),
                null,
                IdentityConstants.ACTIVE_UNIQUE_ID,
                descriptor.getWebServiceObject(),
                new _KeyValueOfStringString[0],
                new _PropertyValue[0],
                new _PropertyValue[0],
                (members != null) ? (_IdentityDescriptor[]) WrapperUtils.unwrap(_IdentityDescriptor.class, members)
                    : new _IdentityDescriptor[0],
                (memberOf != null) ? (_IdentityDescriptor[]) WrapperUtils.unwrap(_IdentityDescriptor.class, memberOf)
                    : new _IdentityDescriptor[0]));

        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$
        Check.notNull(displayName, "displayName"); //$NON-NLS-1$

        /*
         * Nothing to put in the properties maps.
         */
    }

    /**
     * Wrapper constructor
     */
    public TeamFoundationIdentity(final _TeamFoundationIdentity webServiceObject) {
        super(webServiceObject);

        /*
         * Pull the values from the web service object into the wrapper's maps.
         * Legacy attributes are merged into properties.
         */

        final _PropertyValue[] wsoProperties = webServiceObject.getProperties();
        if (wsoProperties != null && wsoProperties.length > 0) {
            // Dev11+ server response to Dev11+ clients.
            for (final _PropertyValue property : wsoProperties) {
                properties.put(property.getPname(), property.getVal());
            }
        }

        final _PropertyValue[] wsoLocalProperties = webServiceObject.getLocalProperties();
        if (wsoLocalProperties != null && wsoLocalProperties.length > 0) {
            // Dev11+ server response to Dev11+ clients.
            for (final _PropertyValue property : wsoLocalProperties) {
                localProperties.put(property.getPname(), property.getVal());
            }
        }

        final _KeyValueOfStringString[] wsoAttributes = webServiceObject.getAttributes();
        if (wsoAttributes != null && wsoAttributes.length > 0) {
            // Dev10 server response.
            for (final _KeyValueOfStringString attribute : wsoAttributes) {
                properties.put(attribute.getKey(), attribute.getValue());
            }
        }

        /*
         * Blank the web service objects to save space.
         */
        getWebServiceObject().setProperties(null);
        getWebServiceObject().setLocalProperties(null);
        getWebServiceObject().setAttributes(null);
    }

    /**
     * Returns the wrapped proxy object.
     */
    public synchronized _TeamFoundationIdentity getWebServiceObject() {
        /*
         * The wrapped object is not updated with properties and attributes
         * because we don't send them to the server that way. They must be
         * explicitly retrieved from this class.
         */
        return (_TeamFoundationIdentity) webServiceObject;
    }

    /**
     * Property accessor. Will return null if not found.
     */
    public synchronized boolean tryGetProperty(final String name, final AtomicReference<Object> value) {
        return tryGetProperty(IdentityPropertyScope.BOTH, name, value);
    }

    /**
     * Property accessor. Will return null if not found.
     */
    public synchronized boolean tryGetProperty(
        final IdentityPropertyScope propertyScope,
        final String name,
        final AtomicReference<Object> value) {
        if (propertyScope == IdentityPropertyScope.LOCAL) {
            if (localProperties.containsKey(name)) {
                value.set(localProperties.get(name));
                return true;
            }
            return false;
        } else if (propertyScope == IdentityPropertyScope.GLOBAL) {
            if (properties.containsKey(name)) {
                value.set(properties.get(name));
                return true;
            }
            return false;
        } else {
            if (!tryGetProperty(IdentityPropertyScope.LOCAL, name, value) || value.get() == null) {
                return tryGetProperty(IdentityPropertyScope.GLOBAL, name, value);
            }
            return true;
        }
    }

    /**
     * Property accessor. Will throw if not found.
     */
    public synchronized Object getProperty(final String name) {
        return getProperty(IdentityPropertyScope.BOTH, name);
    }

    /**
     * Property accessor. Will throw if not found.
     */
    public synchronized Object getProperty(final IdentityPropertyScope propertyScope, final String name) {
        if (propertyScope == IdentityPropertyScope.LOCAL) {
            return localProperties.get(name);
        } else if (propertyScope == IdentityPropertyScope.GLOBAL) {
            return properties.get(name);
        } else {
            final AtomicReference<Object> value = new AtomicReference<Object>();
            if (!tryGetProperty(IdentityPropertyScope.LOCAL, name, value) || value.get() == null) {
                return properties.get(name);
            }
            return value.get();
        }
    }

    /**
     * Property bag. This could be useful, for example if consumer has to
     * iterate through current properties and modify / remove some based on
     * pattern matching property names.
     * <p>
     * This method differs from the Visual Studio implementation in that it
     * returns a read-only collection.
     *
     */
    public synchronized Iterable<Entry<String, Object>> getProperties() {
        return getProperties(IdentityPropertyScope.BOTH);
    }

    /**
     * Property bag. This could be useful, for example if consumer has to
     * iterate through current properties and modify / remove some based on
     * pattern matching property names.
     * <p>
     * This method differs from the Visual Studio implementation in that it
     * returns a read-only collection.
     */
    public synchronized Iterable<Entry<String, Object>> getProperties(final IdentityPropertyScope propertyScope) {
        /*
         * Java lacks a writable composite collection class that would let us
         * return a writable object, so we return read-only sets/lists.
         */

        if (propertyScope == IdentityPropertyScope.LOCAL) {
            return Collections.unmodifiableSet(localProperties.entrySet());
        } else if (propertyScope == IdentityPropertyScope.GLOBAL) {
            return Collections.unmodifiableSet(properties.entrySet());
        } else {
            final List<Entry<String, Object>> copy = new ArrayList<Entry<String, Object>>();
            copy.addAll(localProperties.entrySet());
            for (final Entry<String, Object> e : properties.entrySet()) {
                if (!localProperties.containsKey(e.getKey())) {
                    copy.add(e);
                }
            }
            return Collections.unmodifiableList(copy);
        }
    }

    /**
     * Sets a property, will overwrite if already set.
     *
     * @param name
     *        the name of the property
     * @param value
     *        the value of the property
     */
    public synchronized void setProperty(final String name, final Object value) {
        setProperty(IdentityPropertyScope.GLOBAL, name, value);
    }

    /**
     * Sets a property, will overwrite if already set.
     *
     * @param name
     *        the name of the property
     * @param value
     *        the value of the property
     * @param propertyScope
     *        indiciates if local or global property is set
     */
    public synchronized void setProperty(
        final IdentityPropertyScope propertyScope,
        final String name,
        final Object value) {
        PropertyValidation.validatePropertyName(name);
        PropertyValidation.validatePropertyValue(name, value);

        if (IdentityAttributeTags.READ_ONLY_PROPERTIES.contains(name)) {
            throw new NotSupportedException(
                MessageFormat.format(
                    Messages.getString("TeamFoundationIdentity.IdentityPropertyReadOnlyFormat"), //$NON-NLS-1$
                    name));
        }

        if (propertyScope == IdentityPropertyScope.LOCAL) {
            setLocalProperty(name, value);
        } else if (propertyScope == IdentityPropertyScope.GLOBAL) {
            setGlobalProperty(name, value);
            removeProperty(IdentityPropertyScope.LOCAL, name);
        } else {
            throw new IllegalArgumentException(Messages.getString("TeamFoundationIdentity.InvalidPropertyScope")); //$NON-NLS-1$
        }
    }

    /**
     * Sets a property, will overwrite if already set.
     *
     * @param name
     *        the name of the property
     * @param value
     *        the value of the property
     */
    private synchronized void setGlobalProperty(final String name, final Object value) {
        properties.put(name, value);

        // Mark this property as modified, so that it will be processed
        // on Update
        modifiedPropertiesLog.add(name);
    }

    private synchronized void setLocalProperty(final String name, final Object value) {
        localProperties.put(name, value);

        // Mark this property as modified, so that it will be processed
        // on Update
        modifiedLocalPropertiesLog.add(name);
    }

    /**
     * Remove property, if it exists.
     *
     * @param propertyName
     *        the name of the property
     */
    public synchronized void removeProperty(final String name) {
        setProperty(IdentityPropertyScope.GLOBAL, name, null);
    }

    /**
     * Remove property, if it exists.
     *
     * @param propertyName
     *        the name of the property
     */
    public synchronized void removeProperty(final IdentityPropertyScope propertyScope, final String name) {
        setProperty(propertyScope, name, null);
    }

    /**
     * @deprecated use {@link #getProperty(String)}
     */
    @Deprecated
    public synchronized String getAttribute(final String name, final String defaultValue) {
        if (properties.containsKey(name)) {
            return properties.get(name).toString();
        } else {
            return defaultValue;
        }
    }

    /**
     * @deprecated use {@link #setProperty(String, Object)}
     */
    @Deprecated
    public synchronized void setAttribute(final String name, final String value) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        properties.put(name, value);

        // reset the unique name since it might depend on this attribute.
        clientSideUniqueName = null;
    }

    public synchronized boolean isContainer() {
        // TODO This implementation is what VS does. Should the wrapper object's
        // "isContainer" always be ignored?

        boolean result = false;

        if (properties.containsKey(IdentityAttributeTags.SCHEMA_CLASS_NAME)) {
            final Object value = properties.get(IdentityAttributeTags.SCHEMA_CLASS_NAME);
            if (value != null && value.toString().equalsIgnoreCase(IdentityConstants.SCHEMA_CLASS_GROUP)) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Modified properties, to be processed on update.
     */
    public synchronized Set<String> getModifiedPropertiesLog(final IdentityPropertyScope propertyScope) {
        if (propertyScope == IdentityPropertyScope.LOCAL) {
            return modifiedLocalPropertiesLog;
        } else if (propertyScope == IdentityPropertyScope.GLOBAL) {
            return modifiedPropertiesLog;
        } else {
            throw new IllegalArgumentException(Messages.getString("TeamFoundationIdentity.InvalidPropertyScope")); //$NON-NLS-1$
        }
    }

    public synchronized void resetModifiedProperties() {
        modifiedPropertiesLog.clear();
        modifiedLocalPropertiesLog.clear();
    }

    public synchronized boolean isActive() {
        return getWebServiceObject().isIsActive();
    }

    public synchronized GUID getTeamFoundationID() {
        return new GUID(getWebServiceObject().getTeamFoundationId());
    }

    public synchronized String getDisplayName() {
        return getWebServiceObject().getDisplayName();
    }

    public synchronized void setDisplayName(final String displayName) {
        getWebServiceObject().setDisplayName(displayName);
    }

    /**
     * A human-readable string that can be used to reference the identity.
     * Intended to be easy to read and type. Can be used in conjunction with the
     * general search factor in leiu of a display name.
     * <ul>
     * <li>Do not take dependencies on the format of this string.</li>
     * <li>Do not parse this string.</li>
     * <li>The UniqueName is not guaranteed to include a domain name.</li>
     * <li>The format and information in this string is subject to change.</li>
     * </ul>
     * <p>
     * Examples are <code>CONTOSO\jsmith</code> and
     * <code>jsmith@contoso.com</code>.
     */
    public synchronized String getUniqueName() {
        // If the Dev11+ server returned the unique name, use that.
        final String uniqueName = getWebServiceObject().getUniqueName();
        if (uniqueName != null && uniqueName.length() > 0) {
            return uniqueName;
        }

        // approximate the Dev11+ server behavior.
        if (clientSideUniqueName == null) {
            // approximate the Dev11+ server behavior
            final int uniqueId = getUniqueUserID();
            final String domain = getAttribute(IdentityAttributeTags.DOMAIN, ""); //$NON-NLS-1$
            final String account = getAttribute(IdentityAttributeTags.ACCOUNT_NAME, ""); //$NON-NLS-1$

            // domain can be empty for bind-pending identities for now.
            Check.notNull(account, "account"); //$NON-NLS-1$

            if (uniqueId == IdentityConstants.ACTIVE_UNIQUE_ID) {
                if (domain == null || domain.length() == 0) {
                    clientSideUniqueName = account;
                } else {
                    clientSideUniqueName = MessageFormat.format("{0}\\{1}", domain, account); //$NON-NLS-1$
                }
            } else {
                if (domain == null || domain.length() == 0) {
                    clientSideUniqueName = MessageFormat.format("{0}:{1}", account, Integer.toString(uniqueId)); //$NON-NLS-1$
                } else {
                    clientSideUniqueName =
                        MessageFormat.format("{0}\\{1}:{2}", domain, account, Integer.toString(uniqueId)); //$NON-NLS-1$
                }
            }
        }

        return clientSideUniqueName;
    }

    public synchronized int getUniqueUserID() {
        return getWebServiceObject().getUniqueUserId();
    }

    public synchronized IdentityDescriptor getDescriptor() {
        return new IdentityDescriptor(getWebServiceObject().getDescriptor());
    }

    public synchronized IdentityDescriptor[] getMembers() {
        return (IdentityDescriptor[]) WrapperUtils.wrap(IdentityDescriptor.class, getWebServiceObject().getMembers());
    }

    public IdentityDescriptor[] getMemberOf() {
        return (IdentityDescriptor[]) WrapperUtils.wrap(IdentityDescriptor.class, getWebServiceObject().getMemberOf());
    }
}

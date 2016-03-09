// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.core.checkinpolicies.PolicyInstance;
import com.microsoft.tfs.core.checkinpolicies.PolicyLoader;
import com.microsoft.tfs.core.checkinpolicies.PolicyLoaderException;
import com.microsoft.tfs.util.Check;

/**
 * Loads check-in policy implementations in an Eclipse environment by searching
 * for Eclipse plug-ins that implement the check-in policy extension point
 * {@link ExtensionPointPolicyLoader#CHECKIN_POLICY_EXTENSION_POINT_NAME}.
 *
 * @threadsafety thread-safe
 */
public class ExtensionPointPolicyLoader implements PolicyLoader {
    private static final Log log = LogFactory.getLog(ExtensionPointPolicyLoader.class);

    /**
     * The full name of the extension point plug-ins must extend to provide a
     * check-in policy loadable to Explorer and Plug-in for Eclipse.
     */
    public static final String CHECKIN_POLICY_EXTENSION_POINT_NAME = "com.microsoft.tfs.checkinpolicies.checkinPolicy"; //$NON-NLS-1$

    private static final String POLICY_ELEMENT_NAME = "policy"; //$NON-NLS-1$
    private static final String ID_ATTRIBUTE_NAME = "typeID"; //$NON-NLS-1$
    private static final String CLASS_ATTRIBUTE_NAME = "class"; //$NON-NLS-1$

    /**
     * Creates an {@link ExtensionPointPolicyLoader}.
     */
    public ExtensionPointPolicyLoader() {
    }

    /**
     * Gets the configuration elements for the plugins that extend our checkin
     * policy extension point.
     *
     * @return an array of configuration elements from plugins that support our
     *         extension point.
     */
    private IConfigurationElement[] getConfigurationElementsForCheckinPolicy() {
        final IExtensionRegistry registry = Platform.getExtensionRegistry();

        final IExtensionPoint extensionPoint = registry.getExtensionPoint(CHECKIN_POLICY_EXTENSION_POINT_NAME);

        /*
         * These extension points should always be available even if there are
         * no contributions available (policy implementations), but it's good to
         * check anyway.
         */
        if (extensionPoint == null) {
            final String messageFormat = "Couldn't load extension point {0}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, CHECKIN_POLICY_EXTENSION_POINT_NAME);
            log.error(message);
            throw new PolicyLoaderException(message, null);
        }

        return extensionPoint.getConfigurationElements();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getAvailablePolicyTypeIDs() throws PolicyLoaderException {
        final IConfigurationElement[] elements = getConfigurationElementsForCheckinPolicy();

        log.debug("Searching for available checkin policy implementations"); //$NON-NLS-1$

        final List foundIDs = new ArrayList();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].getName().equals(POLICY_ELEMENT_NAME)) {
                final String thisPolicyTypeID = elements[i].getAttribute(ID_ATTRIBUTE_NAME);
                final String thisClass = elements[i].getAttribute(CLASS_ATTRIBUTE_NAME);

                // Only include elements with both attributes defined.
                if (thisPolicyTypeID != null
                    && thisPolicyTypeID.length() > 0
                    && thisClass != null
                    && thisClass.length() > 0) {
                    final String messageFormat = "Policy type ID {0} implemented by {1}"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, thisPolicyTypeID, thisClass);
                    log.debug(message);
                    foundIDs.add(thisPolicyTypeID);
                }
            }
        }

        final String messageFormat = "Total {0} extensions that advertise checkin policy implementations"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, foundIDs.size());
        log.debug(message);

        return (String[]) foundIDs.toArray(new String[foundIDs.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicyInstance load(final String policyTypeID) throws PolicyLoaderException {
        Check.notNullOrEmpty(policyTypeID, "policyTypeID"); //$NON-NLS-1$

        final IConfigurationElement[] elements = getConfigurationElementsForCheckinPolicy();

        String messageFormat = "Trying to load an extension for policy type ID {0}"; //$NON-NLS-1$
        String message = MessageFormat.format(messageFormat, policyTypeID);
        log.debug(message);

        /*
         * Return the first instance we can create from an extension point that
         * declares the correct policy type ID and a class.
         */
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].getName().equals(POLICY_ELEMENT_NAME)) {
                final String thisPolicyTypeID = elements[i].getAttribute(ID_ATTRIBUTE_NAME);
                final String thisClass = elements[i].getAttribute(CLASS_ATTRIBUTE_NAME);

                // Only try to load if the type IDs are equal.
                if (thisPolicyTypeID != null && thisClass != null && thisPolicyTypeID.equals(policyTypeID)) {
                    try {
                        final PolicyInstance instance =
                            (PolicyInstance) elements[i].createExecutableExtension(CLASS_ATTRIBUTE_NAME);

                        /*
                         * Check that the class we loaded really is the correct
                         * type, and its plugin.xml isn't lying.
                         */
                        if (thisPolicyTypeID.equals(instance.getPolicyType().getID())) {
                            return instance;
                        }

                        messageFormat =
                            "Policy implementation class {0} does not implement the policy type ID {1} that its extension claims it does, trying to load another extension"; //$NON-NLS-1$
                        message = MessageFormat.format(messageFormat, thisClass, thisPolicyTypeID);

                        TFSCheckinPoliciesPlugin.getDefault().getLog().log(
                            new Status(Status.ERROR, TFSCheckinPoliciesPlugin.PLUGIN_ID, 0, message, null));

                        log.error(message);
                    } catch (final CoreException e) {
                        messageFormat =
                            "Error creating executable extension for policy type ID {0} from Java class {1}, trying to load another extension"; //$NON-NLS-1$
                        message = MessageFormat.format(messageFormat, thisPolicyTypeID, thisClass);

                        /*
                         * Log the error, but keep on loading other plugins.
                         */
                        TFSCheckinPoliciesPlugin.getDefault().getLog().log(
                            new Status(Status.ERROR, TFSCheckinPoliciesPlugin.PLUGIN_ID, 0, message, e));

                        log.error(message, e);
                    }
                }
            }
        }

        messageFormat = "Could not find an extension for policy ID {0}"; //$NON-NLS-1$
        message = MessageFormat.format(messageFormat, policyTypeID);
        log.debug(message);

        // Nothing found.
        return null;
    }
}

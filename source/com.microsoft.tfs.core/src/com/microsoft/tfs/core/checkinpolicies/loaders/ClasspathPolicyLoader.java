// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies.loaders;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.checkinpolicies.PolicyInstance;
import com.microsoft.tfs.core.checkinpolicies.PolicyLoader;
import com.microsoft.tfs.core.checkinpolicies.PolicyLoaderException;
import com.microsoft.tfs.core.checkinpolicies.loaders.internal.PolicyInstanceProperties;

/**
 * <p>
 * Loads check-in policies from the classpath of the {@link ClassLoader} that
 * loaded this class. See {@link #POLICY_INSTANCE_PROPERTIES_FILENAME} for the
 * name of the classpath resource that declares available policy
 * implementations.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class ClasspathPolicyLoader implements PolicyLoader {
    /**
     * {@link ClasspathPolicyLoader} searches the {@link ClassLoader} that
     * loaded this class for all resources with this name. These resources are
     * properties files which are parsed in order to determine the name of a
     * {@link PolicyInstance} class to load and the policy ID the class
     * implements. Other properties may also be read from this resource.
     */
    public static final String POLICY_INSTANCE_PROPERTIES_FILENAME = "checkin-policy-instance.properties"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(ClasspathPolicyLoader.class);

    /**
     * Creates a {@link ClasspathPolicyLoader}, which loads implementations by
     * searching the classpath for implementation properties files.
     */
    public ClasspathPolicyLoader() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicyInstance load(final String policyTypeID) throws PolicyLoaderException {
        log.debug(MessageFormat.format("Trying to load policy instance for policy type ID '{0}'", policyTypeID)); //$NON-NLS-1$

        // Resolve the ID to a class name.
        final PolicyInstanceProperties[] propertiesFiles = getAllPolicyInstanceProperties();

        for (int i = 0; i < propertiesFiles.length; i++) {
            if (propertiesFiles[i].getID().equals(policyTypeID)) {
                PolicyInstance ret = null;

                try {
                    ret = loadPolicyInstanceClass(propertiesFiles[i].getClassName());
                } catch (final PolicyLoaderException e) {
                    // Log only.
                    log.error(
                        MessageFormat.format(
                            "Could not instantiate class {0} for policy type ID '{1}'.  Other classes that support this ID may still be loaded.", //$NON-NLS-1$
                            propertiesFiles[i].getClassName(),
                            propertiesFiles[i].getID()),
                        e);
                }

                /*
                 * Make sure the instance we created returns its type as the one
                 * the user searched for. We don't want to trust a misleading
                 * properties file.
                 */
                if (ret != null && ret.getPolicyType().getID().equals(policyTypeID) == false) {
                    final String messageFormat =
                        "Class {0} was named in a properties file, but this class supports a different type ID '{1}' than requested ID '{2}'"; //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        propertiesFiles[i].getClassName(),
                        ret.getPolicyType().getID(),
                        policyTypeID);

                    log.error(message);
                    throw new PolicyLoaderException(message, null);
                }

                if (ret != null) {
                    return ret;
                }
            }
        }

        log.warn(MessageFormat.format("Could not load any policies that support policy type ID '{0}'", policyTypeID)); //$NON-NLS-1$

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getAvailablePolicyTypeIDs() throws PolicyLoaderException {
        log.debug("Searching for available policy type IDs"); //$NON-NLS-1$

        /*
         * Find all properties files and get the class names from them.
         */
        final PolicyInstanceProperties[] propertiesFiles = getAllPolicyInstanceProperties();
        final String[] ids = new String[propertiesFiles.length];

        for (int i = 0; i < propertiesFiles.length; i++) {
            ids[i] = propertiesFiles[i].getID();
        }

        log.debug(MessageFormat.format("Found {0} IDs", Integer.toString(ids.length))); //$NON-NLS-1$

        return ids;
    }

    /**
     * Loads a single class by name.
     */
    private PolicyInstance loadPolicyInstanceClass(final String className) throws PolicyLoaderException {
        try {
            final Class instanceClass = ClasspathPolicyLoader.class.getClassLoader().loadClass(className);
            final Object newInstance = instanceClass.newInstance();

            if (newInstance instanceof PolicyInstance == false) {
                final String messageFormat =
                    "Class name {0} loaded successfully, but it does not implement the {1} interface so it cannot be used"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, className, PolicyInstance.class.getName());

                log.error(message);
                throw new PolicyLoaderException(message, null);
            }

            return (PolicyInstance) newInstance;
        } catch (final ClassNotFoundException e) {
            final String message = Messages.getString("ClasspathPolicyLoader.ErrorLoadingPolicyInstanceClass"); //$NON-NLS-1$
            log.warn(message, e);
            throw new PolicyLoaderException(message, e, null);
        } catch (final InstantiationException e) {
            final String message = Messages.getString("ClasspathPolicyLoader.ErrorInstantiatingPolicyInstanceClass"); //$NON-NLS-1$
            log.error(message, e);
            throw new PolicyLoaderException(message, e, null);
        } catch (final IllegalAccessException e) {
            final String message =
                Messages.getString("ClasspathPolicyLoader.AccessErrorInstantiatingPolicyInstanceClass"); //$NON-NLS-1$
            log.error(message, e);
            throw new PolicyLoaderException(message, e, null);
        }
    }

    /**
     * Gets all properties information available. Instance properties files
     * define the policy type ID and instance class name.
     *
     * @return all properties information that could be loaded.
     * @throws PolicyLoaderException
     *         if an error occurred reading properties files.
     */
    private PolicyInstanceProperties[] getAllPolicyInstanceProperties() throws PolicyLoaderException {
        final List propertiesList = new ArrayList();

        /*
         * Find all policy instance properties files in the new classloader.
         */
        Enumeration propertiesFileResources;
        try {
            propertiesFileResources =
                ClasspathPolicyLoader.class.getClassLoader().getResources(POLICY_INSTANCE_PROPERTIES_FILENAME);
        } catch (final IOException e) {
            final String messageFormat = "Error getting properties resources ('{0}') from classloader"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, POLICY_INSTANCE_PROPERTIES_FILENAME);

            log.warn(message, e);
            throw new PolicyLoaderException(e, null);
        }

        while (propertiesFileResources.hasMoreElements()) {
            final URL propertiesResourceURL = (URL) propertiesFileResources.nextElement();

            /*
             * Open and read the properties file.
             */
            InputStream propertiesResourceInputStream = null;
            try {
                propertiesResourceInputStream = propertiesResourceURL.openStream();

                final PolicyInstanceProperties props = PolicyInstanceProperties.load(propertiesResourceInputStream);

                final String messageFormat =
                    "Check-in policy properties file {0} declares policy type ID '{1}' is implemented by class {2}"; //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    propertiesResourceURL.toString(),
                    props.getID(),
                    props.getClassName());

                log.info(message);

                propertiesList.add(props);
            } catch (final IOException e) {
                // Log it only, so we can continue reading properties.
                log.error("Error reading properties input stream, continuing", e); //$NON-NLS-1$
            } finally {
                try {
                    if (propertiesResourceInputStream != null) {
                        propertiesResourceInputStream.close();
                    }
                } catch (final IOException e) {
                    // Log it only, so we can continue reading properties.
                    log.error("Error closing properties input stream, continuing", e); //$NON-NLS-1$
                }
            }
        }

        return (PolicyInstanceProperties[]) propertiesList.toArray(new PolicyInstanceProperties[propertiesList.size()]);
    }
}

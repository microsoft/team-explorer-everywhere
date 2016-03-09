/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/params/DefaultHttpParams.java,v 1.9
 * 2004/12/21 23:15:21 olegk Exp $ $Revision: 510589 $ $Date: 2007-02-22
 * 18:04:52 +0100 (Thu, 22 Feb 2007) $
 *
 * ====================================================================
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */

package com.microsoft.tfs.core.httpclient.params;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class represents a collection of HTTP protocol parameters. Protocol
 * parameters may be linked together to form a hierarchy. If a particular
 * parameter value has not been explicitly defined in the collection itself, its
 * value will be drawn from the parent collection of parameters.
 *
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *
 * @version $Revision: 510589 $
 *
 * @since 3.0
 */
public class DefaultHttpParams implements HttpParams, Serializable, Cloneable {

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(DefaultHttpParams.class);

    /** HttpParams class factory. */
    private static HttpParamsFactory httpParamsFactory = new DefaultHttpParamsFactory();

    /**
     * Gets the default HttpParams to be used.
     *
     * @return the value returned from
     *         <code>HttpParamsFactory#getDefaultParams()</code>
     *
     * @see HttpParamsFactory#getDefaultParams()
     */
    public static HttpParams getDefaultParams() {
        return httpParamsFactory.getDefaultParams();
    }

    /**
     * Sets the factory that will provide the default HttpParams.
     *
     * @param httpParamsFactory
     *        an instance of HttpParamsFactory
     *
     * @see #getDefaultParams()
     */
    public static void setHttpParamsFactory(final HttpParamsFactory httpParamsFactory) {
        if (httpParamsFactory == null) {
            throw new IllegalArgumentException("httpParamsFactory may not be null");
        }
        DefaultHttpParams.httpParamsFactory = httpParamsFactory;
    }

    /** The set of default values to defer to */
    private HttpParams defaults = null;

    /** Hash map of HTTP parameters that this collection contains */
    private HashMap parameters = null;

    /**
     * Creates a new collection of parameters with the given parent. The
     * collection will defer to its parent for a default value if a particular
     * parameter is not explicitly set in the collection itself.
     *
     * @param defaults
     *        the parent collection to defer to, if a parameter is not explictly
     *        set in the collection itself.
     */
    public DefaultHttpParams(final HttpParams defaults) {
        super();
        this.defaults = defaults;
    }

    /**
     * Creates a new collection of parameters with the collection returned by
     * {@link #getDefaultParams()} as a parent. The collection will defer to its
     * parent for a default value if a particular parameter is not explicitly
     * set in the collection itself.
     *
     * @see #getDefaultParams()
     */
    public DefaultHttpParams() {
        this(getDefaultParams());
    }

    @Override
    public synchronized HttpParams getDefaults() {
        return defaults;
    }

    @Override
    public synchronized void setDefaults(final HttpParams params) {
        defaults = params;
    }

    @Override
    public synchronized Object getParameter(final String name) {
        // See if the parameter has been explicitly defined
        Object param = null;
        if (parameters != null) {
            param = parameters.get(name);
        }
        if (param != null) {
            // If so, return
            return param;
        } else {
            // If not, see if defaults are available
            if (defaults != null) {
                // Return default parameter value
                return defaults.getParameter(name);
            } else {
                // Otherwise, return null
                return null;
            }
        }
    }

    @Override
    public synchronized void setParameter(final String name, final Object value) {
        if (parameters == null) {
            parameters = new HashMap();
        }
        parameters.put(name, value);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Set parameter " + name + " = " + value);
        }
    }

    /**
     * Assigns the value to all the parameter with the given names
     *
     * @param names
     *        array of parameter name
     * @param value
     *        parameter value
     */
    public synchronized void setParameters(final String[] names, final Object value) {
        for (int i = 0; i < names.length; i++) {
            setParameter(names[i], value);
        }
    }

    @Override
    public long getLongParameter(final String name, final long defaultValue) {
        final Object param = getParameter(name);
        if (param == null) {
            return defaultValue;
        }
        return ((Long) param).longValue();
    }

    @Override
    public void setLongParameter(final String name, final long value) {
        setParameter(name, new Long(value));
    }

    @Override
    public int getIntParameter(final String name, final int defaultValue) {
        final Object param = getParameter(name);
        if (param == null) {
            return defaultValue;
        }
        return ((Integer) param).intValue();
    }

    @Override
    public void setIntParameter(final String name, final int value) {
        setParameter(name, new Integer(value));
    }

    @Override
    public double getDoubleParameter(final String name, final double defaultValue) {
        final Object param = getParameter(name);
        if (param == null) {
            return defaultValue;
        }
        return ((Double) param).doubleValue();
    }

    @Override
    public void setDoubleParameter(final String name, final double value) {
        setParameter(name, new Double(value));
    }

    @Override
    public boolean getBooleanParameter(final String name, final boolean defaultValue) {
        final Object param = getParameter(name);
        if (param == null) {
            return defaultValue;
        }
        return ((Boolean) param).booleanValue();
    }

    @Override
    public void setBooleanParameter(final String name, final boolean value) {
        setParameter(name, value ? Boolean.TRUE : Boolean.FALSE);// Boolean.valueOf()
                                                                 // = Java 1.4+
    }

    @Override
    public boolean isParameterSet(final String name) {
        return getParameter(name) != null;
    }

    @Override
    public boolean isParameterSetLocally(final String name) {
        return parameters != null && parameters.get(name) != null;
    }

    @Override
    public boolean isParameterTrue(final String name) {
        return getBooleanParameter(name, false);
    }

    @Override
    public boolean isParameterFalse(final String name) {
        return !getBooleanParameter(name, false);
    }

    /**
     * Removes all parameters from this collection.
     */
    public void clear() {
        parameters = null;
    }

    /**
     * Clones this collection of parameters. Please note that paramter values
     * themselves are not cloned.
     *
     * @see java.io.Serializable
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        final DefaultHttpParams clone = (DefaultHttpParams) super.clone();
        if (parameters != null) {
            clone.parameters = (HashMap) parameters.clone();
        }
        clone.setDefaults(defaults);
        return clone;
    }
}

/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/methods/PostMethod.java,v
 * 1.58 2004/08/08 12:50:09 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
 * 06:56:49 +0100 (Wed, 29 Nov 2006) $
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

package com.microsoft.tfs.core.httpclient.methods;

import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.NameValuePair;
import com.microsoft.tfs.core.httpclient.util.EncodingUtil;

/**
 * Implements the HTTP POST method.
 * <p>
 * The HTTP POST method is defined in section 9.5 of
 * <a href="http://www.ietf.org/rfc/rfc2616.txt">RFC2616</a>: <blockquote> The
 * POST method is used to request that the origin server accept the entity
 * enclosed in the request as a new subordinate of the resource identified by
 * the Request-URI in the Request-Line. POST is designed to allow a uniform
 * method to cover the following functions:
 * <ul>
 * <li>Annotation of existing resources</li>
 * <li>Posting a message to a bulletin board, newsgroup, mailing list, or
 * similar group of articles</li>
 * <li>Providing a block of data, such as the result of submitting a form, to a
 * data-handling process</li>
 * <li>Extending a database through an append operation</li>
 * </ul>
 * </blockquote>
 * </p>
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author <a href="mailto:dsale@us.britannica.com">Doug Sale</a>
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever</a>
 * @author Ortwin Gl???ck
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *
 * @version $Revision: 480424 $
 * @since 1.0
 */
public class PostMethod extends EntityEnclosingMethod {
    // -------------------------------------------------------------- Constants

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(PostMethod.class);

    /** The Content-Type for www-form-urlencoded. */
    public static final String FORM_URL_ENCODED_CONTENT_TYPE = "application/x-www-form-urlencoded";

    /**
     * The buffered request body consisting of <code>NameValuePair</code>s.
     */
    private final Vector params = new Vector();

    // ----------------------------------------------------------- Constructors

    /**
     * No-arg constructor.
     *
     * @since 1.0
     */
    public PostMethod() {
        super();
    }

    /**
     * Constructor specifying a URI.
     *
     * @param uri
     *        either an absolute or relative URI
     *
     * @since 1.0
     */
    public PostMethod(final String uri) {
        super(uri);
    }

    // ----------------------------------------------------- Instance Methods

    /**
     * Returns <tt>"POST"</tt>.
     *
     * @return <tt>"POST"</tt>
     *
     * @since 2.0
     */
    @Override
    public String getName() {
        return "POST";
    }

    /**
     * Returns <tt>true</tt> if there is a request body to be sent.
     *
     * <P>
     * This method must be overwritten by sub-classes that implement alternative
     * request content input methods
     * </p>
     *
     * @return boolean
     *
     * @since 2.0beta1
     */
    @Override
    protected boolean hasRequestContent() {
        LOG.trace("enter PostMethod.hasRequestContent()");
        if (!params.isEmpty()) {
            return true;
        } else {
            return super.hasRequestContent();
        }
    }

    /**
     * Clears request body.
     *
     * <p>
     * This method must be overwritten by sub-classes that implement alternative
     * request content input methods
     * </p>
     *
     * @since 2.0beta1
     */
    @Override
    protected void clearRequestBody() {
        LOG.trace("enter PostMethod.clearRequestBody()");
        params.clear();
        super.clearRequestBody();
    }

    /**
     * Generates a request entity from the post parameters, if present. Calls
     * {@link EntityEnclosingMethod#generateRequestBody()} if parameters have
     * not been set.
     *
     * @since 3.0
     */
    @Override
    protected RequestEntity generateRequestEntity() {
        if (!params.isEmpty()) {
            // Use a ByteArrayRequestEntity instead of a StringRequestEntity.
            // This is to avoid potential encoding issues. Form url encoded
            // strings
            // are ASCII by definition but the content type may not be. Treating
            // the content
            // as bytes allows us to keep the current charset without worrying
            // about how
            // this charset will effect the encoding of the form url encoded
            // string.
            final String content = EncodingUtil.formUrlEncode(getParameters(), getRequestCharSet());
            final ByteArrayRequestEntity entity =
                new ByteArrayRequestEntity(EncodingUtil.getAsciiBytes(content), FORM_URL_ENCODED_CONTENT_TYPE);
            return entity;
        } else {
            return super.generateRequestEntity();
        }
    }

    /**
     * Sets the value of parameter with parameterName to parameterValue. This
     * method does not preserve the initial insertion order.
     *
     * @param parameterName
     *        name of the parameter
     * @param parameterValue
     *        value of the parameter
     *
     * @since 2.0
     */
    public void setParameter(final String parameterName, final String parameterValue) {
        LOG.trace("enter PostMethod.setParameter(String, String)");

        removeParameter(parameterName);
        addParameter(parameterName, parameterValue);
    }

    /**
     * Gets the parameter of the specified name. If there exists more than one
     * parameter with the name paramName, then only the first one is returned.
     *
     * @param paramName
     *        name of the parameter
     *
     * @return If a parameter exists with the name argument, the coresponding
     *         NameValuePair is returned. Otherwise null.
     *
     * @since 2.0
     *
     */
    public NameValuePair getParameter(final String paramName) {
        LOG.trace("enter PostMethod.getParameter(String)");

        if (paramName == null) {
            return null;
        }

        final Iterator iter = params.iterator();

        while (iter.hasNext()) {
            final NameValuePair parameter = (NameValuePair) iter.next();

            if (paramName.equals(parameter.getName())) {
                return parameter;
            }
        }
        return null;
    }

    /**
     * Gets the parameters currently added to the PostMethod. If there are no
     * parameters, a valid array is returned with zero elements. The returned
     * array object contains an array of pointers to the internal data members.
     *
     * @return An array of the current parameters
     *
     * @since 2.0
     *
     */
    public NameValuePair[] getParameters() {
        LOG.trace("enter PostMethod.getParameters()");

        final int numPairs = params.size();
        final Object[] objectArr = params.toArray();
        final NameValuePair[] nvPairArr = new NameValuePair[numPairs];

        for (int i = 0; i < numPairs; i++) {
            nvPairArr[i] = (NameValuePair) objectArr[i];
        }

        return nvPairArr;
    }

    /**
     * Adds a new parameter to be used in the POST request body.
     *
     * @param paramName
     *        The parameter name to add.
     * @param paramValue
     *        The parameter value to add.
     *
     * @throws IllegalArgumentException
     *         if either argument is null
     *
     * @since 1.0
     */
    public void addParameter(final String paramName, final String paramValue) throws IllegalArgumentException {
        LOG.trace("enter PostMethod.addParameter(String, String)");

        if ((paramName == null) || (paramValue == null)) {
            throw new IllegalArgumentException("Arguments to addParameter(String, String) cannot be null");
        }
        super.clearRequestBody();
        params.add(new NameValuePair(paramName, paramValue));
    }

    /**
     * Adds a new parameter to be used in the POST request body.
     *
     * @param param
     *        The parameter to add.
     *
     * @throws IllegalArgumentException
     *         if the argument is null or contains null values
     *
     * @since 2.0
     */
    public void addParameter(final NameValuePair param) throws IllegalArgumentException {
        LOG.trace("enter PostMethod.addParameter(NameValuePair)");

        if (param == null) {
            throw new IllegalArgumentException("NameValuePair may not be null");
        }
        addParameter(param.getName(), param.getValue());
    }

    /**
     * Adds an array of parameters to be used in the POST request body. Logs a
     * warning if the parameters argument is null.
     *
     * @param parameters
     *        The array of parameters to add.
     *
     * @since 2.0
     */
    public void addParameters(final NameValuePair[] parameters) {
        LOG.trace("enter PostMethod.addParameters(NameValuePair[])");

        if (parameters == null) {
            LOG.warn("Attempt to addParameters(null) ignored");
        } else {
            super.clearRequestBody();
            for (int i = 0; i < parameters.length; i++) {
                params.add(parameters[i]);
            }
        }
    }

    /**
     * Removes all parameters with the given paramName. If there is more than
     * one parameter with the given paramName, all of them are removed. If there
     * is just one, it is removed. If there are none, then the request is
     * ignored.
     *
     * @param paramName
     *        The parameter name to remove.
     *
     * @return true if at least one parameter was removed
     *
     * @throws IllegalArgumentException
     *         When the parameter name passed is null
     *
     * @since 2.0
     */
    public boolean removeParameter(final String paramName) throws IllegalArgumentException {
        LOG.trace("enter PostMethod.removeParameter(String)");

        if (paramName == null) {
            throw new IllegalArgumentException("Argument passed to removeParameter(String) cannot be null");
        }
        boolean removed = false;
        final Iterator iter = params.iterator();

        while (iter.hasNext()) {
            final NameValuePair pair = (NameValuePair) iter.next();

            if (paramName.equals(pair.getName())) {
                iter.remove();
                removed = true;
            }
        }
        return removed;
    }

    /**
     * Removes all parameter with the given paramName and paramValue. If there
     * is more than one parameter with the given paramName, only one is removed.
     * If there are none, then the request is ignored.
     *
     * @param paramName
     *        The parameter name to remove.
     * @param paramValue
     *        The parameter value to remove.
     *
     * @return true if a parameter was removed.
     *
     * @throws IllegalArgumentException
     *         when param name or value are null
     *
     * @since 2.0
     */
    public boolean removeParameter(final String paramName, final String paramValue) throws IllegalArgumentException {
        LOG.trace("enter PostMethod.removeParameter(String, String)");

        if (paramName == null) {
            throw new IllegalArgumentException("Parameter name may not be null");
        }
        if (paramValue == null) {
            throw new IllegalArgumentException("Parameter value may not be null");
        }

        final Iterator iter = params.iterator();

        while (iter.hasNext()) {
            final NameValuePair pair = (NameValuePair) iter.next();

            if (paramName.equals(pair.getName()) && paramValue.equals(pair.getValue())) {
                iter.remove();
                return true;
            }
        }

        return false;
    }

    /**
     * Sets an array of parameters to be used in the POST request body
     *
     * @param parametersBody
     *        The array of parameters to add.
     *
     * @throws IllegalArgumentException
     *         when param parameters are null
     *
     * @since 2.0beta1
     */
    public void setRequestBody(final NameValuePair[] parametersBody) throws IllegalArgumentException {
        LOG.trace("enter PostMethod.setRequestBody(NameValuePair[])");

        if (parametersBody == null) {
            throw new IllegalArgumentException("Array of parameters may not be null");
        }
        clearRequestBody();
        addParameters(parametersBody);
    }
}

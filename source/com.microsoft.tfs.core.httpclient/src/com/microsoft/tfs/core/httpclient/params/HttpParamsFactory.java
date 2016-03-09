/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/params/HttpParamsFactory.java,v 1.5
 * 2004/05/13 04:01:22 mbecke Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

package com.microsoft.tfs.core.httpclient.params;

/**
 * A factory for getting the default set of parameters to use when creating an
 * instance of <code>HttpParams</code>.
 *
 * @see com.microsoft.tfs.core.httpclient.params.DefaultHttpParams#setHttpParamsFactory(HttpParamsFactory)
 *
 * @since 3.0
 */
public interface HttpParamsFactory {

    /**
     * Gets the default parameters. This method may be called more than once and
     * is not required to always return the same value.
     *
     * @return an instance of HttpParams
     */
    HttpParams getDefaultParams();

}

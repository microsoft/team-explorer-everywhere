// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.vss.client.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omg.CORBA_2_3.portable.InputStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.httpclient.Header;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpException;
import com.microsoft.tfs.core.httpclient.HttpMethodBase;
import com.microsoft.tfs.core.httpclient.HttpStatus;
import com.microsoft.tfs.core.httpclient.methods.DeleteMethod;
import com.microsoft.tfs.core.httpclient.methods.EntityEnclosingMethod;
import com.microsoft.tfs.core.httpclient.methods.GetMethod;
import com.microsoft.tfs.core.httpclient.methods.InputStreamRequestEntity;
import com.microsoft.tfs.core.httpclient.methods.OptionsMethod;
import com.microsoft.tfs.core.httpclient.methods.PatchMethod;
import com.microsoft.tfs.core.httpclient.methods.PostMethod;
import com.microsoft.tfs.core.httpclient.methods.PutMethod;
import com.microsoft.tfs.core.httpclient.methods.RequestEntity;
import com.microsoft.tfs.core.httpclient.methods.StringRequestEntity;
import com.microsoft.tfs.core.product.CoreVersionInfo;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.vss.client.core.model.ApiResourceLocation;
import com.microsoft.vss.client.core.model.ApiResourceLocationCollection;
import com.microsoft.vss.client.core.model.ApiResourceVersion;
import com.microsoft.vss.client.core.model.NameValueCollection;
import com.microsoft.vss.client.core.model.ProxyAuthenticationRequiredException;
import com.microsoft.vss.client.core.model.VssException;
import com.microsoft.vss.client.core.model.VssResourceNotFoundException;
import com.microsoft.vss.client.core.model.VssServiceException;
import com.microsoft.vss.client.core.model.VssServiceResponseException;
import com.microsoft.vss.client.core.model.WrappedException;
import com.microsoft.vss.client.core.utils.JsonHelper;

public abstract class VssHttpClientBase {
    private static final Log log = LogFactory.getLog(VssHttpClientBase.class);

    protected final static String APPLICATION_JSON_TYPE = "application/json"; //$NON-NLS-1$
    protected final static String APPLICATION_OCTET_STREAM_TYPE = "application/octet-stream"; //$NON-NLS-1$
    protected final static String APPLICATION_ZIP_TYPE = "application/zip"; //$NON-NLS-1$
    protected final static String TEXT_PLAIN_TYPE = "text/plaint"; //$NON-NLS-1$
    protected final static String TEXT_HTML_TYPE = "text/html"; //$NON-NLS-1$
    protected final static String APPLICATION_JSON_PATCH_TYPE = "application/json-patch+json"; //$NON-NLS-1$
    protected final static String APPLICATION_GIT_MEDIA_TYPE = "application/vnd.git-media"; //$NON-NLS-1$
    protected final static String IMAGE_SVG_XML_MEDIA_TYPE = "image/svg+xml"; //$NON-NLS-1$
    protected final static String IMAGE_PNG_MEDIA_TYPE = "image/png"; //$NON-NLS-1$
    protected final static String APPLICATION_GZIP = "application/gzip"; //$NON-NLS-1$

    private final static String OPTIONS_RELATIVE_PATH = "_apis"; //$NON-NLS-1$
    private final static String CONNECTION_DATA_RELATIVE_PATH = "_apis/connectiondata"; //$NON-NLS-1$
    private final static String AREA_PARAMETER_NAME = "area"; //$NON-NLS-1$
    private final static String RESOURCE_PARAMETER_NAME = "resource"; //$NON-NLS-1$
    private final static String ROUTE_TEMPLATE_SEPARATOR = "/"; //$NON-NLS-1$
    private final static String MEDIA_TYPE_PARAMETERS_SEPARATOR = ";"; //$NON-NLS-1$

    private final static String CONTENT_TYPE_HEADER = "Content-Type"; //$NON-NLS-1$
    private final static String CONTENT_LENGTH_HEADER = "Content-Length"; //$NON-NLS-1$
    private final static ApiResourceVersion DEFAULT_API_VERSION = new ApiResourceVersion();

    private final static String API_VERSION_PARAMETER_NAME = "api-version"; //$NON-NLS-1$
    private final static String CHARSET_PARAMETER_NAME = "charset"; //$NON-NLS-1$
    private final static String VSS_HTTP_METHOD_OVERRIDE_PROPERTY = "VSS_HTTP_METHOD_OVERRIDE"; //$NON-NLS-1$

    private final TFSConnection connection;
    private final URI baseUrl;
    private final HttpClient httpClient;

    private final boolean overrideEnabled;

    private ApiResourceLocationCollection resourceLocations;
    private Exception lastException;

    protected VssHttpClientBase(final Object connection) {
        this.connection = (TFSConnection) connection;
        this.baseUrl = this.connection.getBaseURI();
        this.httpClient = this.connection.getHTTPClient();

        this.overrideEnabled = getOverrideSetting();
    }

    protected VssHttpClientBase(final Object rsClient, final URI baseUrl) {
        this.connection = null;
        this.baseUrl = null;
        this.httpClient = null;
        this.overrideEnabled = getOverrideSetting();
    }

    protected boolean getOverrideSetting() {
        final String overrideEnabledEnvVar = System.getProperty(VSS_HTTP_METHOD_OVERRIDE_PROPERTY);
        if (!StringUtil.isNullOrEmpty(overrideEnabledEnvVar)) {
            return Boolean.valueOf(overrideEnabledEnvVar);
        }

        return true;
    }

    protected boolean isOverrideEnabled() {
        return this.overrideEnabled;
    }

    public URI getBaseUrl() {
        return baseUrl;
    }

    public Exception getLastExecutionException() {
        return lastException;
    }

    protected Map<String, Class<? extends Exception>> getTranslatedExceptions() {
        return null;
    }

    private ApiResourceLocation getLocation(final UUID locationId) {
        if (resourceLocations == null) {
            resourceLocations = loadLocations();
        }

        if (resourceLocations != null) {
            return resourceLocations.getLocationById(locationId);
        } else {
            return null;
        }
    }

    public boolean checkConnection() {
        log.debug("Checking REST client connection"); //$NON-NLS-1$

        final URI connectionDataTarget = URIUtils.resolve(baseUrl, CONNECTION_DATA_RELATIVE_PATH);
        final HttpMethodBase request = createHttpMethod(HttpMethod.GET, connectionDataTarget);
        request.setFollowRedirects(true);

        try {
            request.setRequestHeader(VssHttpHeaders.ACCEPT, APPLICATION_JSON_TYPE);
            final int statusCode = httpClient.executeMethod(request);

            if (HttpStatus.isSuccessFamily(statusCode)) {
                final byte[] input = request.getResponseBody();

                if (input != null && input.length > 0) {
                    return true;
                }
            } else {
                throw new HttpException(HttpStatus.getStatusText(statusCode));
            }
        } catch (final Exception e) {
            log.error("Connection check failed. Probably the used PAT has expired or has been revoked.", e); //$NON-NLS-1$
            log.error(e.getMessage(), e);
            lastException = e;
        } finally {
            request.releaseConnection();
        }

        return false;
    }

    private ApiResourceLocationCollection loadLocations() {
        final URI optionsTarget = URIUtils.resolve(baseUrl, OPTIONS_RELATIVE_PATH);
        final HttpMethodBase request = createHttpMethod(HttpMethod.OPTIONS, optionsTarget);
        request.setFollowRedirects(true);

        try {
            request.setRequestHeader(VssHttpHeaders.ACCEPT, APPLICATION_JSON_TYPE);
            final int statusCode = httpClient.executeMethod(request);

            if (HttpStatus.isSuccessFamily(statusCode)) {
                return JsonHelper.deserializeResponce(request, ApiResourceLocationCollection.class);
            } else {
                throw new HttpException(HttpStatus.getStatusText(statusCode));
            }
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
            lastException = e;
            return null;
        } finally {
            request.releaseConnection();
        }
    }

    private URI createTarget(
        final UUID locationId,
        final Map<String, Object> routeValues,
        final Map<String, String> queryParameters) {

        final ApiResourceLocation location = getLocation(locationId);
        if (location == null) {
            throw new VssResourceNotFoundException(locationId, baseUrl, lastException);
        }

        final Map<String, String> dictionary =
            toRouteDictionary(routeValues, location.getArea(), location.getResourceName());

        final String routeTemplate = location.getRouteTemplate();
        final String actualTemplate = replaceRouteValues(routeTemplate, dictionary);
        final URI target = URIUtils.resolve(baseUrl, actualTemplate);

        return URIUtils.addQueryParameters(target, queryParameters);
    }

    private String replaceRouteValues(final String template, final Map<String, String> routeValues) {
        final String[] templateParameters = template.split(ROUTE_TEMPLATE_SEPARATOR);
        final List<String> actualParameters = new ArrayList<String>();

        for (int i = 0; i < templateParameters.length; i++) {
            final String parameter = templateParameters[i];

            if (parameter.startsWith("{")) { //$NON-NLS-1$
                final String name;

                if (parameter.startsWith("{*")) { //$NON-NLS-1$
                    name = parameter.substring(2, parameter.length() - 1);
                } else {
                    name = parameter.substring(1, parameter.length() - 1);
                }

                final String value = routeValues.get(name);
                if (!StringUtil.isNullOrEmpty(value)) {
                    actualParameters.add(value);
                }
            } else {
                actualParameters.add(parameter);
            }
        }

        return StringUtil.join(ROUTE_TEMPLATE_SEPARATOR, actualParameters);
    }

    private Map<String, String> toRouteDictionary(
        final Map<String, Object> routeValues,
        final String areaName,
        final String resourceName) {

        final HashMap<String, String> dictionary = new HashMap<String, String>();
        if (routeValues != null) {

            for (final Entry<String, Object> e : routeValues.entrySet()) {
                if (e.getValue() != null) {
                    dictionary.put(e.getKey(), e.getValue().toString());
                }
            }
        }

        if (!dictionary.containsKey(AREA_PARAMETER_NAME)) {
            dictionary.put(AREA_PARAMETER_NAME, areaName);
        }

        if (!dictionary.containsKey(RESOURCE_PARAMETER_NAME)) {
            dictionary.put(RESOURCE_PARAMETER_NAME, resourceName);
        }

        return dictionary;
    }

    private String getMediaTypeWithQualityHeaderValue(final String baseMediaType, final ApiResourceVersion version) {
        final Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(API_VERSION_PARAMETER_NAME, version.toString());
        parameters.put(CHARSET_PARAMETER_NAME, StringUtil.UTF8_CHARSET);

        return getMediaTypeWithQualityHeaderValue(baseMediaType, parameters);
    }

    private String getMediaTypeWithQualityHeaderValue(final String baseMediaType) {
        final Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(CHARSET_PARAMETER_NAME, StringUtil.UTF8_CHARSET);

        return getMediaTypeWithQualityHeaderValue(baseMediaType, parameters);
    }

    private String getMediaTypeWithQualityHeaderValue(
        final String baseMediaType,
        final Map<String, String> parameters) {
        final StringBuilder mediaType =
            new StringBuilder(StringUtil.isNullOrEmpty(baseMediaType) ? APPLICATION_JSON_TYPE : baseMediaType);

        for (final Entry<String, String> e : parameters.entrySet()) {
            addMediaTypeParameter(mediaType, e.getKey(), e.getValue());
        }

        return mediaType.toString();
    }

    private StringBuilder addMediaTypeParameter(
        final StringBuilder mediaType,
        final String parameterName,
        final String parameterValue) {
        mediaType.append(MEDIA_TYPE_PARAMETERS_SEPARATOR);
        mediaType.append(parameterName);
        mediaType.append("="); //$NON-NLS-1$
        mediaType.append(parameterValue);

        return mediaType;
    }

    private boolean isJsonResponse(final HttpMethodBase response) {
        if (response != null) {
            final Header responseContentTypeHeader = response.getResponseHeader(CONTENT_TYPE_HEADER);
            final Header contentLengthHeader = response.getResponseHeader(CONTENT_LENGTH_HEADER);

            if (responseContentTypeHeader != null && contentLengthHeader != null) {
                return APPLICATION_JSON_TYPE.equalsIgnoreCase(responseContentTypeHeader.getValue())
                    && !"0".equals(contentLengthHeader.getValue()); //$NON-NLS-1$
            }
        }

        return false;
    }

    /**
     * Negotiate the appropriate request version to use for the given api
     * resource location, based on the client and server capabilities
     *
     * @param location
     *        - Location of the API resource
     * @param version
     *        - Client version to attempt to use (use the latest VSS API version
     *        if unspecified)
     * @return - Max API version supported on the server that is less than or
     *         equal to the client version. Returns null if the server does not
     *         support this location or this version of the client.
     */
    protected ApiResourceVersion NegotiateRequestVersion(
        final ApiResourceLocation location,
        final ApiResourceVersion version) {

        if (version == null) {
            return DEFAULT_API_VERSION;
        }

        if (location.getMinVersion().compareTo(version.getApiVersion()) > 0) {
            // Client is older than the server. The server no longer supports
            // this resource (deprecated).
            return null;
        } else if (location.getMaxVersion().compareTo(version.getApiVersion()) < 0) {
            // Client is newer than the server. Negotiate down to the latest
            // version on the server.
            final ApiResourceVersion negotiatedVersion = new ApiResourceVersion(location.getMaxVersion());

            // If the server latest version is greater than the released one,
            // it is in preview mode.
            final boolean isPreview = location.getReleasedVersion().compareTo(location.getMaxVersion()) < 0;
            negotiatedVersion.setPreview(isPreview);

            return negotiatedVersion;
        } else {
            // We can send at the requested API version. Make sure the resource
            // version is not bigger than what the server supports.
            final int resourceVersion = Math.min(version.getResourceVersion(), location.getResourceVersion());
            final ApiResourceVersion negotiatedVersion =
                new ApiResourceVersion(version.getApiVersion(), resourceVersion);

            // If server released version is less than the requested one, the
            // negotiated version is in preview mode.
            if (location.getReleasedVersion().compareTo(version.getApiVersion()) < 0) {
                negotiatedVersion.setPreview(true);
            } else {
                negotiatedVersion.setPreview(version.isPreview());
            }

            return negotiatedVersion;
        }
    }

    protected ApiResourceVersion NegotiateRequestVersion(final ApiResourceLocation location) {
        return NegotiateRequestVersion(location, DEFAULT_API_VERSION);
    }

    protected ApiResourceVersion NegotiateRequestVersion(final UUID locationId, final ApiResourceVersion version) {
        return NegotiateRequestVersion(getLocation(locationId), version);
    }

    protected HttpMethodBase createRequest(
        final HttpMethod method,
        final UUID locationId,
        final Map<String, Object> routeValues,
        final ApiResourceVersion version,
        final Map<String, String> queryParameters) {
        return createRequest(
            method,
            locationId,
            routeValues,
            version,
            null,
            null,
            queryParameters,
            APPLICATION_JSON_TYPE);
    }

    protected HttpMethodBase createRequest(
        final HttpMethod method,
        final UUID locationId,
        final ApiResourceVersion version,
        final String acceptMediaType) {
        return createRequest(method, locationId, null, version, null, null, null, acceptMediaType);
    }

    protected HttpMethodBase createRequest(
        final HttpMethod method,
        final UUID locationId,
        final Map<String, Object> routeValues,
        final ApiResourceVersion version,
        final String acceptMediaType) {
        return createRequest(method, locationId, routeValues, version, null, null, null, acceptMediaType);
    }

    protected HttpMethodBase createRequest(
        final HttpMethod method,
        final UUID locationId,
        final Map<String, Object> routeValues,
        final ApiResourceVersion version,
        final Map<String, String> queryParameters,
        final String acceptMediaType) {
        return createRequest(method, locationId, routeValues, version, null, null, queryParameters, acceptMediaType);
    }

    protected HttpMethodBase createRequest(
        final HttpMethod method,
        final UUID locationId,
        final ApiResourceVersion version,
        final Map<String, String> queryParameters,
        final String acceptMediaType) {
        return createRequest(method, locationId, null, version, null, null, queryParameters, acceptMediaType);
    }

    protected <TEntity> HttpMethodBase createRequest(
        final HttpMethod method,
        final UUID locationId,
        final ApiResourceVersion version,
        final TEntity value,
        final String contentMediaType,
        final String acceptMediaType) {
        return createRequest(method, locationId, null, version, value, contentMediaType, null, acceptMediaType);
    }

    protected <TEntity> HttpMethodBase createRequest(
        final HttpMethod method,
        final UUID locationId,
        final Map<String, Object> routeValues,
        final ApiResourceVersion version,
        final TEntity value,
        final String contentMediaType,
        final String acceptMediaType) {
        return createRequest(method, locationId, routeValues, version, value, contentMediaType, null, acceptMediaType);
    }

    protected <TEntity> HttpMethodBase createRequest(
        final HttpMethod method,
        final UUID locationId,
        final ApiResourceVersion version,
        final TEntity value,
        final String contentMediaType,
        final Map<String, String> queryParameters,
        final String acceptMediaType) {
        return createRequest(
            method,
            locationId,
            null,
            version,
            value,
            contentMediaType,
            queryParameters,
            acceptMediaType);
    }

    protected <TEntity> HttpMethodBase createRequest(
        final HttpMethod method,
        final UUID locationId,
        final Map<String, Object> routeValues,
        final ApiResourceVersion version,
        final TEntity value,
        final Map<String, String> queryParameters) {
        return createRequest(
            method,
            locationId,
            routeValues,
            version,
            value,
            APPLICATION_JSON_TYPE,
            queryParameters,
            APPLICATION_JSON_TYPE);
    }

    protected <TEntity> HttpMethodBase createRequest(
        final HttpMethod method,
        final UUID locationId,
        final Map<String, Object> routeValues,
        final ApiResourceVersion version,
        final TEntity value,
        final String contentMediaType,
        final Map<String, String> queryParameters,
        final String acceptMediaType) {

        final URI target = createTarget(locationId, routeValues, queryParameters);
        final String acceptType =
            getMediaTypeWithQualityHeaderValue(acceptMediaType, NegotiateRequestVersion(locationId, version));

        final HttpMethodBase request;
        if (shouldOverrideHttpMethod(method)) {
            request = createHttpMethod(HttpMethod.POST, target);
            request.setRequestHeader(VssHttpHeaders.HTTP_METHOD_OVERRIDE, method.toString());
        } else {
            request = createHttpMethod(method, target);
        }

        request.setRequestHeader(VssHttpHeaders.ACCEPT, acceptType);
        request.setRequestHeader(VssHttpHeaders.TFS_VERSION, CoreVersionInfo.getVersion());

        if (request instanceof EntityEnclosingMethod) {
            final String contentType = getMediaTypeWithQualityHeaderValue(contentMediaType);
            final EntityEnclosingMethod entityMethod = (EntityEnclosingMethod) request;

            RequestEntity entity = null;

            if (value != null && (value instanceof InputStream)) {
                entity = new InputStreamRequestEntity((InputStream) value, contentMediaType);
            } else {
                final String content;
                if (value != null) {
                    content = JsonHelper.serializeRequestToString(value);
                } else {
                    content = StringUtil.EMPTY;
                }

                try {
                    entity = new StringRequestEntity(content, contentType, StringUtil.UTF8_CHARSET);
                } catch (final UnsupportedEncodingException e) {
                    // Note that this exception cannot happen at this point
                    log.error("Illegal content encoding found.", e); //$NON-NLS-1$
                }
            }

            entityMethod.setRequestEntity(entity);
            entityMethod.setRequestHeader(VssHttpHeaders.CONTENT_LENGTH, String.valueOf(entity.getContentLength()));
        }

        return request;
    }

    private HttpMethodBase createHttpMethod(final HttpMethod method, final URI target) {
        if (HttpMethod.GET.equals(method)) {
            return new GetMethod(target.toString());
        }

        if (HttpMethod.PUT.equals(method)) {
            return new PutMethod(target.toString());
        }

        if (HttpMethod.POST.equals(method)) {
            return new PostMethod(target.toString());
        }

        if (HttpMethod.DELETE.equals(method)) {
            return new DeleteMethod(target.toString());
        }

        if (HttpMethod.OPTIONS.equals(method)) {
            return new OptionsMethod(target.toString());
        }

        if (HttpMethod.PATCH.equals(method)) {
            return new PatchMethod(target.toString());
        }

        throw new IllegalArgumentException(
            MessageFormat.format(Messages.getString("VssHttpClientBase.IncorrectHttpMethodNameFormat"), method)); //$NON-NLS-1$
    }

    protected HttpMethodBase sendRequest(final HttpMethodBase request) {
        try {
            connection.getHTTPClient().executeMethod(request);
        } catch (final HttpException e) {
            log.error(e.getMessage(), e);
            throw new VssServiceException(e.getMessage(), e);
        } catch (final IOException e) {
            log.error(e.getMessage(), e);
        }

        handleResponse(request);

        return request;
    }

    protected void sendRequest(final Object request) {
        sendRequest((HttpMethodBase) request);
    }

    protected <TResult> TResult sendRequest(final Object request, final Class<TResult> resultClass) {
        final HttpMethodBase response = sendRequest((HttpMethodBase) request);
        return JsonHelper.deserializeResponce(response, resultClass);
    }

    protected <TResult> TResult sendRequest(final Object request, final TypeReference<TResult> resultClass) {
        final HttpMethodBase response = sendRequest((HttpMethodBase) request);
        return JsonHelper.deserializeResponce(response, resultClass);
    }

    protected void handleResponse(final HttpMethodBase response) {
        if (response.getStatusCode() == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
            throw new ProxyAuthenticationRequiredException();
        } else if (!HttpStatus.isSuccessFamily(response.getStatusCode())) {
            Exception exceptionToThrow = null;

            if (isJsonResponse(response)) {
                final WrappedException wrappedException =
                    JsonHelper.deserializeResponce(response, WrappedException.class);
                exceptionToThrow = wrappedException.Unwrap(getTranslatedExceptions());
            }

            if (exceptionToThrow == null || !(exceptionToThrow instanceof VssException)) {
                String message = null;

                if (exceptionToThrow != null) {
                    message = exceptionToThrow.getMessage();
                }

                final Header errorHeader = response.getResponseHeader(VssHttpHeaders.TFS_SERVICE_ERROR);

                if (errorHeader != null) {
                    try {
                        message = URLDecoder.decode(errorHeader.getValue(), "UTF-8"); //$NON-NLS-1$
                    } catch (final UnsupportedEncodingException e) {
                        // do nothing
                    }
                } else if (StringUtil.isNullOrEmpty(message) && !StringUtil.isNullOrEmpty(response.getStatusText())) {
                    message = response.getStatusText();
                }

                exceptionToThrow = new VssServiceResponseException(response.getStatusCode(), message, exceptionToThrow);
            }

            throw (VssException) exceptionToThrow;
        }
    }

    protected void addModelAsQueryParams(final NameValueCollection queryParams, final Object model) {
        if (model != null) {
            final Map<String, String> jSearchCriteria = JsonHelper.toQueryParametersMap(model);

            for (final Entry<String, String> property : jSearchCriteria.entrySet()) {
                if (!StringUtil.isNullOrEmpty(property.getValue())) {
                    queryParams.addIfNotEmpty(property.getKey(), property.getValue());
                }
            }
        }
    }

    private boolean shouldOverrideHttpMethod(final HttpMethod method) {
        if (this.isOverrideEnabled()) {
            return method.isOverrideable();
        }

        return false;
    }

    protected static enum HttpMethod {
        PATCH("PATCH", true), //$NON-NLS-1$
        GET("GET", false), //$NON-NLS-1$
        POST("POST", false), //$NON-NLS-1$
        PUT("PUT", true), //$NON-NLS-1$
        DELETE("DELETE", true), //$NON-NLS-1$
        HEAD("HEAD", false), //$NON-NLS-1$
        OPTIONS("OPTIONS", true); //$NON-NLS-1$

        private String verb;
        private boolean overrideable;

        private HttpMethod(final String verb, final boolean overrideable) {
            this.verb = verb;
            this.overrideable = overrideable;
        }

        public boolean isOverrideable() {
            return this.overrideable;
        }

        public String getVerb() {
            return this.verb;
        }
    }
}

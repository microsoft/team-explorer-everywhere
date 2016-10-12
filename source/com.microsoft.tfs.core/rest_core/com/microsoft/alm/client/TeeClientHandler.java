// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.alm.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.microsoft.alm.client.model.VssResourceNotFoundException;
import com.microsoft.alm.client.model.VssServiceException;
import com.microsoft.alm.client.utils.JsonHelper;
import com.microsoft.alm.visualstudio.services.webapi.ApiResourceLocation;
import com.microsoft.alm.visualstudio.services.webapi.ApiResourceLocationCollection;
import com.microsoft.alm.visualstudio.services.webapi.ApiResourceVersion;
import com.microsoft.tfs.core.Messages;
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

public class TeeClientHandler extends VssRestClientHandlerBase implements VssRestClientHandler {
    private static final Log log = LogFactory.getLog(TeeClientHandler.class);

    private final static String MEDIA_TYPE_PARAMETERS_SEPARATOR = ";"; //$NON-NLS-1$

    private final HttpClient httpClient;

    public TeeClientHandler(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public boolean checkConnection() {
        log.debug("Checking REST client connection"); //$NON-NLS-1$

        final URI connectionDataTarget = URIUtils.resolve(getBaseUrl(), CONNECTION_DATA_RELATIVE_PATH);
        final HttpMethodBase request = createHttpMethod(HttpMethod.GET, connectionDataTarget);
        request.setFollowRedirects(true);

        try {
            request.setRequestHeader(VssHttpHeaders.ACCEPT, VssMediaTypes.APPLICATION_JSON_TYPE);
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
            setLastException(e);
        } finally {
            request.releaseConnection();
        }

        return false;
    }

    @Override
    public ApiResourceLocationCollection loadLocations() {
        final URI optionsTarget = URIUtils.resolve(getBaseUrl(), OPTIONS_RELATIVE_PATH);
        final HttpMethodBase request = createHttpMethod(HttpMethod.OPTIONS, optionsTarget);
        request.setFollowRedirects(true);

        try {
            request.setRequestHeader(VssHttpHeaders.ACCEPT, VssMediaTypes.APPLICATION_JSON_TYPE);
            final int statusCode = httpClient.executeMethod(request);

            if (HttpStatus.isSuccessFamily(statusCode)) {
                return JsonHelper.deserializeResponce(request, ApiResourceLocationCollection.class);
            } else {
                throw new HttpException(HttpStatus.getStatusText(statusCode));
            }
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
            setLastException(e);
            return null;
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public <TEntity> VssRestRequest createRequest(
        final HttpMethod method,
        final UUID locationId,
        final Map<String, Object> routeValues,
        final ApiResourceVersion version,
        final TEntity value,
        final String contentMediaType,
        final Map<String, String> queryParameters,
        final String acceptMediaType) {

        final URI target = createTarget(locationId, routeValues, queryParameters);
        return createRequest(
            method,
            target,
            locationId,
            version,
            value,
            contentMediaType,
            queryParameters,
            acceptMediaType);
    }

    protected <TEntity> VssRestRequest createRequest(
        final HttpMethod method,
        final URI target,
        final UUID locationId,
        final ApiResourceVersion version,
        final TEntity value,
        final String contentMediaType,
        final Map<String, String> queryParameters,
        final String acceptMediaType) {

        final String acceptType =
            getMediaTypeWithQualityHeaderValue(acceptMediaType, negotiateRequestVersion(locationId, version));

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

        return new TeeRestRequest(request);
    }

    private URI createTarget(
        final UUID locationId,
        final Map<String, Object> routeValues,
        final Map<String, String> queryParameters) {

        final ApiResourceLocation location = getLocation(locationId);
        if (location == null) {
            throw new VssResourceNotFoundException(locationId, getBaseUrl(), getLastException());
        }

        final Map<String, Object> dictionary =
            toRouteDictionary(routeValues, location.getArea(), location.getResourceName());

        final String routeTemplate = location.getRouteTemplate();
        final String actualTemplate = replaceRouteValues(routeTemplate, dictionary);
        final URI target = URIUtils.resolve(getBaseUrl(), actualTemplate);

        return URIUtils.addQueryParameters(target, queryParameters);
    }

    private String replaceRouteValues(final String template, final Map<String, Object> routeValues) {
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

                final Object value = routeValues.get(name);
                if (value != null) {
                    actualParameters.add(value.toString());
                }
            } else {
                actualParameters.add(parameter);
            }
        }

        return StringUtil.join(ROUTE_TEMPLATE_SEPARATOR, actualParameters);
    }

    private String getMediaTypeWithQualityHeaderValue(final String baseMediaType, final ApiResourceVersion version) {
        final Map<String, String> parameters = new HashMap<String, String>();
        if (version != null) {
            parameters.put(API_VERSION_PARAMETER_NAME, version.toString());
        }
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
        final StringBuilder mediaType = new StringBuilder(
            StringUtil.isNullOrEmpty(baseMediaType) ? VssMediaTypes.APPLICATION_JSON_TYPE : baseMediaType);

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

    public class TeeRestRequest implements VssRestRequest {

        final private HttpMethodBase request;

        public TeeRestRequest(final HttpMethodBase request) {
            this.request = request;
        }

        @Override
        public VssRestResponse sendRequest() {

            try {
                httpClient.executeMethod(request);
            } catch (final HttpException e) {
                log.error(e.getMessage(), e);
                throw new VssServiceException(e.getMessage(), e);
            } catch (final IOException e) {
                log.error(e.getMessage(), e);
            }

            return new TeeRestResponse(request);
        }

        public void setFollowRedirects(final boolean followRedirects) {
            request.setFollowRedirects(followRedirects);
        }
    }

    public class TeeRestResponse implements VssRestResponse {

        final private HttpMethodBase response;

        public TeeRestResponse(final HttpMethodBase response) {
            this.response = response;
        }

        @Override
        public boolean isJsonResponse() {
            final Header responseContentTypeHeader = response.getResponseHeader(VssHttpHeaders.CONTENT_TYPE_HEADER);
            final Header contentLengthHeader = response.getResponseHeader(VssHttpHeaders.CONTENT_LENGTH_HEADER);

            if (responseContentTypeHeader != null && contentLengthHeader != null) {
                return StringUtil.startsWithIgnoreCase(
                    responseContentTypeHeader.getValue(),
                    VssMediaTypes.APPLICATION_JSON_TYPE) && !"0".equals(contentLengthHeader.getValue()); //$NON-NLS-1$
            }

            return false;
        }

        @Override
        public boolean isProxyAuthRequired() {
            return response.getStatusCode() == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED;
        }

        @Override
        public boolean isSuccessResponse() {
            return HttpStatus.isSuccessFamily(response.getStatusCode());
        }

        @Override
        public <TEntity> TEntity readEntity(Class<TEntity> resultClass) {
            return JsonHelper.deserializeResponce(response, resultClass);
        }

        @Override
        public <TEntity> TEntity readEntity(TypeReference<TEntity> resultClass) {
            return JsonHelper.deserializeResponce(response, resultClass);
        }

        @Override
        public String getHeader(String headerName) {
            return response.getResponseHeader(headerName).getValue();
        }

        @Override
        public String getStatusText() {
            return response.getStatusText();
        }

        @Override
        public int getStatusCode() {
            return response.getStatusCode();
        }
    }

}

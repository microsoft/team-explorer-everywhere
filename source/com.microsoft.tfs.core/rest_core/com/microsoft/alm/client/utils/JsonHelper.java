// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.alm.client.utils;

/**
 * Latest versions of Jackson Core is available on the Maven central repository
 * http://repo1.maven.org/maven2/com/fasterxml/jackson/core/
 */

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microsoft.alm.client.VssHttpHeaders;
import com.microsoft.alm.client.model.VssServiceException;
import com.microsoft.alm.visualstudio.services.webapi.VssJsonCollectionWrapper;
import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.DownloadContentTypes;
import com.microsoft.tfs.core.httpclient.Header;
import com.microsoft.tfs.core.httpclient.HttpMethodBase;

public class JsonHelper {
    private static final Log log = LogFactory.getLog(JsonHelper.class);

    private final static ObjectMapper objectMapper;
    private final static SimpleDateFormat dateFormat;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);

        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

        objectMapper.setSerializationInclusion(Include.NON_NULL);

        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //$NON-NLS-1$
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$

        objectMapper.setDateFormat(dateFormat);
    }

    /**
     * Get DateFormat
     *
     * @return DateFormat
     */
    public static DateFormat getDateFormat() {
        return dateFormat;
    }

    /**
     * Get ObjectMapper
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Map to QueryParameters
     *
     * @param model
     * @return Map<String, String>
     */
    public static Map<String, String> toQueryParametersMap(final Object model) {
        final ObjectMapper objectMapper = getObjectMapper();

        try {
            return objectMapper.readValue(
                objectMapper.writeValueAsString(model),
                new TypeReference<Map<String, String>>() {
                });
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
            throw new VssServiceException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserializeResponce(final HttpMethodBase response, final Class<T> clazz) {
        try {
            if (!InputStream.class.isAssignableFrom(clazz)) {
                final byte[] input = response.getResponseBody();

                if (input == null || input.length == 0) {
                    return null;
                }

                return objectMapper.readValue(input, clazz);
            } else if (response.getResponseContentLength() == 0) {
                return null;
            } else {
                final InputStream responseStream = response.getResponseBodyAsStream();
                final Header contentTypeHeader = response.getResponseHeader(VssHttpHeaders.CONTENT_TYPE);

                if (contentTypeHeader != null
                    && contentTypeHeader.getValue().equalsIgnoreCase(DownloadContentTypes.APPLICATION_GZIP)) {
                    return (T) new GZIPInputStream(responseStream);
                } else {
                    return (T) responseStream;
                }
            }
        } catch (final IOException e) {
            log.error(e.getMessage(), e);
            throw new VssServiceException(e.getMessage(), e);
        } finally {
            response.releaseConnection();
        }
    }

    public static <T> T deserializeResponce(final HttpMethodBase response, final TypeReference<T> genericType) {
        try {
            final byte[] input = response.getResponseBody();

            if (input == null || input.length == 0) {
                return null;
            } else {
                if (isArrayType(genericType)) {
                    final JavaType rootType = objectMapper.getTypeFactory().constructParametricType(
                        VssJsonCollectionWrapper.class,
                        objectMapper.constructType(genericType.getType()));

                    final VssJsonCollectionWrapper<T> result = objectMapper.readValue(input, rootType);
                    return result.getValue();
                } else {
                    return objectMapper.readValue(input, genericType);
                }
            }
        } catch (final IOException e) {
            log.error(e.getMessage(), e);
            throw new VssServiceException(e.getMessage(), e);
        } finally {
            response.releaseConnection();
        }
    }
    
    private static <T> boolean isArrayType(final TypeReference<T> genericType) {
        
        final Type type = genericType.getType();
        
        if (genericType.getType() instanceof ParameterizedType) {
            final Type rawType = ((ParameterizedType) genericType.getType()).getRawType();
            return (rawType == ArrayList.class) || (rawType == List.class); 
        }
        
        return false;
    }

    public static String serializeRequestToString(final Object entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (final IOException e) {
            log.error(e.getMessage(), e);
            throw new IllegalArgumentException(
                MessageFormat.format(
                    Messages.getString("JsonHelper.CannotSerializeContentFormat"), //$NON-NLS-1$
                    entity.getClass().getName()),
                e);
        }
    }

}

// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.git.json;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonHelper {
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
     * Get ObjectMapper
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}

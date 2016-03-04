// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.teamfoundation.distributedtask.webapi.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class DemandSerializer extends JsonSerializer<Demand> {

    /**
     * Serializes ReferenceLinks to JSON
     *
     * @param value
     * @param writer
     * @param serializer
     * @throws IOException
     * @throws JsonProcessingException
     */
    @Override
    public void serialize(final Demand value, final JsonGenerator writer, final SerializerProvider serializer)
        throws IOException,
            JsonProcessingException {
        writer.writeObject(value.toString());
    }
}

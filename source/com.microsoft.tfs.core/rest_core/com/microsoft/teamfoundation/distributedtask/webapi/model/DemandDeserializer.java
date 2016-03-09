// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.teamfoundation.distributedtask.webapi.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DemandDeserializer extends JsonDeserializer<Demand> {

    /**
     * Deserializes JSON to Demand
     *
     * @param parser
     * @param context
     * @return Demand
     * @throws IOException
     * @throws JsonProcessingException
     */
    @Override
    public Demand deserialize(final JsonParser parser, final DeserializationContext context)
        throws IOException,
            JsonProcessingException {
        Demand result = null;

        if (parser.getCurrentToken().equals(JsonToken.VALUE_STRING)) {
            result = Demand.tryParse(parser.readValueAs(String.class));
        } else {
            // consume this stream
            final ObjectMapper mapper = (ObjectMapper) parser.getCodec();
            mapper.readTree(parser);
        }

        return result;
    }
}

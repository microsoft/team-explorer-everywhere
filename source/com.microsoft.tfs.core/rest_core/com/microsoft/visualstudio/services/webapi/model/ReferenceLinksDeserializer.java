// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.webapi.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.tfs.core.Messages;
import com.microsoft.vss.client.core.utils.StringUtil;

public class ReferenceLinksDeserializer extends JsonDeserializer<ReferenceLinks> {

    /**
     * Deserializes JSON to ReferenceLinks
     *
     * @param parser
     * @param context
     * @return ReferenceLinks
     * @throws IOException
     * @throws JsonProcessingException
     */
    @Override
    public ReferenceLinks deserialize(final JsonParser parser, final DeserializationContext context)
        throws IOException,
            JsonProcessingException {

        final ReferenceLinks result = new ReferenceLinks();

        if (parser.getCurrentToken().equals(JsonToken.START_OBJECT)) {
            final Map<String, Object> links = new HashMap<String, Object>();

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                // Read the reference link key. We know this is a string
                // because the JsonReader validates that the first token
                // is a property name, and it has to be a string.
                final String name = parser.getCurrentName();

                if (StringUtil.isNullOrEmpty(name)) {
                    throw new IOException(Messages.getString("ReferenceLinksDeserializer.InvalidReferenceLink")); //$NON-NLS-1$
                }

                // Now read the reference link
                parser.nextToken();

                // Start object means we have just one reference link
                if (parser.getCurrentToken().equals(JsonToken.START_OBJECT)) {
                    links.put(name, parser.readValueAs(ReferenceLink.class));
                }
                // Start array means we have a list of reference links.
                else if (parser.getCurrentToken().equals(JsonToken.START_ARRAY)) {
                    final List<ReferenceLink> values = parser.readValueAs(new TypeReference<List<ReferenceLink>>() {
                    });
                    links.put(name, values);
                } else {
                    throw new IOException(Messages.getString("ReferenceLinksDeserializer.InvalidReferenceLink")); //$NON-NLS-1$
                }
            }

            result.setLinks(links);
        } else {
            // consume this stream
            final ObjectMapper mapper = (ObjectMapper) parser.getCodec();
            mapper.readTree(parser);
        }

        return result;
    }
}

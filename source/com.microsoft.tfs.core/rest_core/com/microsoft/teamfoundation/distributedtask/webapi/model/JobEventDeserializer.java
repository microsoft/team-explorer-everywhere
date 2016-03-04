// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.teamfoundation.distributedtask.webapi.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 *
 * @threadsafety unknown
 */
public class JobEventDeserializer extends JsonDeserializer<JobEvent> {

    /**
     * {@inheritDoc}
     */
    @Override
    public JobEvent deserialize(final JsonParser parser, final DeserializationContext context)
        throws IOException,
            JsonProcessingException {

        final ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        final JsonNode rootNode = (JsonNode) mapper.readTree(parser);

        final JsonNode nameNode = rootNode.findValue("Name"); //$NON-NLS-1$

        if (nameNode != null) {
            if (nameNode.isTextual() && "JobAssigned".equals(nameNode.asText())) //$NON-NLS-1$
            {
                return rootNode.traverse(mapper).readValueAs(JobAssignedEvent.class);
            }

            if (nameNode.isTextual() && "JobCompleted".equals(nameNode.asText())) //$NON-NLS-1$
            {
                return rootNode.traverse(mapper).readValueAs(JobCompletedEvent.class);
            }
        }

        final JsonNode requestNode = rootNode.findValue("Request"); //$NON-NLS-1$

        if (requestNode != null) {
            return rootNode.traverse(mapper).readValueAs(JobAssignedEvent.class);
        }

        final JsonNode resultNode = rootNode.findValue("Result"); //$NON-NLS-1$

        if (resultNode != null) {
            return rootNode.traverse(mapper).readValueAs(JobCompletedEvent.class);
        }

        return null;
    }

}

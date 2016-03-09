// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.teamfoundation.build.webapi.model;

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
public class BuildTriggerDeserializer extends JsonDeserializer<BuildTrigger> {

    /**
     * {@inheritDoc}
     */
    @Override
    public BuildTrigger deserialize(final JsonParser parser, final DeserializationContext context)
        throws IOException,
            JsonProcessingException {

        final ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        final JsonNode rootNode = (JsonNode) mapper.readTree(parser);

        final JsonNode typeNode = rootNode.findValue("TriggerType"); //$NON-NLS-1$

        if (typeNode != null) {
            DefinitionTriggerType triggerType = null;

            if (typeNode.isInt() && typeNode.asInt() == DefinitionTriggerType.CONTINUOUS_INTEGRATION.getValue()) {
                triggerType = DefinitionTriggerType.CONTINUOUS_INTEGRATION;
            } else if (typeNode.isInt() && typeNode.asInt() == DefinitionTriggerType.SCHEDULE.getValue()) {
                triggerType = DefinitionTriggerType.SCHEDULE;
            } else if (typeNode.isTextual()
                && DefinitionTriggerType.CONTINUOUS_INTEGRATION.toString().equalsIgnoreCase(typeNode.asText())) {
                triggerType = DefinitionTriggerType.CONTINUOUS_INTEGRATION;
            } else if (typeNode.isTextual()
                && DefinitionTriggerType.SCHEDULE.toString().equalsIgnoreCase(typeNode.asText())) {
                triggerType = DefinitionTriggerType.SCHEDULE;
            }

            if (DefinitionTriggerType.SCHEDULE == triggerType) {
                return rootNode.traverse(mapper).readValueAs(ScheduleTrigger.class);
            }

            if (DefinitionTriggerType.CONTINUOUS_INTEGRATION == triggerType) {
                return rootNode.traverse(mapper).readValueAs(ContinuousIntegrationTrigger.class);
            }
        }

        return null;
    }
}

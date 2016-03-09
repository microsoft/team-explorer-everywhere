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
public class TaskOrchestrationItemDeserializer extends JsonDeserializer<TaskOrchestrationItem> {

    /**
     * {@inheritDoc}
     */
    @Override
    public TaskOrchestrationItem deserialize(final JsonParser parser, final DeserializationContext context)
        throws IOException,
            JsonProcessingException {

        final ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        final JsonNode rootNode = (JsonNode) mapper.readTree(parser);

        final JsonNode itenTypeNode = rootNode.findValue("ItemType"); //$NON-NLS-1$

        if (itenTypeNode != null) {
            TaskOrchestrationItemType itemType = null;

            if (itenTypeNode.isInt() && itenTypeNode.asInt() == TaskOrchestrationItemType.CONTAINER.getValue()) {
                itemType = TaskOrchestrationItemType.CONTAINER;
            } else if (itenTypeNode.isInt() && itenTypeNode.asInt() == TaskOrchestrationItemType.JOB.getValue()) {
                itemType = TaskOrchestrationItemType.JOB;
            } else if (itenTypeNode.isTextual()
                && TaskOrchestrationItemType.CONTAINER.toString().equalsIgnoreCase(itenTypeNode.asText())) {
                itemType = TaskOrchestrationItemType.CONTAINER;
            } else if (itenTypeNode.isTextual()
                && TaskOrchestrationItemType.JOB.toString().equalsIgnoreCase(itenTypeNode.asText())) {
                itemType = TaskOrchestrationItemType.JOB;
            }

            if (TaskOrchestrationItemType.JOB == itemType) {
                return rootNode.traverse(mapper).readValueAs(TaskOrchestrationJob.class);
            }

            if (TaskOrchestrationItemType.CONTAINER == itemType) {
                return rootNode.traverse(mapper).readValueAs(TaskOrchestrationContainer.class);
            }
        }

        return null;
    }
}

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

public class DefinitionReferenceDeserializer extends JsonDeserializer<DefinitionReference> {
    /**
     * Deserializes JSON to DefinitionReference
     *
     * @param parser
     * @param context
     * @return DefinitionReference
     * @throws IOException
     * @throws JsonProcessingException
     */
    @Override
    public DefinitionReference deserialize(final JsonParser parser, final DeserializationContext context)
        throws IOException,
            JsonProcessingException {

        final ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        final JsonNode rootNode = (JsonNode) mapper.readTree(parser);

        final JsonNode typeNode = rootNode.findValue("type"); //$NON-NLS-1$

        if (typeNode != null) {
            DefinitionType definitionType = DefinitionType.BUILD;
            if (typeNode.isInt() && typeNode.asInt() == DefinitionType.XAML.getValue()) {
                definitionType = DefinitionType.XAML;
            } else if (typeNode.isTextual() && DefinitionType.XAML.toString().equalsIgnoreCase(typeNode.asText())) {
                definitionType = DefinitionType.XAML;
            } else {
                definitionType = DefinitionType.BUILD;
            }

            switch (definitionType) {
                case XAML:
                    return rootNode.traverse(mapper).readValueAs(XamlBuildDefinition.class);
                default:
                    try {
                        return rootNode.traverse(mapper).readValueAs(BuildDefinition.class);
                    } catch (final Exception e) {
                        // do nothing and try another type
                    }
                    try {
                        return rootNode.traverse(mapper).readValueAs(BuildDefinitionReference.class);
                    } catch (final Exception e) {
                        // do nothing
                    }
                    break;
            }
        }

        return null;
    }
}

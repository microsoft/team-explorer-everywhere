// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.vss.client.core.json.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.microsoft.vss.client.core.json.JObject;

public class JObjectSerializer extends JsonSerializer<JObject> {

    @Override
    public void serialize(final JObject value, final JsonGenerator writer, final SerializerProvider serializer)
        throws IOException,
            JsonProcessingException {
        writer.writeTree(value.getRoot());
    }

}

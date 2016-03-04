// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.vss.client.core.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.microsoft.vss.client.core.json.serialization.JObjectDeserializer;
import com.microsoft.vss.client.core.json.serialization.JObjectSerializer;

@JsonDeserialize(using = JObjectDeserializer.class)
@JsonSerialize(using = JObjectSerializer.class)
public class JObject {

    JsonNode root;

    public JsonNode getRoot() {
        return root;
    }

    public void setRoot(final JsonNode root) {
        this.root = root;
    }
}

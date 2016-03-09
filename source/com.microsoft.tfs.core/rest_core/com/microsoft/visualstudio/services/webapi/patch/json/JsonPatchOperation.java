// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.webapi.patch.json;

import java.util.ArrayList;
import com.microsoft.visualstudio.services.webapi.patch.Operation;

/**
 * The JSON model for JSON Patch Operation
 */
public class JsonPatchOperation {
    private Operation op;
    private String path;
    private String from;
    private Object value;

    /**
     * The patch operation
     */
     public Operation getOp() {
        return op;
     }

    /**
     * The patch operation
     */
     public void setOp(final Operation op) {
        this.op = op;
     }

    /**
     * The path for the operation
     */
     public Operation getPath() {
        return op;
     }

    /**
     * The path for the operation
     */
     public void setPath(final String path) {
        this.path = path;
     }

    /**
     * The path to copy from for the Move/Copy operation.
     */
     public String getFrom() {
        return from;
     }

    /**
     * The path to copy from for the Move/Copy operation.
     */
     public void setFrom(final String from) {
        this.from = from;
     }

    /**
     * The value for the operation.
     * This is either a primitive or a JToken.
     */
     public Object getValue() {
        return value;
     }

    /**
     * The value for the operation.
     * This is either a primitive or a JToken.
     */
     public void setValue(final Object value) {
        this.value = value;
     }
}

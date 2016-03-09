// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query.qe;

public interface WIQLTranslatorFieldService {
    public String getLocalizedFieldName(String fieldName);

    public String getInvariantFieldName(String fieldName);

    public boolean isDateTimeField(String fieldName);

    public boolean isDecimalField(String fieldName);

    public boolean isStringField(String fieldName);
}

// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.prompt;

import com.microsoft.tfs.util.TypesafeEnum;

public class QuestionResponse extends TypesafeEnum {
    public static final QuestionResponse YES = new QuestionResponse(0);
    public static final QuestionResponse NO = new QuestionResponse(1);
    public static final QuestionResponse ALL = new QuestionResponse(2);
    public static final QuestionResponse CANCEL = new QuestionResponse(3);

    private QuestionResponse(final int value) {
        super(value);
    }
}

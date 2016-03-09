// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.prompt;

import com.microsoft.tfs.util.TypesafeEnum;

public class QuestionType extends TypesafeEnum {
    public static final QuestionType YES_NO = new QuestionType(0);
    public static final QuestionType YES_NO_ALL = new QuestionType(1);
    public static final QuestionType YES_NO_CANCEL = new QuestionType(2);
    public static final QuestionType YES_NO_ALL_CANCEL = new QuestionType(3);

    private QuestionType(final int value) {
        super(value);
    }
}

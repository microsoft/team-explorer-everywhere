// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

 /*
* ---------------------------------------------------------
 * --------------------------------------------------------- Generated file, DO
 * NOT EDIT ---------------------------------------------------------
 */
package com.microsoft.visualstudio.services.forminput.model;

import java.util.List;

public class InputValues {

    private String defaultValue;
    private InputValuesError error;
    private String inputId;
    private boolean isDisabled;
    private boolean isLimitedToPossibleValues;
    private boolean isReadOnly;
    private List<InputValue> possibleValues;

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(final String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public InputValuesError getError() {
        return error;
    }

    public void setError(final InputValuesError error) {
        this.error = error;
    }

    public String getInputId() {
        return inputId;
    }

    public void setInputId(final String inputId) {
        this.inputId = inputId;
    }

    public boolean getIsDisabled() {
        return isDisabled;
    }

    public void setIsDisabled(final boolean isDisabled) {
        this.isDisabled = isDisabled;
    }

    public boolean getIsLimitedToPossibleValues() {
        return isLimitedToPossibleValues;
    }

    public void setIsLimitedToPossibleValues(final boolean isLimitedToPossibleValues) {
        this.isLimitedToPossibleValues = isLimitedToPossibleValues;
    }

    public boolean getIsReadOnly() {
        return isReadOnly;
    }

    public void setIsReadOnly(final boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    public List<InputValue> getPossibleValues() {
        return possibleValues;
    }

    public void setPossibleValues(final List<InputValue> possibleValues) {
        this.possibleValues = possibleValues;
    }
}

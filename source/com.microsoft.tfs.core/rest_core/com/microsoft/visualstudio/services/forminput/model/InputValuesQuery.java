// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

 /*
* ---------------------------------------------------------
 * --------------------------------------------------------- Generated file, DO
 * NOT EDIT ---------------------------------------------------------
 */
package com.microsoft.visualstudio.services.forminput.model;

import java.util.HashMap;
import java.util.List;

public class InputValuesQuery {

    private HashMap<String, String> currentValues;
    private List<InputValues> inputValues;
    private Object resource;

    public HashMap<String, String> getCurrentValues() {
        return currentValues;
    }

    public void setCurrentValues(final HashMap<String, String> currentValues) {
        this.currentValues = currentValues;
    }

    public List<InputValues> getInputValues() {
        return inputValues;
    }

    public void setInputValues(final List<InputValues> inputValues) {
        this.inputValues = inputValues;
    }

    public Object getResource() {
        return resource;
    }

    public void setResource(final Object resource) {
        this.resource = resource;
    }
}

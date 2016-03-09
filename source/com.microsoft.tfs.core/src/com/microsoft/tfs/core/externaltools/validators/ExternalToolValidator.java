// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools.validators;

import com.microsoft.tfs.core.externaltools.ExternalTool;

/**
 * Validates command line arguments for an external tool.
 *
 * @since TEE-SDK-10.1
 */
public interface ExternalToolValidator {
    /**
     * Tests whether the command and arguments for the given
     * {@link ExternalToolException} satisfy the validator's requirements. For
     * example, some substitution strings may be required in arguments, others
     * may be forbidden. The method throws if the tool is invalid, it does not
     * throw if the tool is valid.
     *
     * @param externalTool
     *        the tool to validate (must not be <code>null</code>)
     * @throws ExternalToolException
     *         if the tool is invalid
     */
    public void validate(ExternalTool externalTool) throws ExternalToolException;
}

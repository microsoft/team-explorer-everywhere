// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions.mappers;

import com.microsoft.tfs.core.exceptions.TFSAccessException;
import com.microsoft.tfs.core.ws.runtime.exceptions.SOAPFault;

/**
 * Mapper for TFS location service exceptions.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class LocationExceptionMapper extends TECoreExceptionMapper {
    public static RuntimeException map(final RuntimeException e) {
        if (e instanceof SOAPFault
            && ((SOAPFault) e).getSubCode() != null
            && ((SOAPFault) e).getSubCode().getSubCode() != null) {
            if ("AccessCheckException".equals(((SOAPFault) e).getSubCode().getSubCode().getLocalPart())) //$NON-NLS-1$
            {
                throw new TFSAccessException((SOAPFault) e);
            }
        }

        return TECoreExceptionMapper.map(e);
    }
}

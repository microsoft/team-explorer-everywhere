// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies;

import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PolicyFailureInfo;
import com.microsoft.tfs.util.Check;

public abstract class PolicyFailureData {
    public static PolicyFailureData fromPolicyFailure(final PolicyFailure policyFailure, final PolicyContext context) {
        return new RealPolicyFailure(policyFailure, context);
    }

    public static PolicyFailureData fromPolicyFailureInfo(final PolicyFailureInfo policyFailureInfo) {
        return new HistoricalPolicyFailure(policyFailureInfo);
    }

    public static PolicyFailureData[] fromPolicyFailures(
        final PolicyFailure[] policyFailures,
        final PolicyContext context) {
        Check.notNull(policyFailures, "policyFailures"); //$NON-NLS-1$
        final PolicyFailureData[] ret = new PolicyFailureData[policyFailures.length];

        for (int i = 0; i < policyFailures.length; i++) {
            ret[i] = fromPolicyFailure(policyFailures[i], context);
        }

        return ret;
    }

    public abstract String getMessage();

    public abstract void activate();

    public abstract void displayHelp();

    private static class HistoricalPolicyFailure extends PolicyFailureData {
        private final PolicyFailureInfo failure;

        public HistoricalPolicyFailure(final PolicyFailureInfo failure) {
            Check.notNull(failure, "failure"); //$NON-NLS-1$
            this.failure = failure;
        }

        @Override
        public String getMessage() {
            return failure.getMessage();
        }

        @Override
        public void activate() {
        }

        @Override
        public void displayHelp() {
        }
    }

    private static class RealPolicyFailure extends PolicyFailureData {
        private final PolicyFailure failure;
        private final PolicyContext context;

        public RealPolicyFailure(final PolicyFailure failure, final PolicyContext context) {
            Check.notNull(failure, "failure"); //$NON-NLS-1$
            Check.notNull(context, "context"); //$NON-NLS-1$

            this.failure = failure;
            this.context = context;
        }

        @Override
        public String getMessage() {
            return failure.getMessage();
        }

        @Override
        public void activate() {
            failure.getPolicy().activate(failure, context);
        }

        @Override
        public void displayHelp() {
            failure.getPolicy().displayHelp(failure, context);
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }

            if (!(obj instanceof RealPolicyFailure)) {
                return false;
            }

            final PolicyFailure f1 = failure;
            final PolicyFailure f2 = ((RealPolicyFailure) obj).failure;

            if (!f1.getPolicy().getPolicyType().equals(f2.getPolicy().getPolicyType())) {
                return false;
            }

            final String message1 = f1.getMessage();
            final String message2 = f2.getMessage();

            return message1 == null ? message2 == null : message1.equals(message2);
        }

        @Override
        public int hashCode() {
            int result = 17;

            result = result * 37 + failure.getPolicy().getPolicyType().hashCode();
            final String message = failure.getMessage();
            result = result * 37 + ((message == null) ? 0 : message.hashCode());

            return result;
        }
    }
}

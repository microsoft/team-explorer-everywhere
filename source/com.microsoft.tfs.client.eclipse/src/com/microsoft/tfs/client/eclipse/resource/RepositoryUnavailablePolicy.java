// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resource;

import java.text.MessageFormat;

import org.eclipse.core.resources.IResource;

public abstract class RepositoryUnavailablePolicy {
    public static final RepositoryUnavailablePolicy THROW = new Throw();
    public static final RepositoryUnavailablePolicy ACCEPT_RESOURCE = new Accept();
    public static final RepositoryUnavailablePolicy REJECT_RESOURCE = new Reject();

    private RepositoryUnavailablePolicy() {

    }

    public abstract boolean acceptResourceWithNoRepository(IResource resource);

    private static class Throw extends RepositoryUnavailablePolicy {
        @Override
        public boolean acceptResourceWithNoRepository(final IResource resource) {
            throw new IllegalArgumentException(MessageFormat.format(
                "Resource {0} does not have a repository", //$NON-NLS-1$
                resource));
        }
    }

    private static class Accept extends RepositoryUnavailablePolicy {
        @Override
        public boolean acceptResourceWithNoRepository(final IResource resource) {
            return true;
        }
    }

    private static class Reject extends RepositoryUnavailablePolicy {
        @Override
        public boolean acceptResourceWithNoRepository(final IResource resource) {
            return false;
        }
    }
}

// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.delegatedauthorization;

import java.util.Arrays;
import java.util.UUID;

import com.microsoft.visualstudio.services.delegatedauthorization.model.SessionToken;

public class DelegatedAuthorizationHttpClient extends DelegatedAuthorizationHttpClientBase {

    public DelegatedAuthorizationHttpClient(final Object tfsConnection) {
        super(tfsConnection);
    }

    public SessionToken createAccountCodeAccessToken(final String displayName, final UUID accountId) {

        final SessionToken token = new SessionToken();

        token.setDisplayName(displayName);
        token.setScope(TokenScope.combine(TokenScope.CODE_READ, TokenScope.CODE_WRITE, TokenScope.CODE_MANAGE));
        token.setTargetAccounts(Arrays.asList(new String[] {
            accountId.toString()
        }));

        return createSessionToken(token);
    }

    /**
     * VisualStudioOnline Personal Access Token scopes
     */
    private enum TokenScope {
        /**
         * Grants the ability to access build artifacts, including build
         * results, definitions, and requests, and the ability to receive
         * notifications about build events via service hooks.
         */
        BUILD_ACCESS("vso.build"), //$NON-NLS-1$

        /**
         * Grants the ability to access build artifacts, including build
         * results, definitions, and requests, and the ability to queue a build,
         * update build properties, and the ability to receive notifications
         * about build events via service hooks.
         */
        BUILD_EXECUTE("vso.build_execute"), //$NON-NLS-1$

        /**
         * Grants the ability to access rooms and view, post, and update
         * messages. Also grants the ability to manage rooms and users and to
         * receive notifications about new messages via service hooks.
         */
        CHAT_MANAGE("vso.chat_manage"), //$NON-NLS-1$

        /**
         * Grants the ability to access rooms and view, post, and update
         * messages. Also grants the ability to receive notifications about new
         * messages via service hooks.
         */
        CHAT_WRITE("vso.chat_write"), //$NON-NLS-1$

        /**
         * Grants the ability to read, update, and delete source code, access
         * metadata about commits, changesets, branches, and other version
         * control artifacts. Also grants the ability to create and manage code
         * repositories, create and manage pull requests and code reviews, and
         * to receive notifications about version control events via service
         * hooks.
         */
        CODE_MANAGE("vso.code_manage"), //$NON-NLS-1$

        /**
         * Grants the ability to read source code and metadata about commits,
         * changesets, branches, and other version control artifacts. Also
         * grants the ability to get notified about version control events via
         * service hooks.
         */
        CODE_READ("vso.code"), //$NON-NLS-1$

        /**
         * Grants the ability to read, update, and delete source code, access
         * metadata about commits, changesets, branches, and other version
         * control artifacts. Also grants the ability to create and manage pull
         * requests and code reviews and to receive notifications about version
         * control events via service hooks.
         */
        CODE_WRITE("vso.code_write"), //$NON-NLS-1$

        /**
         * Grants the ability to read, write, and delete feeds and packages.
         */
        PACKAGING_MANAGE("vso.packaging_manage"), //$NON-NLS-1$

        /**
         * Grants the ability to list feeds and read packages in those feeds.
         */
        PACKAGING_READ("vso.packaging"), //$NON-NLS-1$

        /**
         * Grants the ability to list feeds and read, write, and delete packages
         * in those feeds.
         */
        PACKAGING_WRITE("vso.packaging_write"), //$NON-NLS-1$

        /**
         * Grants the ability to read your profile, accounts, collections,
         * projects, teams, and other top-level organizational artifacts.
         */
        PROFILE_READ("vso.profile"), //$NON-NLS-1$

        /**
         * Grants the ability to read service hook subscriptions and metadata,
         * including supported events, consumers, and actions.
         */
        SERVICE_HOOK_READ("vso.hooks"), //$NON-NLS-1$

        /**
         * Grants the ability to create and update service hook subscriptions
         * and read metadata, including supported events, consumers, and
         * actions."
         */
        SERVICE_HOOK_WRITE("vso.hooks_write"), //$NON-NLS-1$

        /**
         * Grants the ability to read test plans, cases, results and other test
         * management related artifacts.
         */
        TEST_READ("vso.test"), //$NON-NLS-1$

        /**
         * Grants the ability to read, create, and update test plans, cases,
         * results and other test management related artifacts.
         */
        TEST_WRITE("vso.test_write"), //$NON-NLS-1$

        /**
         * Grants the ability to read work items, queries, boards, area and
         * iterations paths, and other work item tracking related metadata. Also
         * grants the ability to execute queries and to receive notifications
         * about work item events via service hooks.
         */
        WORK_READ("vso.work"), //$NON-NLS-1$

        /**
         * Grants the ability to read, create, and update work items and
         * queries, update board metadata, read area and iterations paths other
         * work item tracking related metadata, execute queries, and to receive
         * notifications about work item events via service hooks.
         */
        WORK_WRITE("vso.work_write"); //$NON-NLS-1$

        private String value;

        TokenScope(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public static String combine(final TokenScope... tokens) {
            StringBuilder sb = new StringBuilder();

            for (final TokenScope token : tokens) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(token);
            }

            return sb.toString();
        }
    }

}

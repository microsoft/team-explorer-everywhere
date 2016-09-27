// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.protocolhandler;

import org.junit.Ignore;

import com.microsoft.alm.teamfoundation.sourcecontrol.webapi.VstsInfo;
import com.microsoft.tfs.client.common.git.json.JsonHelper;

@Ignore
public abstract class ProtocolHandlerHelpersTestData {
    private static String defaultRepo = /* */
        /* */ "{" //$NON-NLS-1$
            + "    \"serverUrl\":\"http://fabricam-dv1:8080/tfs\"," //$NON-NLS-1$
            + "    \"collection\":{" //$NON-NLS-1$
            + "        \"id\":\"b19afa93-8765-4506-9ddc-fbf6d62da591\"," //$NON-NLS-1$
            + "        \"name\":\"a collection\"," //$NON-NLS-1$
            + "        \"url\":\"http://fabricam-dv1:8080/tfs/_apis/projectCollections/b19afa93-8765-4506-9ddc-fbf6d62da591\"" //$NON-NLS-1$
            + "    }," //$NON-NLS-1$
            + "    \"repository\":{" //$NON-NLS-1$
            + "        \"id\":\"9d8fdb41-bb0e-4876-b074-925df549d80d\"," //$NON-NLS-1$
            + "        \"name\":\"gitTest_01\"," //$NON-NLS-1$
            + "        \"url\":\"http://fabricam-dv1:8080/tfs/a%20collection/_apis/git/repositories/9d8fdb41-bb0e-4876-b074-925df549d80d\"," //$NON-NLS-1$
            + "        \"project\":{" //$NON-NLS-1$
            + "            \"id\":\"e3c37a08-125a-4440-bf97-d5f84fa3a973\"," //$NON-NLS-1$
            + "            \"name\":\"gitTest_01\"," //$NON-NLS-1$
            + "            \"url\":\"http://fabricam-dv1:8080/tfs/a%20collection/_apis/projects/e3c37a08-125a-4440-bf97-d5f84fa3a973\"," //$NON-NLS-1$
            + "            \"state\":1," //$NON-NLS-1$
            + "            \"revision\":7" //$NON-NLS-1$
            + "        }," //$NON-NLS-1$
            + "        \"remoteUrl\":\"http://fabricam-dv1:8080/tfs/a%20collection/_git/gitTest_01\"" //$NON-NLS-1$
            + "    }" //$NON-NLS-1$
            + "}"; //$NON-NLS-1$

    public static VstsInfo getDefaultRepoInfo() {
        return deserializeInfo(defaultRepo);
    }

    public static String getDefaultRepoProjectId() {
        return "e3c37a08-125a-4440-bf97-d5f84fa3a973"; //$NON-NLS-1$
    }

    public static String getDefaultRepoProjectName() {
        return "gitTest_01"; //$NON-NLS-1$
    }

    public static String getDefaultRepoRepoId() {
        return "9d8fdb41-bb0e-4876-b074-925df549d80d"; //$NON-NLS-1$
    }

    public static String getDefaultRepoRepoName() {
        return "gitTest_01"; //$NON-NLS-1$
    }

    public static String getDefaultRepoRemoteUrl() {
        return "http://fabricam-dv1:8080/tfs/a%20collection/_git/gitTest_01"; //$NON-NLS-1$
    }

    private static String nonDefaultRepo = //
        /* */ "{" //$NON-NLS-1$
            + "    \"serverUrl\":\"http://fabricam-dv1:8080/tfs\"," //$NON-NLS-1$
            + "    \"collection\":{" //$NON-NLS-1$
            + "        \"id\":\"b19afa93-8765-4506-9ddc-fbf6d62da591\"," //$NON-NLS-1$
            + "        \"name\":\"DefaultCollection\"," //$NON-NLS-1$
            + "        \"url\":\"http://fabricam-dv1:8080/tfs/_apis/projectCollections/b19afa93-8765-4506-9ddc-fbf6d62da591\"" //$NON-NLS-1$
            + "    }," //$NON-NLS-1$
            + "    \"repository\":{" //$NON-NLS-1$
            + "        \"id\":\"c6f0d307-c983-4892-8429-417a4089fcf2\"," //$NON-NLS-1$
            + "        \"name\":\"gitTest_011\"," //$NON-NLS-1$
            + "        \"url\":\"http://fabricam-dv1:8080/tfs/DefaultCollection/_apis/git/repositories/c6f0d307-c983-4892-8429-417a4089fcf2\"," //$NON-NLS-1$
            + "        \"project\":{" //$NON-NLS-1$
            + "            \"id\":\"e3c37a08-125a-4440-bf97-d5f84fa3a973\"," //$NON-NLS-1$
            + "            \"name\":\"gitTest_01\"," //$NON-NLS-1$
            + "            \"url\":\"http://fabricam-dv1:8080/tfs/DefaultCollection/_apis/projects/e3c37a08-125a-4440-bf97-d5f84fa3a973\"," //$NON-NLS-1$
            + "            \"state\":1," //$NON-NLS-1$
            + "            \"revision\":7" //$NON-NLS-1$
            + "        }," //$NON-NLS-1$
            + "        \"remoteUrl\":\"http://fabricam-dv1:8080/tfs/DefaultCollection/gitTest_01/_git/gitTest_011\"" //$NON-NLS-1$
            + "    }" //$NON-NLS-1$
            + "}"; //$NON-NLS-1$

    public static VstsInfo getNonDefaultRepoInfo() {
        return deserializeInfo(nonDefaultRepo);
    }

    public static String getNonDefaultRepoProjectId() {
        return "e3c37a08-125a-4440-bf97-d5f84fa3a973"; //$NON-NLS-1$
    }

    public static String getNonDefaultRepoProjectName() {
        return "gitTest_01"; //$NON-NLS-1$
    }

    public static String getNonDefaultRepoRepoId() {
        return "c6f0d307-c983-4892-8429-417a4089fcf2"; //$NON-NLS-1$
    }

    public static String getNonDefaultRepoRepoName() {
        return "gitTest_011"; //$NON-NLS-1$
    }

    public static String getNonDefaultRepoRemoteUrl() {
        return "http://fabricam-dv1:8080/tfs/DefaultCollection/gitTest_01/_git/gitTest_011"; //$NON-NLS-1$
    }

    private static String nonLatinRepo = /* */
        /* */ "{" //$NON-NLS-1$
            + "    \"serverUrl\":\"http://fabricam-dv1:8080/tfs\"," //$NON-NLS-1$
            + "    \"collection\":{" //$NON-NLS-1$
            + "        \"id\":\"b19afa93-8765-4506-9ddc-fbf6d62da591\"," //$NON-NLS-1$
            + "        \"name\":\"DefaultCollection\"," //$NON-NLS-1$
            + "        \"url\":\"http://fabricam-dv1:8080/tfs/_apis/projectCollections/b19afa93-8765-4506-9ddc-fbf6d62da591\"" //$NON-NLS-1$
            + "    }," //$NON-NLS-1$
            + "    \"repository\":{" //$NON-NLS-1$
            + "        \"id\":\"1c1227fd-4470-464b-bc7e-c372bf95700f\"," //$NON-NLS-1$
            + "        \"name\":\"репа\"," //$NON-NLS-1$
            + "        \"url\":\"http://fabricam-dv1:8080/tfs/DefaultCollection/_apis/git/repositories/1c1227fd-4470-464b-bc7e-c372bf95700f\"," //$NON-NLS-1$
            + "        \"project\":{" //$NON-NLS-1$
            + "            \"id\":\"e3c37a08-125a-4440-bf97-d5f84fa3a973\"," //$NON-NLS-1$
            + "            \"name\":\"gitTest_01\"," //$NON-NLS-1$
            + "            \"url\":\"http://fabricam-dv1:8080/tfs/DefaultCollection/_apis/projects/e3c37a08-125a-4440-bf97-d5f84fa3a973\"," //$NON-NLS-1$
            + "            \"state\":1," //$NON-NLS-1$
            + "            \"revision\":7" //$NON-NLS-1$
            + "        }," //$NON-NLS-1$
            + "        \"remoteUrl\":\"http://fabricam-dv1:8080/tfs/DefaultCollection/gitTest_01/_git/%D1%80%D0%B5%D0%BF%D0%B0\"" //$NON-NLS-1$
            + "    }" //$NON-NLS-1$
            + "}"; //$NON-NLS-1$

    public static VstsInfo getNonLatinRepoInfo() {
        return deserializeInfo(nonLatinRepo);
    }

    public static String getNonLatinRepoProjectId() {
        return "e3c37a08-125a-4440-bf97-d5f84fa3a973"; //$NON-NLS-1$
    }

    public static String getNonLatinRepoProjectName() {
        return "gitTest_01"; //$NON-NLS-1$
    }

    public static String getNonLatinRepoRepoId() {
        return "1c1227fd-4470-464b-bc7e-c372bf95700f"; //$NON-NLS-1$
    }

    public static String getNonLatinRepoRepoName() {
        return "репа"; //$NON-NLS-1$
    }

    public static String getNonLatinRepoRemoteUrl() {
        return "http://fabricam-dv1:8080/tfs/DefaultCollection/gitTest_01/_git/%D1%80%D0%B5%D0%BF%D0%B0"; //$NON-NLS-1$
    }

    private static String vstsRepo = /* */
        /* */ "{" //$NON-NLS-1$
            + "    \"serverUrl\":\"https://fabricam.visualstudio.com\"," //$NON-NLS-1$
            + "    \"collection\":{" //$NON-NLS-1$
            + "        \"id\":\"a34b19ab-3e17-49c1-8763-cd9b8b43872c\"," //$NON-NLS-1$
            + "        \"name\":\"fabricam\"," //$NON-NLS-1$
            + "        \"url\":\"https://fabricam.visualstudio.com/_apis/projectCollections/a34b19ab-3e17-49c1-8763-cd9b8b43872c\"" //$NON-NLS-1$
            + "    }," //$NON-NLS-1$
            + "    \"repository\":{" //$NON-NLS-1$
            + "        \"id\":\"37d525ae-3515-4f73-bbbc-a60bd93328fb\"," //$NON-NLS-1$
            + "        \"name\":\"gitTest_012\"," //$NON-NLS-1$
            + "        \"url\":\"https://fabricam.visualstudio.com/_apis/git/repositories/37d525ae-3515-4f73-bbbc-a60bd93328fb\"," //$NON-NLS-1$
            + "        \"project\":{" //$NON-NLS-1$
            + "            \"id\":\"6ff5b5c3-2bc0-4bc7-97a3-46a08ed9f8e6\"," //$NON-NLS-1$
            + "            \"name\":\"gitTest_01\"," //$NON-NLS-1$
            + "            \"description\":\"description Thu Apr 09 17:41:01 EDT 2015\"," //$NON-NLS-1$
            + "            \"url\":\"https://fabricam.visualstudio.com/_apis/projects/6ff5b5c3-2bc0-4bc7-97a3-46a08ed9f8e6\"," //$NON-NLS-1$
            + "            \"state\":1," //$NON-NLS-1$
            + "            \"revision\":409368930" //$NON-NLS-1$
            + "        }," //$NON-NLS-1$
            + "        \"remoteUrl\":\"https://fabricam.visualstudio.com/gitTest_01/_git/gitTest_012\"" //$NON-NLS-1$
            + "    }" //$NON-NLS-1$
            + "}"; //$NON-NLS-1$

    public static VstsInfo getVstsRepoInfo() {
        return deserializeInfo(vstsRepo);
    }

    public static String getVstsRepoProjectId() {
        return "6ff5b5c3-2bc0-4bc7-97a3-46a08ed9f8e6"; //$NON-NLS-1$
    }

    public static String getVstsRepoProjectName() {
        return "gitTest_01"; //$NON-NLS-1$
    }

    public static String getVstsRepoRepoId() {
        return "37d525ae-3515-4f73-bbbc-a60bd93328fb"; //$NON-NLS-1$
    }

    public static String getVstsRepoRepoName() {
        return "gitTest_012"; //$NON-NLS-1$
    }

    public static String getVstsRepoRemoteUrl() {
        return "https://fabricam.visualstudio.com/gitTest_01/_git/gitTest_012"; //$NON-NLS-1$
    }

    private static VstsInfo deserializeInfo(final String serializedInfo) {
        try {
            return JsonHelper.getObjectMapper().readValue(serializedInfo.getBytes("UTF-8"), VstsInfo.class); //$NON-NLS-1$
        } catch (final Exception e) {
            throw new RuntimeException("Error in test authoring", e); //$NON-NLS-1$
        }
    }
}

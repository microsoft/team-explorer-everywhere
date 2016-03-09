// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.utils;

import java.util.List;

import com.microsoft.tfs.core.clients.build.IBuildControllerQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildControllerSpec;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IRetentionPolicy;
import com.microsoft.tfs.core.clients.build.ISchedule;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildAgent;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildAgentQueryResult;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildController;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildControllerQueryResult;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDefinition;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDefinitionQueryResult;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDeletionResult;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDetail;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildInformationNode;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildQueryResult;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildQueueQueryResult;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildServer;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildServiceHost;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.Failure;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.ProcessTemplate;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.QueuedBuild;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.Schedule;
import com.microsoft.tfs.core.clients.build.soapextensions.ProcessTemplateType;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._03._BuildGroupQueryResult;
import ms.tfs.build.buildservice._04._BuildAgent;
import ms.tfs.build.buildservice._04._BuildAgentQueryResult;
import ms.tfs.build.buildservice._04._BuildController;
import ms.tfs.build.buildservice._04._BuildControllerQueryResult;
import ms.tfs.build.buildservice._04._BuildControllerSpec;
import ms.tfs.build.buildservice._04._BuildDefinition;
import ms.tfs.build.buildservice._04._BuildDefinitionQueryResult;
import ms.tfs.build.buildservice._04._BuildDeletionResult;
import ms.tfs.build.buildservice._04._BuildDetail;
import ms.tfs.build.buildservice._04._BuildInformationNode;
import ms.tfs.build.buildservice._04._BuildQueryResult;
import ms.tfs.build.buildservice._04._BuildQueueQueryResult;
import ms.tfs.build.buildservice._04._BuildServiceHost;
import ms.tfs.build.buildservice._04._Failure;
import ms.tfs.build.buildservice._04._ProcessTemplate;
import ms.tfs.build.buildservice._04._ProcessTemplateType;
import ms.tfs.build.buildservice._04._QueuedBuild;
import ms.tfs.build.buildservice._04._RetentionPolicy;
import ms.tfs.build.buildservice._04._Schedule;

public class BuildTypeConvertor {
    /**
     * Convert passed {@link _BuildGroupQueryResult} array into an array of new
     * fully populated {@link BuildDefinitionQueryResult} instances.
     *
     * @param buildServer
     *        The instance implementing {@link IBuildServer} for which these
     *        results were obtained, this is required to populate the
     *        {@link BuildDefinition}s conatined inside the
     *        {@link BuildDefinitionQueryResult}. This is usually the instance
     *        of the {@link BuildClient}.
     * @param queryResults
     *        The array of {@link _BuildGroupQueryResult}s obtained from the
     *        server.
     * @return a fully populated array of new {@link BuildDefinitionQueryResult}
     *         instances.
     */
    public static BuildDefinitionQueryResult[] toBuildDefinitionQueryResultArray(
        final IBuildServer buildServer,
        final _BuildDefinitionQueryResult[] queryResults) {
        final BuildDefinitionQueryResult[] returnResults = new BuildDefinitionQueryResult[queryResults.length];
        for (int i = 0; i < returnResults.length; i++) {
            returnResults[i] = new BuildDefinitionQueryResult(buildServer, queryResults[i]);
        }
        return returnResults;
    }

    public static BuildAgentQueryResult[] toBuildAgentQueryResultArray(
        final IBuildServer buildServer,
        final _BuildAgentQueryResult[] queryResults) {
        final BuildAgentQueryResult[] returnResults = new BuildAgentQueryResult[queryResults.length];
        for (int i = 0; i < returnResults.length; i++) {
            returnResults[i] = new BuildAgentQueryResult(buildServer, queryResults[i]);
        }
        return returnResults;
    }

    /**
     * Convert the passed {@link _BuildDefinition} array into an array of new
     * fully populated {@link BuildDefinition}s.
     *
     * @param server
     *        The instance for which these {@link _BuildDefinition}s where
     *        obtained, this is required to fully populate the
     *        {@link BuildDefinition} instance.
     * @param buildDefinitions
     *        The {@link _BuildDefinition}s to convert, cannot be
     *        <code>null</code>
     * @return an array of new fully populated {@link BuildDefinition}s.
     */
    public static BuildDefinition[] toBuildDefinitionArray(
        final IBuildServer server,
        final _BuildDefinition[] buildDefinitions) {
        final BuildDefinition[] returnDefinitions = new BuildDefinition[buildDefinitions.length];
        for (int i = 0; i < buildDefinitions.length; i++) {
            returnDefinitions[i] = new BuildDefinition(server, buildDefinitions[i]);
        }
        return returnDefinitions;
    }

    public static _BuildDefinition[] toBuildDefinitionArray(final BuildDefinition[] buildDefinitions) {
        final _BuildDefinition[] returnArray = new _BuildDefinition[buildDefinitions.length];
        for (int i = 0; i < returnArray.length; i++) {
            returnArray[i] = buildDefinitions[i].getWebServiceObject();
        }
        return returnArray;
    }

    public static BuildDetail[] toBuildDetailArray(final IBuildServer server, final _BuildDetail[] buildDetails) {
        final BuildDetail[] returnArray = new BuildDetail[buildDetails.length];
        for (int i = 0; i < returnArray.length; i++) {
            returnArray[i] = new BuildDetail(server, buildDetails[i]);
        }
        return returnArray;
    }

    public static BuildInformationNode[] toBuildInformationNodeArray(final _BuildInformationNode[] buildNodes) {
        final BuildInformationNode[] returnArray = new BuildInformationNode[buildNodes.length];
        for (int i = 0; i < returnArray.length; i++) {
            returnArray[i] = new BuildInformationNode(buildNodes[i]);
        }
        return returnArray;
    }

    public static BuildAgent[] toBuildAgentArray(final _BuildAgent[] buildAgents) {
        final BuildAgent[] returnArray = new BuildAgent[buildAgents.length];
        for (int i = 0; i < returnArray.length; i++) {
            returnArray[i] = new BuildAgent(null, buildAgents[i]);
        }
        return returnArray;
    }

    public static Failure[] toBuildFailureArray(final _Failure[] queryFailures) {
        final Failure[] failures = new Failure[queryFailures.length];
        for (int i = 0; i < queryFailures.length; i++) {
            failures[i] = new Failure(queryFailures[i]);
        }
        return failures;
    }

    public static BuildQueryResult[] toBuildQueryResults(
        final IBuildServer server,
        final _BuildQueryResult[] queryResults) {
        final BuildQueryResult[] results = new BuildQueryResult[queryResults.length];
        for (int i = 0; i < results.length; i++) {
            results[i] = new BuildQueryResult(server, queryResults[i]);
        }
        return results;
    }

    public static BuildQueueQueryResult[] toBuildQueueQueryResults(
        final IBuildServer server,
        final _BuildQueueQueryResult[] queueQueryResults) {
        final BuildQueueQueryResult[] results = new BuildQueueQueryResult[queueQueryResults.length];
        for (int i = 0; i < results.length; i++) {
            results[i] = new BuildQueueQueryResult(server, queueQueryResults[i]);
        }
        return results;
    }

    public static QueuedBuild[] toQueuedBuildArray(final IBuildServer server, final _QueuedBuild[] builds) {
        final QueuedBuild[] qb = new QueuedBuild[builds.length];
        for (int i = 0; i < qb.length; i++) {
            qb[i] = new QueuedBuild(server, builds[i]);
        }
        return qb;
    }

    public static BuildDeletionResult[] toBuildDeletionResultAray(final _BuildDeletionResult[] results) {
        final BuildDeletionResult[] retArray = new BuildDeletionResult[results.length];
        for (int i = 0; i < retArray.length; i++) {
            retArray[i] = new BuildDeletionResult(results[i]);
        }
        return retArray;
    }

    public static Schedule[] toScheduleArray(final _Schedule[] schedules) {
        final Schedule[] retArray = new Schedule[schedules.length];
        for (int i = 0; i < retArray.length; i++) {
            retArray[i] = new Schedule(schedules[i]);
        }
        return retArray;
    }

    public static _Schedule[] toScheduleArray(final ISchedule[] schedules) {
        final _Schedule[] retArray = new _Schedule[schedules.length];
        for (int i = 0; i < retArray.length; i++) {
            retArray[i] = ((Schedule) schedules[i]).getWebServiceObject();
        }
        return retArray;
    }

    public static _RetentionPolicy[] toRetentionPolicyArray(final IRetentionPolicy[] policies) {
        final _RetentionPolicy[] retArray = new _RetentionPolicy[policies.length];
        for (int i = 0; i < retArray.length; i++) {
            // TODO: Guessing nulls passed for legacy call.
            retArray[i] = new _RetentionPolicy(
                policies[i].getBuildReason().getWebServiceObject(),
                policies[i].getBuildStatus().getWebServiceObject(),
                policies[i].getNumberToKeep(),
                policies[i].getDeleteOptions().getWebServiceObject());
        }
        return retArray;
    }

    public static _BuildController[] toBuildControllersArray(final BuildController[] controllers) {
        final _BuildController[] controllerArray = new _BuildController[controllers.length];
        for (int i = 0; i < controllerArray.length; i++) {
            controllerArray[i] = controllers[i].getWebServiceObject();
        }

        return controllerArray;
    }

    public static BuildController[] toBuildControllersArray(
        final IBuildServer server,
        final _BuildController[] controllers) {
        final BuildController[] controllerArray = new BuildController[controllers.length];
        for (int i = 0; i < controllerArray.length; i++) {
            controllerArray[i] = new BuildController(server, controllers[i]);
        }

        return controllerArray;
    }

    public static BuildServiceHost[] getServiceHostsFromControllers(final BuildController[] controllers) {
        final BuildServiceHost[] hostArray = new BuildServiceHost[controllers.length];

        for (int i = 0; i < hostArray.length; i++) {
            hostArray[i] = (BuildServiceHost) controllers[i].getServiceHost();
        }

        return hostArray;
    }

    public static _BuildServiceHost[] toBuildServiceHostArray(final BuildServiceHost[] hosts) {
        final _BuildServiceHost[] hostArray = new _BuildServiceHost[hosts.length];

        for (int i = 0; i < hostArray.length; i++) {
            hostArray[i] = hosts[i].getWebServiceObject();
        }

        return hostArray;
    }

    public static BuildServiceHost[] toBuildServiceHostArray(
        final IBuildServer server,
        final _BuildServiceHost[] hosts) {
        final BuildServiceHost[] hostArray = new BuildServiceHost[hosts.length];

        for (int i = 0; i < hostArray.length; i++) {
            hostArray[i] = new BuildServiceHost(server, hosts[i]);
        }
        return hostArray;
    }

    public static IBuildControllerQueryResult[] toBuildControllerQueryResultArray(
        final IBuildServer server,
        final _BuildControllerQueryResult[] groupResult) {
        /*
         * Equivalent to internal static IBuildControllerQueryResult[]
         * Convert(IBuildServer buildServer, BuildGroupQueryResult[]
         * groupResults) in .NET OM
         */
        final BuildControllerQueryResult[] resultArray = new BuildControllerQueryResult[groupResult.length];
        for (int i = 0; i < resultArray.length; i++) {
            resultArray[i] = new BuildControllerQueryResult(server, groupResult[i]);
        }
        return resultArray;
    }

    public static _BuildControllerSpec[] toBuildControllerSpecArray(final IBuildControllerSpec[] buildControllerSpecs) {
        return (_BuildControllerSpec[]) WrapperUtils.unwrap(_BuildControllerSpec.class, buildControllerSpecs);
    }

    public static IBuildControllerQueryResult[] toBuildControllerQueryResultArray(
        final BuildServer server,
        final _BuildControllerQueryResult[] queryResults) {
        final BuildControllerQueryResult[] resultArray = new BuildControllerQueryResult[queryResults.length];
        for (int i = 0; i < resultArray.length; i++) {
            resultArray[i] = new BuildControllerQueryResult(server, queryResults[i]);
        }
        return resultArray;
    }

    public static _ProcessTemplateType[] toProcessTemplateTypeArray(final ProcessTemplateType[] types) {
        return (_ProcessTemplateType[]) WrapperUtils.unwrap(_ProcessTemplateType.class, types);
    }

    public static ProcessTemplate[] toProcessTemplateArray(
        final IBuildServer server,
        final _ProcessTemplate[] templates) {
        final ProcessTemplate[] returnArray = new ProcessTemplate[templates.length];
        for (int i = 0; i < returnArray.length; i++) {
            returnArray[i] = new ProcessTemplate(server, templates[i]);
        }
        return returnArray;
    }

    public static <T> void addArrayToList(final List<T> list, final T[] items) {
        for (final T item : items) {
            list.add(item);
        }
    }
}

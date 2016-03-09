// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.strategies;

import org.eclipse.core.resources.IProjectDescription;

import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolder;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

/**
 * Defines a strategy for performing a get as part of an import task.
 *
 * The various GetStrategies range from doing nothing to performing a full
 * recursive get on an Eclipse project root path.
 */
public abstract class ImportGetStrategy {
    /**
     * Perform the get. Expects {@link TaskMonitorService#getTaskMonitor()}
     * returns a fresh {@link TaskMonitor} (
     * {@link TaskMonitor#begin(String, int)} has not been called).
     *
     * @param selectedPath
     *        the selected server path for import
     * @param importOptions
     *        the import options
     * @return the GetStrategyStatus for the get
     */
    public abstract GetStrategyStatus get(ImportFolder selectedPath, ImportOptions importOptions);

    /**
     * @param selectedPath
     *        the selected server path for import
     * @param importOptions
     *        the import options
     * @return some text describing what this strategy will do
     */
    public abstract String getPlan(ImportFolder selectedPath, ImportOptions importOptions);

    /**
     * The GetStrategyStatus, the result of the GetStrategy execution.
     */
    public class GetStrategyStatus {
        private final GetRequest getRequest;
        private final GetStatus getStatus;

        public GetStrategyStatus(final GetRequest getRequest, final GetStatus getStatus) {
            this.getRequest = getRequest;
            this.getStatus = getStatus;
        }

        public GetRequest getGetRequest() {
            return getRequest;
        }

        public GetStatus getGetStatus() {
            return getStatus;
        }
    }

    /**
     * A GetStrategy that does nothing.
     */
    public static class Null extends ImportGetStrategy {
        @Override
        public String getPlan(final ImportFolder selectedPath, final ImportOptions importOptions) {
            return "no get"; //$NON-NLS-1$
        }

        @Override
        public GetStrategyStatus get(final ImportFolder selectedPath, final ImportOptions importOptions) {
            return null;
        }
    }

    /**
     * This was originally intended to just do a get on the .project file if the
     * path was mapped (but the .project file did not exist). No use case can be
     * found, however, where this scenario occurs - so now we always do a full
     * recursive get latest in that case.
     */
    public static class ProjectMetadataFile extends ImportGetStrategy {
        @Override
        public String getPlan(final ImportFolder selectedPath, final ImportOptions importOptions) {
            return "get project file"; //$NON-NLS-1$
        }

        @Override
        public GetStrategyStatus get(final ImportFolder selectedPath, final ImportOptions importOptions) {
            final GetRequest getRequest = new GetRequest(new ItemSpec(selectedPath.getFullPath()
                + "/" //$NON-NLS-1$
                + IProjectDescription.DESCRIPTION_FILE_NAME, RecursionType.NONE), LatestVersionSpec.INSTANCE);

            final GetOptions getOptions = GetOptions.GET_ALL;

            final GetStatus getStatus = importOptions.getTFSWorkspace().get(new GetRequest[] {
                getRequest
            }, getOptions);

            return new GetStrategyStatus(getRequest, getStatus);
        }
    }

    /**
     * A GetStrategy that does a full recursive get on the server folder
     * corresponding to an Eclipse project root. This is the "standard" get
     * strategy used for a normal import.
     */
    public static class FullRecursive extends ImportGetStrategy {
        @Override
        public String getPlan(final ImportFolder selectedPath, final ImportOptions importOptions) {
            return "recursive get"; //$NON-NLS-1$
        }

        @Override
        public GetStrategyStatus get(final ImportFolder selectedPath, final ImportOptions importOptions) {
            final GetRequest getRequest = new GetRequest(
                new ItemSpec(selectedPath.getFullPath(), RecursionType.FULL),
                LatestVersionSpec.INSTANCE);

            final GetStatus getStatus = importOptions.getTFSWorkspace().get(new GetRequest[] {
                getRequest
            }, GetOptions.NONE);

            return new GetStrategyStatus(getRequest, getStatus);
        }
    }

    /**
     * A GetStrategy that does a full recursive get with the force option on the
     * server folder corresponding to an Eclipse project root. This is used when
     * the user requests it, or when the server believes we have files we don't
     * (from an old get that's been deleted.)
     */
    public static class ForceFullRecursive extends ImportGetStrategy {
        @Override
        public String getPlan(final ImportFolder selectedPath, final ImportOptions importOptions) {
            return "forced recursive get"; //$NON-NLS-1$
        }

        @Override
        public GetStrategyStatus get(final ImportFolder selectedPath, final ImportOptions importOptions) {
            final GetRequest getRequest = new GetRequest(
                new ItemSpec(selectedPath.getFullPath(), RecursionType.FULL),
                LatestVersionSpec.INSTANCE);

            final GetStatus getStatus = importOptions.getTFSWorkspace().get(new GetRequest[] {
                getRequest
            }, GetOptions.GET_ALL);

            return new GetStrategyStatus(getRequest, getStatus);
        }
    }
}

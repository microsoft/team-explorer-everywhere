// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal;

import java.text.Collator;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IBuildAgent;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildControllerQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildControllerSpec;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDefinitionComparer;
import com.microsoft.tfs.core.clients.build.utils.BuildPath;
import com.microsoft.tfs.util.Check;

/**
 * The common cache for TeamBuild Data.
 */
public class TeamBuildCache {
    /**
     * Cache timeout in milliseconds - set to 300000 (5 mins) which is the
     * default for the Microsoft client.
     */
    public static final int CACHE_TIMEOUT_VALUE = 300000;

    private final static Map<String, TeamBuildCache> instances = new HashMap<String, TeamBuildCache>(3);

    private final IBuildServer buildServer;
    private final String teamProject;
    private IBuildDefinition[] buildDefinitions;
    private long buildDefinitionCacheTime;
    private final Object buildDefinitionLock = new Object();
    // private IBuildAgent[] buildAgents;
    // private long buildAgentCacheTime;
    // private final Object buildAgentLock = new Object();
    private IBuildController[] buildControllers;
    private long buildControllerCacheTime;
    private final Object buildControllerLock = new Object();
    private String[] buildQualities;
    private long buildQualitiesCacheTime;
    private final Object buildQualityLock = new Object();
    private final String cacheInstanceKey;

    private TeamBuildCache(final String cacheInstanceKey, final IBuildServer buildServer, final String teamProject) {
        this.cacheInstanceKey = cacheInstanceKey;
        this.buildServer = buildServer;
        this.teamProject = teamProject;
    }

    public IBuildDefinition[] getBuildDefinitions(final boolean forceRefresh) {
        synchronized (buildDefinitionLock) {
            if (forceRefresh || buildDefinitions == null || cacheExpired(buildDefinitionCacheTime)) {
                buildDefinitions = buildServer.queryBuildDefinitions(teamProject);
                Arrays.sort(buildDefinitions, new BuildDefinitionComparer(buildServer));
                buildDefinitionCacheTime = System.currentTimeMillis();
            }
        }
        return buildDefinitions;
    }

    public IBuildController[] getBuildControllers(final boolean forceRefresh) {

        IBuildDefinition[] v1Definitions = null;
        if ((forceRefresh || buildControllers == null || cacheExpired(buildControllerCacheTime))
            && buildServer.getBuildServerVersion().isV1()) {
            // Can take a while to getBuildDefinitions for a V1 server - do
            // outside of the lock to avoid deadlock.
            // Note that reloadBuildDefinitionsAndAgents uses this behaviour to
            // optimise the reload.
            v1Definitions = getBuildDefinitions(forceRefresh);
        }

        synchronized (buildControllerLock) {
            if (forceRefresh || buildControllers == null || cacheExpired(buildControllerCacheTime)) {
                if (buildServer.getBuildServerVersion().isV1()) {
                    buildControllers = getBuildControllersFromDefinitions(v1Definitions);
                } else {
                    if (buildServer.getBuildServerVersion().isV2()) {
                        final IBuildControllerSpec controllerSpec = buildServer.createBuildControllerSpec();
                        controllerSpec.setName(BuildPath.combine(teamProject, BuildConstants.STAR));
                        final IBuildControllerQueryResult result = buildServer.queryBuildControllers(controllerSpec);
                        buildControllers = result.getControllers();
                        if (result.getFailures().length > 0) {
                            throw new RuntimeException(result.getFailures()[0].getMessage());
                        }
                    } else {
                        buildControllers = buildServer.queryBuildControllers(true);
                    }
                }
                buildControllerCacheTime = System.currentTimeMillis();
            }

            // TODO - Sort controllers before returning.

        }
        return buildControllers;
    }

    public void reloadBuildDefinitionsAndControllers() {
        if (buildServer.getBuildServerVersion().isV1()) {
            // This is a performance optimization when talking to a V1 server
            // getBuildControllers will automatically re-populate the build
            // definitions.
            getBuildControllers(true);
        } else {
            getBuildDefinitions(true);
            getBuildControllers(true);
        }
    }

    /**
     * Called when talking to a TFS 2005 server to convert the build definitions
     * into an array of build controllers contained within those definitions. In
     * TFS 2005 there was no notion of a build agent/controller as an entity
     * managed by the build server - only build machines that are referenced in
     * the build definition script. I V1 server will have to have all of it's
     * TFSBuild.proj files parsed to created the {@link IBuildDefinition}[] with
     * the appropriate information taken out of the array here and put into the
     * {@link IBuildAgent}[]
     *
     * @param buildDefinitions
     *        the V1 definitions to inspect for referenced build agents.
     * @return an array of {@link IBuildControllers}s referened in the passed
     *         {@link IBuildDefinition}[].
     */
    private IBuildController[] getBuildControllersFromDefinitions(final IBuildDefinition[] buildDefinitions) {
        final Map<String, IBuildController> buildControllers = new HashMap<String, IBuildController>();

        for (int i = 0; i < buildDefinitions.length; i++) {
            final IBuildController buildController = buildDefinitions[i].getBuildController();
            if (buildController != null
                && buildController.getName() != null
                && buildController.getName().length() > 0) {
                final String name = buildController.getName() + "_" + buildDefinitions[i].getName(); //$NON-NLS-1$
                buildDefinitions[i].setBuildController(buildController);
                buildControllers.put(name, buildController);
            }
        }

        final Collection<IBuildController> values = buildControllers.values();
        return values.toArray(new IBuildController[values.size()]);
    }

    public String[] getBuildQualities(final boolean forceRefresh) {
        synchronized (buildQualityLock) {
            if (forceRefresh || buildQualities == null || cacheExpired(buildQualitiesCacheTime)) {
                buildQualities = buildServer.getBuildQualities(teamProject);
                Arrays.sort(buildQualities, Collator.getInstance());
                buildQualitiesCacheTime = System.currentTimeMillis();
            }
        }
        return buildQualities;
    }

    private boolean cacheExpired(final long cachedTime) {
        return System.currentTimeMillis() > cachedTime + CACHE_TIMEOUT_VALUE;
    }

    /**
     * Clears the team build cache for the existing build server, if it exists.
     * Returns a new (empty) team build cache.
     *
     * @param buildServer
     * @param teamProjectName
     * @return the new (empty) team build cache
     */
    public static TeamBuildCache refreshInstance(final IBuildServer buildServer, final String teamProjectName) {
        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$
        Check.notNullOrEmpty(teamProjectName, "teamProjectName"); //$NON-NLS-1$

        final String key = buildServer.getConnection().getAuthorizedTFSUser().toString()
            + "@" //$NON-NLS-1$
            + buildServer.getConnection().getInstanceID().getGUIDString()
            + "/" //$NON-NLS-1$
            + teamProjectName;

        synchronized (instances) {
            final TeamBuildCache instance = new TeamBuildCache(key, buildServer, teamProjectName);
            instances.put(key, instance);
            return instance;
        }
    }

    public static TeamBuildCache getInstance(final IBuildServer buildServer, final String teamProjectName) {
        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$
        Check.notNullOrEmpty(teamProjectName, "teamProjectName"); //$NON-NLS-1$

        final String key = buildServer.getConnection().getAuthorizedTFSUser().toString()
            + "@" //$NON-NLS-1$
            + buildServer.getConnection().getInstanceID().getGUIDString()
            + "/" //$NON-NLS-1$
            + teamProjectName;

        synchronized (instances) {
            TeamBuildCache instance = instances.get(key);
            if (instance == null) {
                instance = new TeamBuildCache(key, buildServer, teamProjectName);
                instances.put(key, instance);
            }
            return instance;
        }
    }

    /**
     * Destroy all instances of the TeamBuildCache allowing the cached data to
     * be reclaimed.
     */
    public static void destroy() {
        synchronized (instances) {
            instances.clear();
        }
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cacheInstanceKey == null) ? 0 : cacheInstanceKey.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TeamBuildCache)) {
            return false;
        }
        final TeamBuildCache other = (TeamBuildCache) obj;
        if (cacheInstanceKey == null) {
            if (other.cacheInstanceKey != null) {
                return false;
            }
        } else if (!cacheInstanceKey.equals(other.cacheInstanceKey)) {
            return false;
        }
        return true;
    }

}

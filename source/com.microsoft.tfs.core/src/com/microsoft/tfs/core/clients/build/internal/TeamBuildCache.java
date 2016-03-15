// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal;

import java.text.Collator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDefinitionComparer;
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

        synchronized (buildControllerLock) {
            if (forceRefresh || buildControllers == null || cacheExpired(buildControllerCacheTime)) {
                buildControllers = buildServer.queryBuildControllers(true);
                buildControllerCacheTime = System.currentTimeMillis();
            }

            // TODO - Sort controllers before returning.

        }
        return buildControllers;
    }

    public void reloadBuildDefinitionsAndControllers() {
        getBuildDefinitions(true);
        getBuildControllers(true);
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

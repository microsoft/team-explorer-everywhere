// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.teamfoundation.distributedtask.webapi;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.ws.Response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.microsoft.teamfoundation.distributedtask.webapi.model.JobEvent;
import com.microsoft.teamfoundation.distributedtask.webapi.model.TaskLog;
import com.microsoft.teamfoundation.distributedtask.webapi.model.TaskOrchestrationPlan;
import com.microsoft.teamfoundation.distributedtask.webapi.model.Timeline;
import com.microsoft.teamfoundation.distributedtask.webapi.model.TimelineRecord;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.visualstudio.services.webapi.model.VssJsonCollectionWrapper;
import com.microsoft.vss.client.core.model.ApiResourceVersion;
import com.microsoft.vss.client.core.model.NameValueCollection;

public class TaskHttpClient extends TaskHttpClientBase {

    public TaskHttpClient(final TFSTeamProjectCollection connection) {
        super(connection);
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param eventData
     *
     * @param planId
     *
     */
    public void postEvent(
        final UUID scopeIdentifier,
        final String hubName,
        final JobEvent eventData,
        final UUID planId) {

        final UUID locationId = UUID.fromString("557624af-b29e-4c20-8ab0-0399d2204f3f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("scopeIdentifier", scopeIdentifier); //$NON-NLS-1$
        routeValues.put("hubName", hubName); //$NON-NLS-1$
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.POST,
            locationId,
            routeValues,
            apiVersion,
            eventData,
            APPLICATION_JSON_TYPE,
            APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest, Response.class);
    }

    /**
     * @param eventData
     *
     * @param planId
     *
     */
    @Deprecated
    @Override
    public void postEvent(final JobEvent eventData, final UUID planId) {

        final UUID locationId = UUID.fromString("557624af-b29e-4c20-8ab0-0399d2204f3f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.POST,
            locationId,
            routeValues,
            apiVersion,
            eventData,
            APPLICATION_JSON_TYPE,
            APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest, Response.class);
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param planId
     *
     * @param logId
     *
     * @return TaskLog
     */
    public TaskLog appendLog(
        final InputStream uploadStream,
        final UUID scopeIdentifier,
        final String hubName,
        final UUID planId,
        final int logId) {

        final UUID locationId = UUID.fromString("46f5667d-263a-4684-91b1-dff7fdcf64e2"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("scopeIdentifier", scopeIdentifier); //$NON-NLS-1$
        routeValues.put("hubName", hubName); //$NON-NLS-1$
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("logId", logId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.POST,
            locationId,
            routeValues,
            apiVersion,
            uploadStream,
            APPLICATION_OCTET_STREAM_TYPE,
            APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, TaskLog.class);
    }

    /**
     * @param planId
     *
     * @param logId
     *
     * @return TaskLog
     */
    @Deprecated
    @Override
    public TaskLog appendLog(final InputStream uploadStream, final UUID planId, final int logId) {

        final UUID locationId = UUID.fromString("46f5667d-263a-4684-91b1-dff7fdcf64e2"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("logId", logId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.POST,
            locationId,
            routeValues,
            apiVersion,
            uploadStream,
            APPLICATION_OCTET_STREAM_TYPE,
            APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, TaskLog.class);
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param planId
     *
     * @param logId
     *
     * @param startLine
     *
     * @param endLine
     *
     * @return List<String>
     */
    public List<String> getLog(
        final UUID scopeIdentifier,
        final String hubName,
        final UUID planId,
        final int logId,
        final Integer startLine,
        final Integer endLine) {

        final UUID locationId = UUID.fromString("46f5667d-263a-4684-91b1-dff7fdcf64e2"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("scopeIdentifier", scopeIdentifier); //$NON-NLS-1$
        routeValues.put("hubName", hubName); //$NON-NLS-1$
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("logId", logId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("startLine", startLine); //$NON-NLS-1$
        queryParameters.addIfNotNull("endLine", endLine); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.GET,
            locationId,
            routeValues,
            apiVersion,
            queryParameters,
            APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<String>>() {
        });
    }

    /**
     * @param planId
     *
     * @param logId
     *
     * @param startLine
     *
     * @param endLine
     *
     * @return List<String>
     */
    @Override
    @Deprecated
    public List<String> getLog(final UUID planId, final int logId, final Integer startLine, final Integer endLine) {

        final UUID locationId = UUID.fromString("46f5667d-263a-4684-91b1-dff7fdcf64e2"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("logId", logId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("startLine", startLine); //$NON-NLS-1$
        queryParameters.addIfNotNull("endLine", endLine); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.GET,
            locationId,
            routeValues,
            apiVersion,
            queryParameters,
            APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<String>>() {
        });
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param planId
     *
     * @param timelineId
     *
     */
    public void deleteTimeline(
        final UUID scopeIdentifier,
        final String hubName,
        final UUID planId,
        final UUID timelineId) {

        final UUID locationId = UUID.fromString("83597576-cc2c-453c-bea6-2882ae6a1653"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("scopeIdentifier", scopeIdentifier); //$NON-NLS-1$
        routeValues.put("hubName", hubName); //$NON-NLS-1$
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$

        final Object httpRequest =
            super.createRequest(HttpMethod.DELETE, locationId, routeValues, apiVersion, APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest, Response.class);
    }

    /**
     * @param planId
     *
     * @param timelineId
     *
     */
    @Deprecated
    @Override
    public void deleteTimeline(final UUID planId, final UUID timelineId) {

        final UUID locationId = UUID.fromString("83597576-cc2c-453c-bea6-2882ae6a1653"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$

        final Object httpRequest =
            super.createRequest(HttpMethod.DELETE, locationId, routeValues, apiVersion, APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest, Response.class);
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param records
     *
     * @param planId
     *
     * @param timelineId
     *
     * @return List<TimelineRecord>
     */
    public List<TimelineRecord> updateRecords(
        final UUID scopeIdentifier,
        final String hubName,
        final List<TimelineRecord> records,
        final UUID planId,
        final UUID timelineId) {
        return updateRecords(
            scopeIdentifier,
            hubName,
            VssJsonCollectionWrapper.newInstance(records),
            planId,
            timelineId);
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param records
     *
     * @param planId
     *
     * @param timelineId
     *
     * @return List<TimelineRecord>
     */
    public List<TimelineRecord> updateRecords(
        final UUID scopeIdentifier,
        final String hubName,
        final VssJsonCollectionWrapper<List<TimelineRecord>> records,
        final UUID planId,
        final UUID timelineId) {

        final UUID locationId = UUID.fromString("8893bc5b-35b2-4be7-83cb-99e683551db4"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("scopeIdentifier", scopeIdentifier); //$NON-NLS-1$
        routeValues.put("hubName", hubName); //$NON-NLS-1$
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.PATCH,
            locationId,
            routeValues,
            apiVersion,
            records,
            APPLICATION_JSON_TYPE,
            APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<TimelineRecord>>() {
        });
    }

    /**
     * @param records
     *
     * @param planId
     *
     * @param timelineId
     *
     * @return List<TimelineRecord>
     */
    @Deprecated
    @Override
    public List<TimelineRecord> updateRecords(
        final VssJsonCollectionWrapper<List<TimelineRecord>> records,
        final UUID planId,
        final UUID timelineId) {

        final UUID locationId = UUID.fromString("8893bc5b-35b2-4be7-83cb-99e683551db4"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.PATCH,
            locationId,
            routeValues,
            apiVersion,
            records,
            APPLICATION_JSON_TYPE,
            APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<TimelineRecord>>() {
        });
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param lines
     *
     * @param planId
     *
     * @param timelineId
     *
     * @param recordId
     *
     */
    public void postLines(
        final UUID scopeIdentifier,
        final String hubName,
        final List<String> lines,
        final UUID planId,
        final UUID timelineId,
        final UUID recordId) {
        postLines(scopeIdentifier, hubName, VssJsonCollectionWrapper.newInstance(lines), planId, timelineId, recordId);
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param lines
     *
     * @param planId
     *
     * @param timelineId
     *
     * @param recordId
     *
     */
    public void postLines(
        final UUID scopeIdentifier,
        final String hubName,
        final VssJsonCollectionWrapper<List<String>> lines,
        final UUID planId,
        final UUID timelineId,
        final UUID recordId) {

        final UUID locationId = UUID.fromString("858983e4-19bd-4c5e-864c-507b59b58b12"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("scopeIdentifier", scopeIdentifier); //$NON-NLS-1$
        routeValues.put("hubName", hubName); //$NON-NLS-1$
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$
        routeValues.put("recordId", recordId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.POST,
            locationId,
            routeValues,
            apiVersion,
            lines,
            APPLICATION_JSON_TYPE,
            APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest, Response.class);
    }

    /**
     * @param lines
     *
     * @param planId
     *
     * @param timelineId
     *
     * @param recordId
     *
     */
    @Deprecated
    @Override
    public void postLines(
        final VssJsonCollectionWrapper<List<String>> lines,
        final UUID planId,
        final UUID timelineId,
        final UUID recordId) {

        final UUID locationId = UUID.fromString("858983e4-19bd-4c5e-864c-507b59b58b12"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$
        routeValues.put("recordId", recordId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.POST,
            locationId,
            routeValues,
            apiVersion,
            lines,
            APPLICATION_JSON_TYPE,
            APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest, Response.class);
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param planId
     *
     * @return List<Timeline>
     */
    public List<Timeline> getTimelines(final UUID scopeIdentifier, final String hubName, final UUID planId) {

        final UUID locationId = UUID.fromString("83597576-cc2c-453c-bea6-2882ae6a1653"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("scopeIdentifier", scopeIdentifier); //$NON-NLS-1$
        routeValues.put("hubName", hubName); //$NON-NLS-1$
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest =
            super.createRequest(HttpMethod.GET, locationId, routeValues, apiVersion, APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<Timeline>>() {
        });
    }

    /**
     * @param planId
     *
     * @return List<Timeline>
     */
    @Deprecated
    @Override
    public List<Timeline> getTimelines(final UUID planId) {

        final UUID locationId = UUID.fromString("83597576-cc2c-453c-bea6-2882ae6a1653"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest =
            super.createRequest(HttpMethod.GET, locationId, routeValues, apiVersion, APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<Timeline>>() {
        });
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param planId
     *
     * @param timelineId
     *
     * @return Response
     */
    public Timeline getTimeline(
        final UUID scopeIdentifier,
        final String hubName,
        final UUID planId,
        final UUID timelineId) {
        return getTimeline(scopeIdentifier, hubName, planId, timelineId, (Integer) null, (Boolean) null);
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param planId
     *
     * @param timelineId
     *
     * @param includeRecords
     *
     * @return Response
     */
    public Timeline getTimeline(
        final UUID scopeIdentifier,
        final String hubName,
        final UUID planId,
        final UUID timelineId,
        final boolean includeRecords) {
        return getTimeline(scopeIdentifier, hubName, planId, timelineId, (Integer) null, includeRecords);
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param planId
     *
     * @param timelineId
     *
     * @param changeId
     *
     * @return Response
     */
    public Timeline getTimeline(
        final UUID scopeIdentifier,
        final String hubName,
        final UUID planId,
        final UUID timelineId,
        final int changeId) {
        return getTimeline(scopeIdentifier, hubName, planId, timelineId, changeId, (Boolean) null);
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param planId
     *
     * @param timelineId
     *
     * @param changeId
     *
     * @param includeRecords
     *
     * @return Response
     */
    public Timeline getTimeline(
        final UUID scopeIdentifier,
        final String hubName,
        final UUID planId,
        final UUID timelineId,
        final Integer changeId,
        final Boolean includeRecords) {

        final UUID locationId = UUID.fromString("83597576-cc2c-453c-bea6-2882ae6a1653"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("scopeIdentifier", scopeIdentifier); //$NON-NLS-1$
        routeValues.put("hubName", hubName); //$NON-NLS-1$
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("changeId", changeId); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeRecords", includeRecords); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.GET,
            locationId,
            routeValues,
            apiVersion,
            queryParameters,
            APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Timeline.class);
    }

    /**
     * @param planId
     *
     * @param timelineId
     *
     * @param changeId
     *
     * @param includeRecords
     *
     * @return Response
     */
    @Deprecated
    @Override
    public Timeline getTimeline(
        final UUID planId,
        final UUID timelineId,
        final Integer changeId,
        final Boolean includeRecords) {

        final UUID locationId = UUID.fromString("83597576-cc2c-453c-bea6-2882ae6a1653"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("changeId", changeId); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeRecords", includeRecords); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.GET,
            locationId,
            routeValues,
            apiVersion,
            queryParameters,
            APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Timeline.class);
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param planId
     *
     * @return TaskOrchestrationPlan
     */
    public TaskOrchestrationPlan getPlan(final UUID scopeIdentifier, final String hubName, final UUID planId) {

        final UUID locationId = UUID.fromString("5cecd946-d704-471e-a45f-3b4064fcfaba"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("scopeIdentifier", scopeIdentifier); //$NON-NLS-1$
        routeValues.put("hubName", hubName); //$NON-NLS-1$
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest =
            super.createRequest(HttpMethod.GET, locationId, routeValues, apiVersion, APPLICATION_JSON_TYPE);
        return super.sendRequest(httpRequest, TaskOrchestrationPlan.class);
    }

    /**
     * @param planId
     *
     * @return TaskOrchestrationPlan
     */
    @Deprecated
    @Override
    public TaskOrchestrationPlan getPlan(final UUID planId) {

        final UUID locationId = UUID.fromString("5cecd946-d704-471e-a45f-3b4064fcfaba"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest =
            super.createRequest(HttpMethod.GET, locationId, routeValues, apiVersion, APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, TaskOrchestrationPlan.class);
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param planId
     *
     * @param timelineId
     *
     * @return List<TimelineRecord>
     */
    public List<TimelineRecord> getRecords(
        final UUID scopeIdentifier,
        final String hubName,
        final UUID planId,
        final UUID timelineId) {
        return getRecords(scopeIdentifier, hubName, planId, timelineId, (Integer) null);
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param planId
     *
     * @param timelineId
     *
     * @param changeId
     *
     * @return List<TimelineRecord>
     */
    public List<TimelineRecord> getRecords(
        final UUID scopeIdentifier,
        final String hubName,
        final UUID planId,
        final UUID timelineId,
        final Integer changeId) {

        final UUID locationId = UUID.fromString("8893bc5b-35b2-4be7-83cb-99e683551db4"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("scopeIdentifier", scopeIdentifier); //$NON-NLS-1$
        routeValues.put("hubName", hubName); //$NON-NLS-1$
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("changeId", changeId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.GET,
            locationId,
            routeValues,
            apiVersion,
            queryParameters,
            APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<TimelineRecord>>() {
        });
    }

    /**
     * @param planId
     *
     * @param timelineId
     *
     * @param changeId
     *
     * @return List<TimelineRecord>
     */
    @Deprecated
    @Override
    public List<TimelineRecord> getRecords(final UUID planId, final UUID timelineId, final Integer changeId) {

        final UUID locationId = UUID.fromString("8893bc5b-35b2-4be7-83cb-99e683551db4"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("changeId", changeId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.GET,
            locationId,
            routeValues,
            apiVersion,
            queryParameters,
            APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<TimelineRecord>>() {
        });
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param planId
     *
     * @return List<TaskLog>
     */
    public List<TaskLog> getLogs(final UUID scopeIdentifier, final String hubName, final UUID planId) {

        final UUID locationId = UUID.fromString("46f5667d-263a-4684-91b1-dff7fdcf64e2"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("scopeIdentifier", scopeIdentifier); //$NON-NLS-1$
        routeValues.put("hubName", hubName); //$NON-NLS-1$
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest =
            super.createRequest(HttpMethod.GET, locationId, routeValues, apiVersion, APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<TaskLog>>() {
        });
    }

    /**
     * @param planId
     *
     * @return List<TaskLog>
     */
    @Deprecated
    @Override
    public List<TaskLog> getLogs(final UUID planId) {

        final UUID locationId = UUID.fromString("46f5667d-263a-4684-91b1-dff7fdcf64e2"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest =
            super.createRequest(HttpMethod.GET, locationId, routeValues, apiVersion, APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<TaskLog>>() {
        });
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param log
     *
     * @param planId
     *
     * @return TaskLog
     */
    public TaskLog createLog(final UUID scopeIdentifier, final String hubName, final TaskLog log, final UUID planId) {

        final UUID locationId = UUID.fromString("46f5667d-263a-4684-91b1-dff7fdcf64e2"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("scopeIdentifier", scopeIdentifier); //$NON-NLS-1$
        routeValues.put("hubName", hubName); //$NON-NLS-1$
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.POST,
            locationId,
            routeValues,
            apiVersion,
            log,
            APPLICATION_JSON_TYPE,
            APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, TaskLog.class);
    }

    /**
     * @param log
     *
     * @param planId
     *
     * @return TaskLog
     */
    @Deprecated
    @Override
    public TaskLog createLog(final TaskLog log, final UUID planId) {

        final UUID locationId = UUID.fromString("46f5667d-263a-4684-91b1-dff7fdcf64e2"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.POST,
            locationId,
            routeValues,
            apiVersion,
            log,
            APPLICATION_JSON_TYPE,
            APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, TaskLog.class);
    }

    /**
     * @param scopeIdentifier
     *        An UUID of the request scope, e.g. a Team Project ID
     *
     * @param hubName
     *        Either "build" for Buiuld2 hub or "rm" for Release Management hub
     *
     * @param timeline
     *
     * @param planId
     *
     * @return Timeline
     */
    public Timeline createTimeline(
        final UUID scopeIdentifier,
        final String hubName,
        final Timeline timeline,
        final UUID planId) {

        final UUID locationId = UUID.fromString("83597576-cc2c-453c-bea6-2882ae6a1653"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("scopeIdentifier", scopeIdentifier); //$NON-NLS-1$
        routeValues.put("hubName", hubName); //$NON-NLS-1$
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.POST,
            locationId,
            routeValues,
            apiVersion,
            timeline,
            APPLICATION_JSON_TYPE,
            APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Timeline.class);
    }

    /**
     * @param timeline
     *
     * @param planId
     *
     * @return Timeline
     */
    @Deprecated
    @Override
    public Timeline createTimeline(final Timeline timeline, final UUID planId) {

        final UUID locationId = UUID.fromString("83597576-cc2c-453c-bea6-2882ae6a1653"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(
            HttpMethod.POST,
            locationId,
            routeValues,
            apiVersion,
            timeline,
            APPLICATION_JSON_TYPE,
            APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Timeline.class);
    }
}
